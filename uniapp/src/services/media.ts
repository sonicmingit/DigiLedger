import {
  activeNode,
  getServerProfile,
  nodeOrigin,
  nodeUrl,
  type NodeName,
  type ServerProfile,
} from "./server-profile";

export type ResolveMediaUrlOptions = {
  /** Override the active node for a request-bound preview when necessary. */
  node?: NodeName;
  profile?: ServerProfile;
};

const bucketPattern = /^[A-Za-z0-9][A-Za-z0-9._-]*$/;
const observedBuckets = new Set<string>();

function buildOssPath(bucket: string, objectKey: string) {
  const cleanBucket = bucket.trim();
  const cleanKey = objectKey.replace(/^\/+/, "");
  if (
    !bucketPattern.test(cleanBucket) ||
    !cleanKey ||
    cleanKey.split("/").some((part) => !part || part === "." || part === "..")
  ) {
    return "";
  }
  return `/oss/${cleanBucket}/${cleanKey}`;
}

function ossPathFromPathname(pathname: string) {
  const path = pathname.replace(/^\/api(?=\/|$)/i, "");
  const match = /^\/oss\/([^/?#]+)\/(.+)$/i.exec(path);
  if (!match) return "";
  const ossPath = buildOssPath(match[1], match[2]);
  if (ossPath) observedBuckets.add(match[1]);
  return ossPath;
}

function isLikelyMinioEndpoint(url: URL) {
  const hostname = url.hostname.toLowerCase();
  return (
    /(^|[.-])minio([.-]|$)/i.test(hostname) ||
    url.port === "9000" ||
    url.port === "19000"
  );
}

function legacyMinioPath(url: URL) {
  if (!isLikelyMinioEndpoint(url)) return "";
  const parts = url.pathname.split("/").filter(Boolean);
  if (parts[0]?.toLowerCase() === "minio") parts.shift();
  if (parts[0]?.toLowerCase() === "api") parts.shift();
  const bucket = parts.shift() || "";
  const objectKey = parts.join("/");
  const ossPath = buildOssPath(bucket, objectKey);
  if (!ossPath) return "";
  observedBuckets.add(bucket);
  return ossPath;
}

function selectNode(profile: ServerProfile, requested?: NodeName) {
  const first = requested || activeNode(profile);
  const second: NodeName = first === "primary" ? "secondary" : "primary";
  return [first, second, profile.preferred].find((node, index, nodes) =>
    nodes.indexOf(node) === index && !!nodeUrl(profile, node),
  );
}

function withCurrentServer(path: string, profile: ServerProfile, node?: NodeName) {
  const selected = selectNode(profile, node);
  const origin = selected ? nodeOrigin(profile, selected) : "";
  return `${origin.replace(/\/$/, "")}${path}`;
}

function resolveFromAbsoluteUrl(
  value: string,
  profile: ServerProfile,
  node?: NodeName,
) {
  let parsed: URL;
  try {
    parsed = new URL(value.startsWith("//") ? `http:${value}` : value);
  } catch (_) {
    return value;
  }

  const proxyPath = ossPathFromPathname(parsed.pathname);
  if (proxyPath) {
    return `${withCurrentServer(proxyPath, profile, node)}${parsed.search}${parsed.hash}`;
  }

  const legacyPath = legacyMinioPath(parsed);
  if (legacyPath) return withCurrentServer(legacyPath, profile, node);
  return value;
}

/**
 * Resolve backend media paths for H5 and native Android.
 *
 * `/oss/{bucket}/{objectKey}` is served by the API node, so native builds
 * need the active node's origin while H5 can keep a relative URL. Static
 * assets and ordinary external URLs are deliberately left untouched.
 */
export function resolveMediaUrl(
  value?: string | null,
  options: ResolveMediaUrlOptions = {},
) {
  const raw = String(value || "").trim();
  if (!raw || /^\/static(?:\/|$)/i.test(raw)) return raw;

  const profile = options.profile || getServerProfile();
  if (/^\/oss\//i.test(raw) || /^\/api\/oss\//i.test(raw)) {
    const path = ossPathFromPathname(raw.split(/[?#]/, 1)[0]);
    if (!path) return raw;
    const suffix = raw.slice(raw.indexOf(path) + path.length);
    return `${withCurrentServer(path, profile, options.node)}${suffix}`;
  }

  if (/^(?:https?:)?\/\//i.test(raw)) {
    return resolveFromAbsoluteUrl(raw, profile, options.node);
  }

  return raw;
}

export function resolveMediaUrls(
  values?: Array<string | null | undefined>,
  options: ResolveMediaUrlOptions = {},
) {
  return (values || [])
    .map((value) => resolveMediaUrl(value, options))
    .filter((value): value is string => !!value);
}
