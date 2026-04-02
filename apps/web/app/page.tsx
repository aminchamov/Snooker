import Link from "next/link";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import type { LiveMatchRow, PlayerRankingRow } from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  const supabase = createPublicSupabaseClient();

  const [{ data: liveRows }, { data: rankings, error: rankingError }] = await Promise.all([
    supabase.from("live_match_state").select("*").eq("id", "active").limit(1),
    supabase
      .from("player_rankings")
      .select("*")
      .eq("archived", false)
      .order("wins", { ascending: false })
      .order("max_break", { ascending: false })
  ]);

  const live = ((liveRows ?? []) as LiveMatchRow[])[0] ?? null;
  const rankingRows = rankingError ? ([] as PlayerRankingRow[]) : ((rankings ?? []) as PlayerRankingRow[]);
  const hasLive = !!live?.is_active;

  return (
    <div className="grid home-grid">
      <section className="home-hero panel">
        <div className="home-hero-content">
          <p className="home-eyebrow">El Ocho Public Board</p>
          <h1 className="page-title">Live Lounge Pulse, Rankings, and Tournaments</h1>
          <p className="page-subtitle">
            Follow live action, track top players, and jump into tournament brackets in one place.
          </p>
          <div className="home-hero-actions">
            <Link href="/tournaments" className="button">Browse Tournaments</Link>
            <Link href="/players" className="button secondary">All Players</Link>
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header home-section-head">
          <h2 style={{ margin: 0 }}>Live Match</h2>
          {hasLive ? <span className="badge badge-live">Live now</span> : <span className="badge badge-ok">No active game</span>}
        </div>
        <div className="panel-body">
          {hasLive ? (
            <Link href="/live" className="home-live-link">
              <div className="home-live-scoreline">
                <span>{live.player1_name ?? "Player 1"}</span>
                <strong>{live.player1_score} - {live.player2_score}</strong>
                <span>{live.player2_name ?? "Player 2"}</span>
              </div>
              <div className="home-live-meta">
                <span>Queue: {live.queue_count}</span>
                <span>Timer: {Math.floor(Math.max(0, live.elapsed_seconds) / 60)}m {Math.max(0, live.elapsed_seconds) % 60}s</span>
                <span>Pts Left: {live.points_left_to_147}</span>
              </div>
              <span className="home-live-cta">Open Live Scoreboard</span>
            </Link>
          ) : (
            <div className="home-live-idle">
              <p>No live match is running right now.</p>
              <Link href="/live" className="button secondary">Open Live Page</Link>
            </div>
          )}
        </div>
      </section>

      <section className="panel">
        <div className="panel-header home-section-head">
          <h2 style={{ margin: 0 }}>Player Rankings</h2>
          <Link href="/players" className="button outlined">Open Full Rankings</Link>
        </div>
        <div className="panel-body table-wrap">
          {rankingRows.length === 0 ? (
            <div className="empty">No ranking data yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Player</th>
                  <th>Played</th>
                  <th>W</th>
                  <th>D</th>
                  <th>L</th>
                  <th>Max Break</th>
                  <th>Avg Points</th>
                  <th>Titles</th>
                </tr>
              </thead>
              <tbody>
                {rankingRows.map((row, index) => (
                  <tr key={row.player_id}>
                    <td>{index + 1}</td>
                    <td>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: "0.5rem" }}>
                        <img src={row.image_uri ?? "/elocho_logo.png"} alt={`${row.name} avatar`} className="avatar-mini" />
                        <Link href={`/players/${row.player_id}`}>{row.name}</Link>
                      </span>
                    </td>
                    <td>{row.games_played}</td>
                    <td>{row.wins}</td>
                    <td>{row.draws}</td>
                    <td>{row.losses}</td>
                    <td>{row.max_break}</td>
                    <td>{Number(row.average_points_per_match).toFixed(2)}</td>
                    <td>{row.tournaments_won}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section className="panel home-tournament-cta">
        <div className="panel-body">
          <h2 style={{ marginTop: 0 }}>Tournaments</h2>
          <p>View ongoing and completed brackets, live tournament games, and recorded results.</p>
          <Link href="/tournaments" className="button">Go To Tournaments</Link>
        </div>
      </section>
    </div>
  );
}
