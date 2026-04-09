import { formatTableSizeLabel } from "@/lib/liveAccessShared";
import type { LiveMatchRow } from "@/lib/types";

type Props = {
  live: LiveMatchRow;
};

type Ball = {
  id: string;
  x: number;
  y: number;
  fill: string;
  label?: string;
  darkLabel?: boolean;
};

const TABLE_W = 2000;
const TABLE_H = 1000;

function nx(x: number): number {
  return x * TABLE_W;
}

function ny(y: number): number {
  return y * TABLE_H;
}

function createRedPackPoints(): Array<{ x: number; y: number }> {
  const apexX = 0.758;
  const apexY = 0.5;
  const dx = 0.028;
  const dy = 0.049;

  const points: Array<{ x: number; y: number }> = [];
  for (let row = 1; row <= 5; row++) {
    const x = apexX + (row - 1) * dx;
    const yStart = apexY - ((row - 1) * dy) / 2;
    for (let col = 0; col < row; col++) {
      points.push({ x, y: yStart + col * dy });
    }
  }
  return points;
}

function Ball3D({ ball, radius }: { ball: Ball; radius: number }) {
  const labelColor = ball.darkLabel ? "#2d2208" : "#fefefe";
  return (
    <g>
      <circle cx={ball.x + radius * 0.22} cy={ball.y + radius * 0.24} r={radius * 1.08} fill="rgba(0,0,0,0.44)" />
      <circle cx={ball.x} cy={ball.y} r={radius} fill={ball.fill} />
      <circle cx={ball.x} cy={ball.y} r={radius} fill="url(#ballShade)" />
      <circle cx={ball.x - radius * 0.28} cy={ball.y - radius * 0.32} r={radius * 0.27} fill="rgba(255,255,255,0.82)" />
      <circle cx={ball.x - radius * 0.16} cy={ball.y - radius * 0.16} r={radius * 0.58} fill="rgba(255,255,255,0.15)" />
      {ball.label ? (
        <text
          x={ball.x}
          y={ball.y + radius * 0.35}
          textAnchor="middle"
          fontSize={radius * 0.95}
          fontWeight={700}
          fill={labelColor}
          style={{ paintOrder: "stroke", stroke: "rgba(0,0,0,0.45)", strokeWidth: 1.8 }}
        >
          {ball.label}
        </text>
      ) : null}
    </g>
  );
}

export function SnookerTablePreview({ live }: Props) {
  const redPoints = createRedPackPoints().slice(0, Math.max(0, Math.min(15, live.reds_remaining)));
  const ballRadius = TABLE_H * 0.03;

  const balls: Ball[] = [
    { id: "cue", x: nx(0.115), y: ny(0.5), fill: "#f5f5f5" },
    ...(live.green_visible ? [{ id: "green", x: nx(0.248), y: ny(0.32), fill: "#2f9d55", label: "3" }] : []),
    ...(live.brown_visible ? [{ id: "brown", x: nx(0.248), y: ny(0.5), fill: "#6e4a36", label: "4" }] : []),
    ...(live.yellow_visible ? [{ id: "yellow", x: nx(0.248), y: ny(0.68), fill: "#f6cb3d", label: "2", darkLabel: true }] : []),
    ...(live.blue_visible ? [{ id: "blue", x: nx(0.5), y: ny(0.5), fill: "#2366bb", label: "5" }] : []),
    ...(live.pink_visible ? [{ id: "pink", x: nx(0.72), y: ny(0.5), fill: "#eb8fae", label: "6", darkLabel: true }] : []),
    ...redPoints.map((point, index) => ({
      id: `red-${index}`,
      x: nx(point.x),
      y: ny(point.y),
      fill: "#b72033"
    })),
    ...(live.black_visible ? [{ id: "black", x: nx(0.912), y: ny(0.5), fill: "#121212", label: "7" }] : [])
  ];

  const inset = TABLE_H * 0.072;
  const feltLeft = inset;
  const feltTop = inset;
  const feltWidth = TABLE_W - inset * 2;
  const feltHeight = TABLE_H - inset * 2;
  const baulkX = feltLeft + feltWidth * 0.265;
  const dRadius = feltHeight * 0.295;
  const dCy = feltTop + feltHeight * 0.5;
  const logoWidth = feltWidth * 0.54;
  const logoHeight = logoWidth * (203 / 648);
  const logoX = feltLeft + (feltWidth - logoWidth) / 2;
  const logoY = feltTop + (feltHeight - logoHeight) / 2;

  const pocketRadius = inset * 0.9;
  const pocketPositions = [
    { x: inset * 0.46, y: inset * 0.46 },
    { x: TABLE_W * 0.5, y: inset * 0.3 },
    { x: TABLE_W - inset * 0.46, y: inset * 0.46 },
    { x: inset * 0.46, y: TABLE_H - inset * 0.46 },
    { x: TABLE_W * 0.5, y: TABLE_H - inset * 0.3 },
    { x: TABLE_W - inset * 0.46, y: TABLE_H - inset * 0.46 }
  ];

  return (
    <div className="snooker-table-shell">
      <div className="snooker-table-head">
        <h3 className="snooker-table-title">Live Table State</h3>
        <span className="table-size-badge">{formatTableSizeLabel(live.table_size)}</span>
      </div>
      <div className="snooker-table-wrap">
        <svg viewBox={`0 0 ${TABLE_W} ${TABLE_H}`} className="snooker-table" role="img" aria-label="Snooker table state">
          <defs>
            <linearGradient id="frameGradient" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor="#6b3a18" />
              <stop offset="50%" stopColor="#3d1f08" />
              <stop offset="100%" stopColor="#6b3a18" />
            </linearGradient>
            <radialGradient id="feltGradient" cx="50%" cy="50%" r="58%">
              <stop offset="0%" stopColor="#31a863" />
              <stop offset="55%" stopColor="#237a4e" />
              <stop offset="100%" stopColor="#195c38" />
            </radialGradient>
            <radialGradient id="feltVignette" cx="50%" cy="50%" r="60%">
              <stop offset="55%" stopColor="rgba(0,0,0,0)" />
              <stop offset="100%" stopColor="rgba(0,0,0,0.24)" />
            </radialGradient>
            <radialGradient id="ballShade" cx="50%" cy="50%" r="50%">
              <stop offset="60%" stopColor="rgba(0,0,0,0)" />
              <stop offset="100%" stopColor="rgba(0,0,0,0.30)" />
            </radialGradient>
          </defs>

          <rect width={TABLE_W} height={TABLE_H} rx="34" fill="url(#frameGradient)" />
          <rect width={TABLE_W} height={TABLE_H} rx="34" fill="none" stroke="rgba(255,255,255,0.15)" strokeWidth="4" />

          <rect x={feltLeft} y={feltTop} width={feltWidth} height={feltHeight} rx="18" fill="#1f7a47" />
          <rect x={feltLeft} y={feltTop} width={feltWidth} height={feltHeight} rx="18" fill="url(#feltGradient)" />
          <rect x={feltLeft} y={feltTop} width={feltWidth} height={feltHeight} rx="18" fill="url(#feltVignette)" />
          <rect x={feltLeft} y={feltTop} width={feltWidth} height={feltHeight} rx="18" fill="none" stroke="rgba(255,255,255,0.22)" strokeWidth="3" />

          <line
            x1={baulkX}
            y1={feltTop + feltHeight * 0.06}
            x2={baulkX}
            y2={feltTop + feltHeight * 0.94}
            stroke="rgba(255,255,255,0.64)"
            strokeWidth="3"
          />
          <path
            d={`M ${baulkX} ${dCy - dRadius} A ${dRadius} ${dRadius} 0 0 1 ${baulkX} ${dCy + dRadius}`}
            fill="none"
            stroke="rgba(255,255,255,0.64)"
            strokeWidth="3"
          />

          <circle cx={feltLeft + feltWidth * 0.735} cy={feltTop + feltHeight * 0.5} r="7" fill="rgba(255,255,255,0.35)" />
          <circle cx={feltLeft + feltWidth * 0.908} cy={feltTop + feltHeight * 0.5} r="7" fill="rgba(255,255,255,0.35)" />

          {pocketPositions.map((pocket, idx) => (
            <g key={`pocket-${idx}`}>
              <circle cx={pocket.x} cy={pocket.y} r={pocketRadius} fill="#110a03" />
              <circle cx={pocket.x} cy={pocket.y} r={pocketRadius * 0.68} fill="#060302" />
            <circle cx={pocket.x} cy={pocket.y} r={pocketRadius} fill="none" stroke="rgba(255,255,255,0.16)" strokeWidth="2" />
            </g>
          ))}

          <image
            href="/elocho_logo.png"
            x={logoX}
            y={logoY}
            width={logoWidth}
            height={logoHeight}
            preserveAspectRatio="xMidYMid meet"
            opacity="0.24"
          />

          {balls.map((ball) => (
            <Ball3D key={ball.id} ball={ball} radius={ballRadius} />
          ))}

          {live.reds_remaining > 0 ? (
            <text
              x={nx(0.81)}
              y={ny(0.735)}
              textAnchor="middle"
              fontSize="30"
              fontWeight={800}
              fill="#ffffff"
              style={{ paintOrder: "stroke", stroke: "rgba(0,0,0,0.6)", strokeWidth: 2.4 }}
            >
              x{live.reds_remaining}
            </text>
          ) : null}
        </svg>
      </div>
      <p className="snooker-table-meta">Reds remaining: {live.reds_remaining} · Table size: {formatTableSizeLabel(live.table_size)}</p>
    </div>
  );
}
