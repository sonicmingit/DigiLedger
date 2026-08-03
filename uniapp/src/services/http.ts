import {
  getServerProfile,
  markActiveNode,
  nodeUrl,
  type NodeName,
} from "./server-profile";

type Method = "GET" | "HEAD" | "POST" | "PUT" | "PATCH" | "DELETE";
type Envelope<T> = { code: number; data: T; msg?: string };
export class ApiError extends Error {
  constructor(
    message: string,
    public status = 0,
    public business = false,
  ) {
    super(message);
  }
}

function requestOnce<T>(
  base: string,
  path: string,
  method: Method,
  data?: unknown,
  header?: Record<string, string>,
  timeout = 12000,
): Promise<T> {
  const url = `${base}${path.startsWith("/") ? path : `/${path}`}`;
  return new Promise((resolve, reject) =>
    uni.request({
      url,
      // 当前 uni-app 运行时支持 PATCH，但项目锁定的旧版类型声明尚未列出该方法。
      method: method as any,
      data: data as any,
      header: { "Content-Type": "application/json", ...header },
      timeout,
      success: (res) => {
        const body = res.data as Envelope<T>;
        if (res.statusCode >= 500)
          return reject(
            new ApiError(`服务器异常 (${res.statusCode})`, res.statusCode),
          );
        if (res.statusCode >= 400)
          return reject(
            new ApiError(
              body?.msg || `请求失败 (${res.statusCode})`,
              res.statusCode,
            ),
          );
        if (!body || body.code !== 200)
          return reject(
            new ApiError(body?.msg || "业务处理失败", res.statusCode, true),
          );
        resolve(body.data);
      },
      fail: (e) => reject(new ApiError(e.errMsg || "网络连接失败")),
    }),
  );
}

/** Safe failover: only idempotent reads retry once; writes never cross-node automatically. */
export async function apiRequest<T>(
  path: string,
  options: {
    method?: Method;
    data?: unknown;
    header?: Record<string, string>;
    node?: NodeName;
    allowFailover?: boolean;
  } = {},
) {
  const p = getServerProfile(),
    method = options.method || "GET",
    first = options.node || p.preferred,
    other: NodeName = first === "primary" ? "secondary" : "primary";
  const base = nodeUrl(p, first);
  if (!base) throw new ApiError("请先配置服务器地址");
  try {
    const result = await requestOnce<T>(
      base,
      path,
      method,
      options.data,
      options.header,
      p.timeoutMs,
    );
    markActiveNode(first, p);
    return result;
  } catch (error) {
    const e = error as ApiError;
    const retry =
      (method === "GET" || method === "HEAD") &&
      options.allowFailover !== false &&
      p.autoFailover &&
      !e.business &&
      (e.status === 0 || e.status >= 500) &&
      !!nodeUrl(p, other);
    if (!retry) throw e;
    const result = await requestOnce<T>(
      nodeUrl(p, other),
      path,
      method,
      options.data,
      options.header,
      p.timeoutMs,
    );
    markActiveNode(other, p);
    return result;
  }
}
export const testNode = (node: NodeName) =>
  apiRequest<unknown>("/dashboard/summary", {
    method: "GET",
    node,
    allowFailover: false,
  });

/** Uses uni.uploadFile so the same upload flow works in H5 and native Android. */
export function uploadFile(filePath: string, node?: NodeName) {
  const p = getServerProfile(),
    uploadNode = node || p.preferred,
    base = nodeUrl(p, uploadNode);
  if (!base) return Promise.reject(new ApiError("请先配置服务器地址"));
  return new Promise<{ url: string; objectKey: string }>((resolve, reject) =>
    uni.uploadFile({
      url: `${base}/files/upload`,
      filePath,
      name: "file",
      timeout: p.timeoutMs,
      success: (res) => {
        if (res.statusCode >= 400)
          return reject(
            new ApiError(`上传失败 (${res.statusCode})`, res.statusCode),
          );
        try {
          const body = JSON.parse(res.data);
          if (body.code !== 200)
            throw new ApiError(body.msg || "上传失败", res.statusCode, true);
          markActiveNode(uploadNode, p);
          resolve(body.data);
        } catch (e) {
          reject(e);
        }
      },
      fail: (e) => reject(new ApiError(e.errMsg || "上传失败")),
    }),
  );
}
