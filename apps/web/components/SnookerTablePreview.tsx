import type { LiveMatchRow } from "@/lib/types";

type Props = {
  live: LiveMatchRow;
};

type Dot = {
  x: number;
  y: number;
  color: string;
  stroke?: string;
};

function generateReds(count: number): Dot[] {
  const total = Math.max(0, Math.min(15, count));
  const rows = [1, 2, 3, 4, 5];
  const dots: Dot[] = [];
  let remaining = total;
  let y = 130;

  for (let r = 0; r < rows.length && remaining > 0; r++) {
    const inRow = Math.min(rows[r], remaining);
    const startX = 260 - ((inRow - 1) * 8);
    for (let i = 0; i < inRow; i++) {
      dots.push({ x: startX + i * 16, y, color: "#b72033" });
    }
    remaining -= inRow;
    y += 16;
  }
  return dots;
}

export function SnookerTablePreview({ live }: Props) {
  const reds = generateReds(live.reds_remaining);
  const colors: Dot[] = [
    live.yellow_visible ? { x: 86, y: 222, color: "#f6cb3d" } : null,
    live.green_visible ? { x: 86, y: 78, color: "#2f9d55" } : null,
    live.brown_visible ? { x: 120, y: 150, color: "#6e4a36" } : null,
    live.blue_visible ? { x: 220, y: 150, color: "#2366bb" } : null,
    live.pink_visible ? { x: 280, y: 150, color: "#eb8fae" } : null,
    live.black_visible ? { x: 340, y: 150, color: "#1a1a1a", stroke: "#6d6d6d" } : null
  ].filter((item): item is Dot => Boolean(item));

  return (
    <div className="snooker-table-shell">
      <h3 className="snooker-table-title">Live Table State</h3>
      <svg viewBox="0 0 430 300" className="snooker-table" role="img" aria-label="Snooker table state">
        <rect x="10" y="10" width="410" height="280" rx="20" fill="#4f2f1f" />
        <rect x="26" y="26" width="378" height="248" rx="12" fill="#1f6f43" stroke="#2d8b56" strokeWidth="2" />

        <line x1="120" y1="26" x2="120" y2="274" stroke="#e8d8a8" strokeWidth="2" opacity="0.7" />
        <path d="M 120 90 A 58 58 0 0 1 120 210" stroke="#e8d8a8" strokeWidth="2" fill="none" opacity="0.7" />

        <circle cx="26" cy="26" r="8" fill="#0d0d0d" />
        <circle cx="215" cy="26" r="7" fill="#0d0d0d" />
        <circle cx="404" cy="26" r="8" fill="#0d0d0d" />
        <circle cx="26" cy="274" r="8" fill="#0d0d0d" />
        <circle cx="215" cy="274" r="7" fill="#0d0d0d" />
        <circle cx="404" cy="274" r="8" fill="#0d0d0d" />

        {reds.map((dot, idx) => (
          <circle key={`r-${idx}`} cx={dot.x} cy={dot.y} r="6" fill={dot.color} />
        ))}

        {colors.map((dot, idx) => (
          <circle key={`c-${idx}`} cx={dot.x} cy={dot.y} r="6.5" fill={dot.color} stroke={dot.stroke ?? "#ffffff22"} strokeWidth="1.2" />
        ))}

        <circle cx="56" cy="150" r="6.5" fill="#f8f8f8" stroke="#c7c7c7" strokeWidth="1" />
      </svg>
      <p className="snooker-table-meta">Reds remaining: {live.reds_remaining}</p>
    </div>
  );
}
