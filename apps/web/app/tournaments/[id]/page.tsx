import { notFound } from "next/navigation";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import { TournamentBracketView } from "@/components/TournamentBracketView";
import type { MatchRow, PlayerRow, TournamentMatchRow, TournamentRow } from "@/lib/types";

type Props = {
  params: Promise<{ id: string }>;
};

export const dynamic = "force-dynamic";

export default async function TournamentDetailPage({ params }: Props) {
  const { id } = await params;
  const tournamentId = Number(id);
  if (Number.isNaN(tournamentId)) notFound();

  const supabase = createPublicSupabaseClient();

  const [tournamentRes, bracketMatchesRes, playersRes, matchesRes] = await Promise.all([
    supabase.from("tournaments").select("*").eq("id", tournamentId).single(),
    supabase
      .from("tournament_matches")
      .select("*")
      .eq("tournament_id", tournamentId)
      .order("round_number", { ascending: true })
      .order("bracket_position", { ascending: true }),
    supabase.from("players").select("*").eq("archived", false),
    supabase.from("matches").select("*").eq("tournament_id", tournamentId)
  ]);

  if (!tournamentRes.data) notFound();

  const t = tournamentRes.data as TournamentRow;
  const tmRows = (bracketMatchesRes.data ?? []) as TournamentMatchRow[];
  const playerRows = (playersRes.data ?? []) as PlayerRow[];
  const playerMap = new Map(playerRows.map((p) => [p.id, p.name]));
  const resultRows = (matchesRes.data ?? []) as MatchRow[];

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">{t.name}</h1>
        <p className="page-subtitle">Bracket and match progress.</p>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Tournament Overview</h2>
        </div>
        <div className="panel-body">
          <dl className="kv">
            <dt>Status</dt>
            <dd>{t.status}</dd>
            <dt>Player count</dt>
            <dd>{t.player_count}</dd>
            <dt>Total rounds</dt>
            <dd>{t.total_rounds}</dd>
            <dt>Champion</dt>
            <dd>{t.champion_player_id ? playerMap.get(t.champion_player_id) ?? `#${t.champion_player_id}` : "-"}</dd>
          </dl>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Bracket</h2>
        </div>
        <div className="panel-body">
          <TournamentBracketView tournament={t} matches={tmRows} playerNameById={playerMap} />
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Recorded Tournament Matches</h2>
        </div>
        <div className="panel-body table-wrap">
          {resultRows.length === 0 ? (
            <div className="empty">No completed tournament games yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Match ID</th>
                  <th>Score</th>
                  <th>Winner</th>
                  <th>Highest Break</th>
                </tr>
              </thead>
              <tbody>
                {resultRows.map((m) => (
                  <tr key={m.id}>
                    <td>{m.id}</td>
                    <td>
                      {m.player1_score} - {m.player2_score}
                    </td>
                    <td>{m.is_draw ? "Draw" : m.winner_player_id ? playerMap.get(m.winner_player_id) ?? `#${m.winner_player_id}` : "-"}</td>
                    <td>{m.match_highest_break}</td>
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
