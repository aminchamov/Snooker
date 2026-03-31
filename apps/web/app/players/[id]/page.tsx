import Link from "next/link";
import { notFound } from "next/navigation";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import type { MatchRow, PlayerRankingRow, PlayerRow, TournamentRow } from "@/lib/types";

type Props = {
  params: Promise<{ id: string }>;
};

export const dynamic = "force-dynamic";

export default async function PlayerDetailPage({ params }: Props) {
  const { id } = await params;
  const playerId = Number(id);
  if (Number.isNaN(playerId)) notFound();

  const supabase = createPublicSupabaseClient();

  const [playerRes, rankingRes, matchesRes, wonTournamentsRes] = await Promise.all([
    supabase.from("players").select("*").eq("id", playerId).single(),
    supabase.from("player_rankings").select("*").eq("player_id", playerId).single(),
    supabase
      .from("matches")
      .select("*")
      .or(`player1_id.eq.${playerId},player2_id.eq.${playerId}`)
      .order("started_at_ms", { ascending: false })
      .limit(15),
    supabase.from("tournaments").select("*").eq("champion_player_id", playerId)
  ]);

  if (!playerRes.data) notFound();

  const p = playerRes.data as PlayerRow;
  const r = rankingRes.data as PlayerRankingRow | null;
  const recentMatches = (matchesRes.data ?? []) as MatchRow[];
  const tournaments = (wonTournamentsRes.data ?? []) as TournamentRow[];

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">{p.name}</h1>
        <p className="page-subtitle">Player details and latest synced statistics.</p>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Statistics</h2>
        </div>
        <div className="panel-body">
          <dl className="kv">
            <dt>Games played</dt>
            <dd>{r?.games_played ?? p.total_matches}</dd>
            <dt>Wins</dt>
            <dd>{r?.wins ?? p.wins}</dd>
            <dt>Draws</dt>
            <dd>{r?.draws ?? p.draws}</dd>
            <dt>Losses</dt>
            <dd>{r?.losses ?? p.losses}</dd>
            <dt>Max break</dt>
            <dd>{r?.max_break ?? 0}</dd>
            <dt>Average points per match</dt>
            <dd>{Number(r?.average_points_per_match ?? 0).toFixed(2)}</dd>
            <dt>Tournaments won</dt>
            <dd>{r?.tournaments_won ?? p.tournaments_won}</dd>
          </dl>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Recent Matches</h2>
        </div>
        <div className="panel-body table-wrap">
          {recentMatches.length === 0 ? (
            <div className="empty">No completed matches yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Match ID</th>
                  <th>Score</th>
                  <th>Type</th>
                  <th>Winner</th>
                </tr>
              </thead>
              <tbody>
                {recentMatches.map((match) => (
                  <tr key={match.id}>
                    <td>{match.id}</td>
                    <td>
                      {match.player1_score} - {match.player2_score}
                    </td>
                    <td>{match.match_type}</td>
                    <td>{match.is_draw ? "Draw" : match.winner_player_id ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Tournaments Won</h2>
        </div>
        <div className="panel-body">
          {tournaments.length === 0 ? (
            <div className="empty">No tournament wins yet.</div>
          ) : (
            <ul>
              {tournaments.map((t) => (
                <li key={t.id}>
                  <Link href={`/tournaments/${t.id}`}>{t.name}</Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}
