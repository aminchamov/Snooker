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
                      <Link href={`/players/${row.player_id}`}>{row.name}</Link>
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
          <h2 style={{ margin: 0 }}>Player List</h2>
        </div>
        <div className="panel-body table-wrap">
          {playerRows.length === 0 ? (
            <div className="empty">No players synced yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Wins</th>
                  <th>Draws</th>
                  <th>Losses</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {playerRows.map((player) => (
                  <tr key={player.id}>
                    <td>{player.id}</td>
                    <td>{player.name}</td>
                    <td>{player.wins}</td>
                    <td>{player.draws}</td>
                    <td>{player.losses}</td>
                    <td>
                      <Link href={`/players/${player.id}`}>View details</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>
    </div>
  );
}
