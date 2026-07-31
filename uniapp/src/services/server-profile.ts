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
