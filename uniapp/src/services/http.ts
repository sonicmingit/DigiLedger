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

export type LoginStatus = { configured: boolean; authenticated: boolean; csrfToken: string };
const csrfTokens = new Map<string, string>();
let openingLogin = false;
function requireLogin() {
  const pages = getCurrentPages();
  if (openingLogin || pages[pages.length - 1]?.route === 'pages/login/index') return;
  openingLogin = true;
  uni.reLaunch({ url: '/pages/login/index', complete: () => { openingLogin = false; } });
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
      header: { "Content-Type": "application/json", 'X-CSRF-Token': csrfTokens.get(base) || '', ...header },
      withCredentials: true,
      timeout,
      success: (res) => {
        const body = res.data as Envelope<T>;
        if (res.statusCode === 401 && !path.startsWith('/auth/')) {
          csrfTokens.delete(base);
          requireLogin();
        }
        if (body?.code === 200 && (path === '/auth/status' || path === '/auth/login')) {
          const status = body.data as LoginStatus;
          csrfTokens.set(base, status.csrfToken);
        }
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
  if (!['GET', 'HEAD'].includes(method) && !csrfTokens.has(base)) {
    await requestOnce<LoginStatus>(base, '/auth/status', 'GET');
  }
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

export const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

export async function mobileLogout() {
  await apiRequest('/auth/logout', { method: 'POST' });
  csrfTokens.clear();
  uni.reLaunch({ url: '/pages/login/index' });
}

export function checkUploadSize(size?: number) {
  if (size != null && size > MAX_UPLOAD_BYTES) {
    throw new ApiError("单个文件不能超过 5 MB");
  }
}

/** Sends credentials for both same-origin H5 and cross-origin Capacitor uploads. */
export async function uploadFile(filePath: string, node?: NodeName, size?: number) {
  try {
    checkUploadSize(size);
  } catch (error) {
    return Promise.reject(error);
  }
  const p = getServerProfile(),
    uploadNode = node || p.preferred,
    base = nodeUrl(p, uploadNode);
  if (!base) return Promise.reject(new ApiError("请先配置服务器地址"));
  if (!csrfTokens.has(base)) await requestOnce<LoginStatus>(base, '/auth/status', 'GET');
  // #ifdef H5
  // The bundled uni.uploadFile implementation omits credentials on cross-origin H5 uploads.
  const form = new FormData();
  const file = await fetch(filePath).then((response) => response.blob());
  const suffix = ({
    'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp', 'image/gif': '.gif', 'application/pdf': '.pdf',
    'text/plain': '.txt', 'application/zip': '.zip', 'application/msword': '.doc',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': '.docx',
    'application/vnd.ms-excel': '.xls', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': '.xlsx',
    'application/vnd.ms-powerpoint': '.ppt', 'application/vnd.openxmlformats-officedocument.presentationml.presentation': '.pptx',
  } as Record<string, string>)[file.type] || '';
  const fileName = file instanceof File ? file.name : `attachment${suffix}`;
  form.append('file', file, fileName);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), p.timeoutMs);
  let response: Response;
  try {
    response = await fetch(`${base}/files/upload`, {
      method: 'POST', credentials: 'include', body: form, signal: controller.signal,
      headers: { 'X-CSRF-Token': csrfTokens.get(base) || '' },
    });
  } finally { clearTimeout(timer); }
  if (response.status === 401) requireLogin();
  const body = await response.json();
  if (!response.ok || body.code !== 200) throw new ApiError(body.msg || '上传失败', response.status);
  markActiveNode(uploadNode, p);
  return body.data as { url: string; objectKey: string };
  // #endif
  // #ifndef H5
  return new Promise<{ url: string; objectKey: string }>((resolve, reject) =>
    uni.uploadFile({
      url: `${base}/files/upload`,
      filePath,
      name: "file",
      timeout: p.timeoutMs,
      header: { 'X-CSRF-Token': csrfTokens.get(base) || '' },
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
  // #endif
}
