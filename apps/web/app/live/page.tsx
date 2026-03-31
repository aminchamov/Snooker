import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import { SnookerTablePreview } from "@/components/SnookerTablePreview";
import type { LiveMatchRow } from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function LiveMatchPage() {
  const supabase = createPublicSupabaseClient();
  const { data } = await supabase
    .from("live_match_state")
    .select("*")
    .eq("id", "active")
    .limit(1);

  const live = ((data ?? []) as LiveMatchRow[])[0];

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Live Match Scoreboard</h1>
        <p className="page-subtitle">Public live scoreboard powered by Supabase sync.</p>
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
