"use client";

import { useEffect, useMemo, useState } from "react";
import { browserSupabase } from "@/lib/supabase/browserClient";
import { SnookerTablePreview } from "@/components/SnookerTablePreview";
import type { LiveMatchRow } from "@/lib/types";

type Props = {
  initialLive: LiveMatchRow | null;
};

function normalizeLiveRow(value: unknown): LiveMatchRow | null {
  if (!value || typeof value !== "object") return null;
  const row = value as Record<string, unknown>;
  return {
    id: String(row.id ?? "active"),
    is_active: Boolean(row.is_active),
    player1_id: typeof row.player1_id === "number" ? row.player1_id : null,
    player2_id: typeof row.player2_id === "number" ? row.player2_id : null,
    player1_name: typeof row.player1_name === "string" ? row.player1_name : null,
    player2_name: typeof row.player2_name === "string" ? row.player2_name : null,
    player1_score: typeof row.player1_score === "number" ? row.player1_score : 0,
    player2_score: typeof row.player2_score === "number" ? row.player2_score : 0,
    active_player_number: typeof row.active_player_number === "number" ? row.active_player_number : null,
    current_break_player1: typeof row.current_break_player1 === "number" ? row.current_break_player1 : 0,
    current_break_player2: typeof row.current_break_player2 === "number" ? row.current_break_player2 : 0,
    highest_break_player1: typeof row.highest_break_player1 === "number" ? row.highest_break_player1 : 0,
    highest_break_player2: typeof row.highest_break_player2 === "number" ? row.highest_break_player2 : 0,
    highest_break_in_match: typeof row.highest_break_in_match === "number" ? row.highest_break_in_match : 0,
    reds_remaining: typeof row.reds_remaining === "number" ? row.reds_remaining : 15,
    yellow_visible: row.yellow_visible !== false,
    green_visible: row.green_visible !== false,
    brown_visible: row.brown_visible !== false,
    blue_visible: row.blue_visible !== false,
    pink_visible: row.pink_visible !== false,
    black_visible: row.black_visible !== false,
    tournament_id: typeof row.tournament_id === "number" ? row.tournament_id : null,
    tournament_round: typeof row.tournament_round === "number" ? row.tournament_round : null,
    tournament_match_id: typeof row.tournament_match_id === "number" ? row.tournament_match_id : null,
    source_updated_at_ms: typeof row.source_updated_at_ms === "number" ? row.source_updated_at_ms : Date.now()
  };
}

export function LiveMatchClient({ initialLive }: Props) {
  const [live, setLive] = useState<LiveMatchRow | null>(initialLive);
  const [isRefreshing, setIsRefreshing] = useState(false);

  async function refreshLive() {
    const { data } = await browserSupabase
      .from("live_match_state")
      .select("*")
      .eq("id", "active")
      .limit(1);

    const row = ((data ?? []) as LiveMatchRow[])[0] ?? null;
    setLive(row);
  }

  useEffect(() => {
    const pollId = window.setInterval(() => {
      setIsRefreshing(true);
      refreshLive().finally(() => setIsRefreshing(false));
    }, 1200);

    const channel = browserSupabase
      .channel("live-match-realtime")
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
        }
      )
      .subscribe();

    return () => {
      window.clearInterval(pollId);
      void browserSupabase.removeChannel(channel);
    };
  }, []);

  const statusNote = useMemo(() => (isRefreshing ? "Refreshing..." : "Live sync active"), [isRefreshing]);

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Live Match Scoreboard</h1>
        <p className="page-subtitle">Public live scoreboard powered by Supabase sync. {statusNote}</p>
      </section>

      {!live || !live.is_active ? (
        <section className="empty">No active live match right now.</section>
      ) : (
        <section className="panel">
          <div className="panel-header">
            <h2 style={{ margin: 0 }}>Current Match</h2>
          </div>
          <div className="panel-body grid grid-2">
            <article className="panel" style={{ padding: "1rem" }}>
              <h3 style={{ marginTop: 0 }}>{live.player1_name ?? "Player 1"}</h3>
              <p style={{ fontSize: "2rem", margin: "0.2rem 0" }}>{live.player1_score}</p>
              <p>Current break: {live.current_break_player1}</p>
              <p>Highest break: {live.highest_break_player1}</p>
            </article>
            <article className="panel" style={{ padding: "1rem" }}>
              <h3 style={{ marginTop: 0 }}>{live.player2_name ?? "Player 2"}</h3>
              <p style={{ fontSize: "2rem", margin: "0.2rem 0" }}>{live.player2_score}</p>
              <p>Current break: {live.current_break_player2}</p>
              <p>Highest break: {live.highest_break_player2}</p>
            </article>
          </div>
          <div className="panel-body" style={{ paddingTop: 0 }}>
            <SnookerTablePreview live={live} />
          </div>
          <div className="panel-body" style={{ paddingTop: 0 }}>
            <dl className="kv">
              <dt>Active player</dt>
              <dd>{live.active_player_number ?? "-"}</dd>
              <dt>Reds remaining</dt>
              <dd>{live.reds_remaining}</dd>
              <dt>Highest break (match)</dt>
              <dd>{live.highest_break_in_match}</dd>
              <dt>Tournament context</dt>
              <dd>{live.tournament_id ? `Tournament #${live.tournament_id}, Round ${live.tournament_round ?? "-"}` : "Quick match"}</dd>
            </dl>
          </div>
        </section>
      )}
    </div>
  );
}
