"use client";

import { useEffect, useMemo, useState } from "react";
import { formatTableSizeLabel, normalizeTableSize } from "@/lib/liveAccessShared";
import { browserSupabase } from "@/lib/supabase/browserClient";
import { SnookerTablePreview } from "@/components/SnookerTablePreview";
import type { LiveMatchRow } from "@/lib/types";

type Props = {
  tournamentId: number;
  initialLive: LiveMatchRow | null;
};

type PlayerImageRow = {
  id: number;
  image_uri: string | null;
};

function readNumber(value: unknown, fallback: number): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return fallback;
}

function readNullableNumber(value: unknown): number | null {
  if (value === null || value === undefined) return null;
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
}

function clampPointsLeft(value: number): number {
  return Math.max(0, Math.min(147, Math.round(value)));
}

function formatElapsed(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const minutes = Math.floor(safe / 60);
  const seconds = safe % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function normalizeLiveRow(value: unknown): LiveMatchRow | null {
  if (!value || typeof value !== "object") return null;
  const row = value as Record<string, unknown>;
  const player1Score = readNumber(row.player1_score, 0);
  const player2Score = readNumber(row.player2_score, 0);
  const fallbackPointsLeft = clampPointsLeft(147 - (player1Score + player2Score));

  return {
    id: String(row.id ?? "active"),
    is_active: row.is_active === true,
    player1_id: readNullableNumber(row.player1_id),
    player2_id: readNullableNumber(row.player2_id),
    player1_name: typeof row.player1_name === "string" ? row.player1_name : null,
    player2_name: typeof row.player2_name === "string" ? row.player2_name : null,
    player1_score: player1Score,
    player2_score: player2Score,
    active_player_number: readNullableNumber(row.active_player_number),
    current_break_player1: readNumber(row.current_break_player1, 0),
    current_break_player2: readNumber(row.current_break_player2, 0),
    highest_break_player1: readNumber(row.highest_break_player1, 0),
    highest_break_player2: readNumber(row.highest_break_player2, 0),
    highest_break_in_match: readNumber(row.highest_break_in_match, 0),
    reds_remaining: readNumber(row.reds_remaining, 15),
    yellow_visible: row.yellow_visible !== false,
    green_visible: row.green_visible !== false,
    brown_visible: row.brown_visible !== false,
    blue_visible: row.blue_visible !== false,
    pink_visible: row.pink_visible !== false,
    black_visible: row.black_visible !== false,
    tournament_id: readNullableNumber(row.tournament_id),
    tournament_round: readNullableNumber(row.tournament_round),
    tournament_match_id: readNullableNumber(row.tournament_match_id),
    elapsed_seconds: readNumber(row.elapsed_seconds, 0),
    queue_count: readNumber(row.queue_count, 0),
    points_left_to_147: clampPointsLeft(readNumber(row.points_left_to_147, fallbackPointsLeft)),
    table_size: normalizeTableSize(row.table_size),
    source_updated_at_ms: readNumber(row.source_updated_at_ms, Date.now())
  };
}

export function TournamentLivePanel({ tournamentId, initialLive }: Props) {
  const [live, setLive] = useState<LiveMatchRow | null>(() => normalizeLiveRow(initialLive));
  const [playerImageById, setPlayerImageById] = useState<Record<number, string>>({});

  const hasLiveTournamentMatch = !!live?.is_active && live.tournament_id === tournamentId;

  async function loadPlayerImages(liveRow: LiveMatchRow | null) {
    if (!liveRow) return;
    const ids = [liveRow.player1_id, liveRow.player2_id].filter((id): id is number => typeof id === "number" && id > 0);
    if (ids.length === 0) return;
    const uniqueIds = Array.from(new Set(ids));
    const { data } = await browserSupabase.from("players").select("id,image_uri").in("id", uniqueIds);
    const rows = (data ?? []) as PlayerImageRow[];
    if (rows.length === 0) return;
    setPlayerImageById((prev) => {
      const next = { ...prev };
      for (const row of rows) next[row.id] = row.image_uri ?? "";
      return next;
    });
  }

  async function refreshLive() {
    const { data } = await browserSupabase
      .from("live_match_state")
      .select("*")
      .eq("id", "active")
      .limit(1);
    const normalized = normalizeLiveRow((data ?? [])[0] ?? null);
    setLive(normalized);
    await loadPlayerImages(normalized);
  }

  useEffect(() => {
    const pollId = window.setInterval(() => {
      void refreshLive();
    }, 1200);

    const channel = browserSupabase
      .channel(`tournament-live-${tournamentId}`)
      .on(
        "postgres_changes",
        {
          event: "*",
          schema: "public",
          table: "live_match_state",
          filter: "id=eq.active"
        },
        (payload) => {
          const normalized = normalizeLiveRow(payload.new);
          setLive(normalized);
          void loadPlayerImages(normalized);
        }
      )
      .subscribe();

    void loadPlayerImages(live);

    return () => {
      window.clearInterval(pollId);
      void browserSupabase.removeChannel(channel);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tournamentId]);

  const activeBreak = useMemo(() => {
    if (!live) return 0;
    if (live.active_player_number === 1) return live.current_break_player1;
    if (live.active_player_number === 2) return live.current_break_player2;
    return 0;
  }, [live]);

  if (!hasLiveTournamentMatch || !live) {
    return <div className="empty">No active live game for this tournament right now.</div>;
  }

  const player1Image = live.player1_id ? playerImageById[live.player1_id] ?? "" : "";
  const player2Image = live.player2_id ? playerImageById[live.player2_id] ?? "" : "";

  return (
    <div className="grid" style={{ gap: "0.9rem" }}>
      <div className="live-scoreboard-top">
        <article className={`live-player-card ${live.active_player_number === 1 ? "is-active" : ""}`}>
          <div className="live-player-head">
            {player1Image ? (
              <img src={player1Image} alt={live.player1_name ?? "Player 1"} className="live-player-avatar" />
            ) : (
              <div className="live-player-avatar live-player-avatar-fallback">{(live.player1_name ?? "P1").trim().charAt(0).toUpperCase()}</div>
            )}
          </div>
          <h3>{live.player1_name ?? "Player 1"}</h3>
          <p className="live-player-score">{live.player1_score}</p>
          <p className="live-player-sub">Break: {live.current_break_player1}</p>
        </article>

        <div className="live-center-stack">
          <div className="live-center-logo-wrap">
            <img src="/elocho_logo.png" alt="El Ocho" className="live-center-logo" />
          </div>
          <div className="live-timer-card">
            <div className="live-timer-main">
              <span>{formatElapsed(live.elapsed_seconds)}</span>
            </div>
            <div className="live-queue-pill" title="Queue">
              <span>{live.queue_count}</span>
            </div>
          </div>
          <div className="live-mini-card">
            <span className="label">TABLE</span>
            <span className="value">{formatTableSizeLabel(live.table_size)}</span>
          </div>
          <div className="live-mini-card">
            <span className="label">PTS LEFT</span>
            <span className="value">{live.points_left_to_147}</span>
          </div>
          <div className="live-mini-card">
            <span className="label">LIVE BREAK</span>
            <span className="value">{activeBreak}</span>
          </div>
        </div>

        <article className={`live-player-card ${live.active_player_number === 2 ? "is-active" : ""}`}>
          <div className="live-player-head">
            {player2Image ? (
              <img src={player2Image} alt={live.player2_name ?? "Player 2"} className="live-player-avatar" />
            ) : (
              <div className="live-player-avatar live-player-avatar-fallback">{(live.player2_name ?? "P2").trim().charAt(0).toUpperCase()}</div>
            )}
          </div>
          <h3>{live.player2_name ?? "Player 2"}</h3>
          <p className="live-player-score">{live.player2_score}</p>
          <p className="live-player-sub">Break: {live.current_break_player2}</p>
        </article>
      </div>

      <SnookerTablePreview live={live} />
    </div>
  );
}
