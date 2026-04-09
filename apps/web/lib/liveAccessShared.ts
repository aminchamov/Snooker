export const LIVE_ACCESS_COOKIE = "elocho-live-access";
const LIVE_ACCESS_TOKEN = "granted";
const LIVE_ACCESS_PASSWORD = "12345678";

export function validateLiveAccessPassword(password: string): boolean {
  return password === LIVE_ACCESS_PASSWORD;
}

export function liveAccessCookieValue(): string {
  return LIVE_ACCESS_TOKEN;
}

export function normalizeTableSize(value: unknown): "10ft" | "12ft" {
  if (typeof value === "string" && value.trim().toLowerCase() === "10ft") return "10ft";
  return "12ft";
}

export function formatTableSizeLabel(value: unknown): string {
  return normalizeTableSize(value) === "10ft" ? "10 FT" : "12 FT";
}
