import Link from "next/link";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import type { LiveMatchRow, TournamentRow } from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function TournamentsPage() {
  const supabase = createPublicSupabaseClient();

  const [{ data: tournaments }, { data: liveRows }] = await Promise.all([
    supabase.from("tournaments").select("*").order("source_created_at_ms", { ascending: false }),
    supabase.from("live_match_state").select("*").eq("id", "active").limit(1)
  ]);

  const rows = (tournaments ?? []) as TournamentRow[];
  const live = ((liveRows ?? []) as LiveMatchRow[])[0];

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Tournaments</h1>
        <p className="page-subtitle">Public tournament list, statuses, and bracket access.</p>
      </section>

      {live?.is_active ? (
        <section className="panel">
          <div className="panel-header">
            <h2 style={{ margin: 0 }}>Live Tournament Match</h2>
          </div>
          <div className="panel-body">
            <span className="badge badge-live">Live now</span>
            <p style={{ marginBottom: 0 }}>
              {live.player1_name ?? "Player 1"} {live.player1_score} - {live.player2_score} {live.player2_name ?? "Player 2"}
              {live.tournament_id ? ` (Tournament #${live.tournament_id})` : ""}
            </p>
          </div>
        </section>
      ) : null}

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>All Tournaments</h2>
        </div>
        <div className="panel-body table-wrap">
          {rows.length === 0 ? (
            <div className="empty">No tournaments synced yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Status</th>
                  <th>Players</th>
                  <th>Rounds</th>
                  <th>Champion</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((t) => (
                  <tr key={t.id}>
                    <td>{t.id}</td>
                    <td>{t.name}</td>
                    <td>
                      <span className={`badge ${t.status === "in_progress" ? "badge-live" : "badge-ok"}`}>{t.status}</span>
                    </td>
                    <td>{t.player_count}</td>
                    <td>{t.total_rounds}</td>
                    <td>{t.champion_player_id ?? "-"}</td>
                    <td>
                      <Link href={`/tournaments/${t.id}`}>Open</Link>
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
