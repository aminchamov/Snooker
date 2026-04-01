import Link from "next/link";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import type { PlayerRankingRow, PlayerRow } from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function PlayersPage() {
  const supabase = createPublicSupabaseClient();

  const [{ data: rankings, error: rankingError }, { data: players, error: playersError }] = await Promise.all([
    supabase.from("player_rankings").select("*").order("wins", { ascending: false }).order("max_break", { ascending: false }),
    supabase.from("players").select("*").eq("archived", false).order("name", { ascending: true })
  ]);

  const rankingRows = rankingError ? ([] as PlayerRankingRow[]) : ((rankings ?? []) as PlayerRankingRow[]);
  const playerRows = playersError ? ([] as PlayerRow[]) : ((players ?? []) as PlayerRow[]);

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Players & Rankings</h1>
        <p className="page-subtitle">Public leaderboard and full player directory. No login required.</p>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Rankings</h2>
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
                  <th>Tournaments Won</th>
                </tr>
              </thead>
              <tbody>
                {rankingRows.map((row, index) => (
                  <tr key={row.player_id}>
                    <td>{index + 1}</td>
                    <td>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: "0.5rem" }}>
                        <img
                          src={row.image_uri ?? "/elocho_logo.png"}
                          alt={`${row.name} avatar`}
                          className="avatar-mini"
                        />
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

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Player Directory</h2>
        </div>
        <div className="panel-body">
          {playerRows.length === 0 ? (
            <div className="empty">No players synced yet.</div>
          ) : (
            <div className="grid" style={{ gap: "0.65rem" }}>
              {playerRows.map((player) => (
                <Link key={player.id} href={`/players/${player.id}`} className="player-card">
                  <img
                    src={player.image_uri ?? "/elocho_logo.png"}
                    alt={`${player.name} avatar`}
                    className="avatar"
                  />
                  <div className="info">
                    <div className="name">{player.name}</div>
                    <div className="stats">
                      <span>
                        W <span className="stat-value">{player.wins}</span>
                      </span>
                      <span>
                        D <span className="stat-value">{player.draws}</span>
                      </span>
                      <span>
                        L <span className="stat-value">{player.losses}</span>
                      </span>
                      <span>
                        Games <span className="stat-value">{player.total_matches}</span>
                      </span>
                    </div>
                  </div>
                  <span className="action">Open</span>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
