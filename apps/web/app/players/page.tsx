import Link from "next/link";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import type { PlayerRankingRow } from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function PlayersPage() {
  const supabase = createPublicSupabaseClient();

  const { data: rankings, error: rankingError } = await supabase
    .from("player_rankings")
    .select("*")
    .order("wins", { ascending: false })
    .order("max_break", { ascending: false });

  const rankingRows = rankingError ? ([] as PlayerRankingRow[]) : ((rankings ?? []) as PlayerRankingRow[]);

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Players Rankings</h1>
        <p className="page-subtitle">Public leaderboard with direct access to each player profile and statistics.</p>
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
    </div>
  );
}
