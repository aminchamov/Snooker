import Link from "next/link";
import { notFound } from "next/navigation";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import type { MatchRow, PlayerRankingRow, PlayerRow, TournamentRow } from "@/lib/types";

type Props = {
  params: Promise<{ id: string }>;
};

export const dynamic = "force-dynamic";

function isTrackableMatch(match: MatchRow): boolean {
  return !(match.player1_score === 0 && match.player2_score === 0);
}

export default async function PlayerDetailPage({ params }: Props) {
  const { id } = await params;
  const playerId = Number(id);
  if (Number.isNaN(playerId)) notFound();

  const supabase = createPublicSupabaseClient();

  const [playerRes, rankingRes, matchesRes, wonTournamentsRes] = await Promise.all([
    supabase.from("players").select("*").eq("id", playerId).eq("archived", false).single(),
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
  const recentMatches = ((matchesRes.data ?? []) as MatchRow[]).filter(isTrackableMatch);
  const tournaments = (wonTournamentsRes.data ?? []) as TournamentRow[];

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">{p.name}</h1>
        <p className="page-subtitle">Player details and latest synced statistics.</p>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Profile Avatar</h2>
        </div>
        <div className="panel-body" style={{ display: "flex", justifyContent: "center", paddingTop: "1rem" }}>
          <img
            src={p.image_uri ?? "/elocho_logo.png"}
            alt={`${p.name} avatar`}
            className="avatar-large"
          />
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Statistics</h2>
        </div>
        <div className="panel-body">
          <div style={{ display: "flex", flexWrap: "wrap", gap: "0.55rem" }}>
            <div className="stat-chip">
              <span className="label">Games</span>
              <span className="value">{r?.games_played ?? p.total_matches}</span>
            </div>
            <div className="stat-chip">
              <span className="label">Wins</span>
              <span className="value">{r?.wins ?? p.wins}</span>
            </div>
            <div className="stat-chip">
              <span className="label">Draws</span>
              <span className="value">{r?.draws ?? p.draws}</span>
            </div>
            <div className="stat-chip">
              <span className="label">Losses</span>
              <span className="value">{r?.losses ?? p.losses}</span>
            </div>
            <div className="stat-chip">
              <span className="label">Max Break</span>
              <span className="value">{r?.max_break ?? 0}</span>
            </div>
            <div className="stat-chip">
              <span className="label">Avg Points</span>
              <span className="value">{Number(r?.average_points_per_match ?? 0).toFixed(2)}</span>
            </div>
            <div className="stat-chip">
              <span className="label">Titles</span>
              <span className="value">{r?.tournaments_won ?? p.tournaments_won}</span>
            </div>
          </div>
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
            <ul style={{ margin: 0, paddingLeft: "1.2rem" }}>
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
