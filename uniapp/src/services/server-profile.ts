/** Device-local server routing configuration; never uploaded to the backend. */
export type ServerProfile = {
  primaryUrl: string;
  secondaryUrl: string;
  preferred: "primary" | "secondary";
  autoFailover: boolean;
  timeoutMs: number;
};
export type NodeName = "primary" | "secondary";
const KEY = "__digiledger_server_profile_v2__";
let activeNodeState: { node: NodeName; apiUrl: string } | undefined;
const defaults: ServerProfile = {
  primaryUrl: "/api",
  secondaryUrl: "",
  preferred: "primary",
  autoFailover: true,
  timeoutMs: 12000,
};

export function normalizeApiUrl(value: string) {
  const raw = String(value || "")
    .trim()
    .replace(/\/+$/, "");
  if (!raw) return "";
  return /\/api$/i.test(raw) ? raw : `${raw}/api`;
}
export function ensureServerProfile() {
  const p = getServerProfile();
  saveServerProfile(p);
  return p;
}
export function getServerProfile(): ServerProfile {
  try {
    const raw = uni.getStorageSync(KEY);
    if (raw) return { ...defaults, ...JSON.parse(raw) };
  } catch (_) {}
  return { ...defaults };
}
export function saveServerProfile(profile: ServerProfile) {
  const clean = {
    ...profile,
    primaryUrl: normalizeApiUrl(profile.primaryUrl),
    secondaryUrl: normalizeApiUrl(profile.secondaryUrl),
    timeoutMs: Math.min(
      60000,
      Math.max(1000, Number(profile.timeoutMs) || 12000),
    ),
  };
  uni.setStorageSync(KEY, JSON.stringify(clean));
  return clean;
}
export function nodeUrl(profile: ServerProfile, node: NodeName) {
  return normalizeApiUrl(
    node === "primary" ? profile.primaryUrl : profile.secondaryUrl,
  );
}

/**
 * Returns the server base without the API suffix. Relative profiles stay
 * relative so H5 can continue to use the dev-server/reverse-proxy origin.
 */
export function nodeOrigin(profile: ServerProfile, node: NodeName) {
  const apiUrl = nodeUrl(profile, node);
  if (!apiUrl) return "";
  if (/^https?:\/\//i.test(apiUrl)) {
    try {
      return new URL(apiUrl).origin;
    } catch (_) {
      return apiUrl.replace(/\/api\/?$/i, "");
    }
  }
  if (/^\/\//.test(apiUrl)) {
    try {
      const parsed = new URL(`http:${apiUrl}`);
      return `//${parsed.host}`;
    } catch (_) {
      return apiUrl.replace(/\/api\/?$/i, "");
    }
  }
  return apiUrl.replace(/\/api\/?$/i, "");
}

/** Remember the node that actually completed the latest request. */
export function markActiveNode(node: NodeName, profile = getServerProfile()) {
  const apiUrl = nodeUrl(profile, node);
  if (apiUrl) activeNodeState = { node, apiUrl };
}

/** Prefer the last successful node, then the configured preferred node. */
export function activeNode(profile = getServerProfile()): NodeName {
  if (
    activeNodeState &&
    nodeUrl(profile, activeNodeState.node) === activeNodeState.apiUrl
  ) {
    return activeNodeState.node;
  }
  return profile.preferred;
}
