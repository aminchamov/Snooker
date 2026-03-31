export function formatDateFromMs(ms: number | null | undefined): string {
  if (!ms || Number.isNaN(ms)) return "-";
  return new Date(ms).toLocaleString();
}

export function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    if (!Number.isNaN(parsed)) return parsed;
  }
  return fallback;
}
