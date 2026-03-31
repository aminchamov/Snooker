"use client";

import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { browserSupabase } from "@/lib/supabase/browserClient";
import type { MatchRow, PlayerRow, ProfileRow, TournamentMatchRow, TournamentRow } from "@/lib/types";

type AdminSession = {
  userId: string;
  username: string;
};

type ImportPayload = {
  players?: PlayerRow[];
  tournaments?: TournamentRow[];
  tournament_matches?: TournamentMatchRow[];
  matches?: MatchRow[];
};

function nowMs(): number {
  return Date.now();
}

export default function AdminDashboardPage() {
  const router = useRouter();

  const [session, setSession] = useState<AdminSession | null>(null);
  const [players, setPlayers] = useState<PlayerRow[]>([]);
  const [tournaments, setTournaments] = useState<TournamentRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);

  const [playerForm, setPlayerForm] = useState({ id: "", name: "", image_uri: "" });
  const [tournamentForm, setTournamentForm] = useState({ id: "", name: "", player_count: "2", total_rounds: "1", status: "created" });

  async function ensureAdmin() {
    const auth = await browserSupabase.auth.getUser();
    const user = auth.data.user;
    if (!user) {
      router.push("/admin/login");
      return null;
    }

    const profileRes = await browserSupabase
      .from("profiles")
      .select("id,username,role")
      .eq("id", user.id)
      .single();

    if (profileRes.error || !profileRes.data) {
      await browserSupabase.auth.signOut();
      router.push("/admin/login");
      return null;
    }

    const profile = profileRes.data as ProfileRow;
    if (profile.role !== "admin") {
      await browserSupabase.auth.signOut();
      router.push("/admin/login");
      return null;
    }

    return {
      userId: profile.id,
      username: profile.username
    } as AdminSession;
  }

  async function loadAdminData() {
    setIsLoading(true);
    setStatusError(null);

    const admin = await ensureAdmin();
    if (!admin) return;

    setSession(admin);

    const [playersRes, tournamentsRes] = await Promise.all([
      browserSupabase.from("players").select("*").order("name", { ascending: true }),
      browserSupabase.from("tournaments").select("*").order("source_created_at_ms", { ascending: false })
    ]);

    if (playersRes.error) {
      setStatusError(playersRes.error.message);
    } else {
      setPlayers((playersRes.data ?? []) as PlayerRow[]);
    }

    if (tournamentsRes.error) {
      setStatusError(tournamentsRes.error.message);
    } else {
      setTournaments((tournamentsRes.data ?? []) as TournamentRow[]);
    }

    setIsLoading(false);
  }

  useEffect(() => {
    void loadAdminData();
  }, []);

  async function savePlayer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatusError(null);

    const parsedId = Number(playerForm.id);
    const effectiveId = Number.isNaN(parsedId) || parsedId <= 0 ? nowMs() : parsedId;

    const payload = {
      id: effectiveId,
      name: playerForm.name.trim(),
      image_uri: playerForm.image_uri.trim() || null,
      archived: false,
      total_matches: 0,
      wins: 0,
      losses: 0,
      draws: 0,
      tournaments_played: 0,
      tournaments_won: 0,
      source_created_at_ms: nowMs(),
      source_updated_at_ms: nowMs(),
      deleted_at: null
    };

    const result = await browserSupabase.from("players").upsert(payload, { onConflict: "id" });
    if (result.error) {
      setStatusError(result.error.message);
      return;
    }

    setStatusMessage("Player saved.");
    setPlayerForm({ id: "", name: "", image_uri: "" });
    await loadAdminData();
  }

  async function saveTournament(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatusError(null);

    const parsedId = Number(tournamentForm.id);
    const effectiveId = Number.isNaN(parsedId) || parsedId <= 0 ? nowMs() : parsedId;

    const payload = {
      id: effectiveId,
      name: tournamentForm.name.trim(),
      status: tournamentForm.status,
      champion_player_id: null,
      total_rounds: Number(tournamentForm.total_rounds) || 1,
      player_count: Number(tournamentForm.player_count) || 2,
      source_created_at_ms: nowMs(),
      source_updated_at_ms: nowMs(),
      deleted_at: null
    };

    const result = await browserSupabase.from("tournaments").upsert(payload, { onConflict: "id" });
    if (result.error) {
      setStatusError(result.error.message);
      return;
    }

    setStatusMessage("Tournament saved.");
    setTournamentForm({ id: "", name: "", player_count: "2", total_rounds: "1", status: "created" });
    await loadAdminData();
  }

  function editPlayer(row: PlayerRow) {
    setPlayerForm({ id: String(row.id), name: row.name, image_uri: row.image_uri ?? "" });
  }

  function editTournament(row: TournamentRow) {
    setTournamentForm({
      id: String(row.id),
      name: row.name,
      player_count: String(row.player_count),
      total_rounds: String(row.total_rounds),
      status: row.status
    });
  }

  async function exportAll() {
    setStatusError(null);
    const [playersRes, tournamentsRes, tMatchesRes, matchesRes, liveRes] = await Promise.all([
      browserSupabase.from("players").select("*"),
      browserSupabase.from("tournaments").select("*"),
      browserSupabase.from("tournament_matches").select("*"),
      browserSupabase.from("matches").select("*"),
      browserSupabase.from("live_match_state").select("*")
    ]);

    if (playersRes.error || tournamentsRes.error || tMatchesRes.error || matchesRes.error || liveRes.error) {
      setStatusError(playersRes.error?.message ?? tournamentsRes.error?.message ?? tMatchesRes.error?.message ?? matchesRes.error?.message ?? liveRes.error?.message ?? "Export failed");
      return;
    }

    const payload = {
      exported_at: nowMs(),
      schema_version: 1,
      players: playersRes.data ?? [],
      tournaments: tournamentsRes.data ?? [],
      tournament_matches: tMatchesRes.data ?? [],
      matches: matchesRes.data ?? [],
      live_match_state: liveRes.data ?? []
    };

    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `elocho_web_export_${Date.now()}.json`;
    link.click();
    URL.revokeObjectURL(url);
    setStatusMessage("Export complete.");
  }

  async function importAll(event: ChangeEvent<HTMLInputElement>) {
    setStatusError(null);
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      const text = await file.text();
      const parsed = JSON.parse(text) as ImportPayload;

      const playersPayload = Array.isArray(parsed.players) ? parsed.players : [];
      const tournamentsPayload = Array.isArray(parsed.tournaments) ? parsed.tournaments : [];
      const tournamentMatchesPayload = Array.isArray(parsed.tournament_matches) ? parsed.tournament_matches : [];
      const matchesPayload = Array.isArray(parsed.matches) ? parsed.matches : [];

      const [pRes, tRes, tmRes, mRes] = await Promise.all([
        playersPayload.length > 0 ? browserSupabase.from("players").upsert(playersPayload, { onConflict: "id" }) : Promise.resolve({ error: null }),
        tournamentsPayload.length > 0 ? browserSupabase.from("tournaments").upsert(tournamentsPayload, { onConflict: "id" }) : Promise.resolve({ error: null }),
        tournamentMatchesPayload.length > 0 ? browserSupabase.from("tournament_matches").upsert(tournamentMatchesPayload, { onConflict: "id" }) : Promise.resolve({ error: null }),
        matchesPayload.length > 0 ? browserSupabase.from("matches").upsert(matchesPayload, { onConflict: "id" }) : Promise.resolve({ error: null })
      ]);

      const error = pRes.error ?? tRes.error ?? tmRes.error ?? mRes.error;
      if (error) {
        setStatusError(error.message);
        return;
      }

      setStatusMessage("Import complete.");
      await loadAdminData();
    } catch {
      setStatusError("Invalid import file.");
    } finally {
      event.target.value = "";
    }
  }

  async function logout() {
    await browserSupabase.auth.signOut();
    router.push("/admin/login");
  }

  const canRender = useMemo(() => !isLoading && !!session, [isLoading, session]);

  if (!canRender) {
    return <div className="empty">Loading admin session...</div>;
  }
  const currentSession = session as AdminSession;

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Admin Mode</h1>
        <p className="page-subtitle">Logged in as {currentSession.username}. Public data writes are protected by RLS.</p>
      </section>

      <section className="panel">
        <div className="panel-body" style={{ paddingTop: "1rem", display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
          <button className="button" onClick={exportAll}>Export All JSON</button>
          <label className="button secondary" style={{ cursor: "pointer" }}>
            Import JSON
            <input type="file" accept="application/json" onChange={importAll} style={{ display: "none" }} />
          </label>
          <button className="button danger" onClick={logout}>Logout</button>
        </div>
        {statusMessage ? <p style={{ margin: "0 1rem 1rem", color: "#2b8a3e" }}>{statusMessage}</p> : null}
        {statusError ? <p style={{ margin: "0 1rem 1rem", color: "#c92a2a" }}>{statusError}</p> : null}
      </section>

      <section className="grid grid-2">
        <article className="panel">
          <div className="panel-header">
            <h2 style={{ margin: 0 }}>Add / Edit Player</h2>
          </div>
          <div className="panel-body">
            <form onSubmit={savePlayer}>
              <div className="form-row">
                <label>ID (optional)</label>
                <input className="input" value={playerForm.id} onChange={(e) => setPlayerForm((s) => ({ ...s, id: e.target.value }))} />
              </div>
              <div className="form-row">
                <label>Name</label>
                <input className="input" value={playerForm.name} onChange={(e) => setPlayerForm((s) => ({ ...s, name: e.target.value }))} required />
              </div>
              <div className="form-row">
                <label>Image URI</label>
                <input className="input" value={playerForm.image_uri} onChange={(e) => setPlayerForm((s) => ({ ...s, image_uri: e.target.value }))} />
              </div>
              <button className="button" type="submit">Save Player</button>
            </form>
          </div>
        </article>

        <article className="panel">
          <div className="panel-header">
            <h2 style={{ margin: 0 }}>Add / Edit Tournament</h2>
          </div>
          <div className="panel-body">
            <form onSubmit={saveTournament}>
              <div className="form-row">
                <label>ID (optional)</label>
                <input className="input" value={tournamentForm.id} onChange={(e) => setTournamentForm((s) => ({ ...s, id: e.target.value }))} />
              </div>
              <div className="form-row">
                <label>Name</label>
                <input className="input" value={tournamentForm.name} onChange={(e) => setTournamentForm((s) => ({ ...s, name: e.target.value }))} required />
              </div>
              <div className="form-row">
                <label>Player Count</label>
                <input className="input" value={tournamentForm.player_count} onChange={(e) => setTournamentForm((s) => ({ ...s, player_count: e.target.value }))} />
              </div>
              <div className="form-row">
                <label>Total Rounds</label>
                <input className="input" value={tournamentForm.total_rounds} onChange={(e) => setTournamentForm((s) => ({ ...s, total_rounds: e.target.value }))} />
              </div>
              <div className="form-row">
                <label>Status</label>
                <select className="input" value={tournamentForm.status} onChange={(e) => setTournamentForm((s) => ({ ...s, status: e.target.value }))}>
                  <option value="created">created</option>
                  <option value="in_progress">in_progress</option>
                  <option value="completed">completed</option>
                </select>
              </div>
              <button className="button" type="submit">Save Tournament</button>
            </form>
          </div>
        </article>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Players</h2>
        </div>
        <div className="panel-body table-wrap">
          {players.length === 0 ? (
            <div className="empty">No players yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Wins</th>
                  <th>Draws</th>
                  <th>Losses</th>
                  <th>Edit</th>
                </tr>
              </thead>
              <tbody>
                {players.map((player) => (
                  <tr key={player.id}>
                    <td>{player.id}</td>
                    <td>{player.name}</td>
                    <td>{player.wins}</td>
                    <td>{player.draws}</td>
                    <td>{player.losses}</td>
                    <td>
                      <button className="button secondary" type="button" onClick={() => editPlayer(player)}>
                        Load
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Tournaments</h2>
        </div>
        <div className="panel-body table-wrap">
          {tournaments.length === 0 ? (
            <div className="empty">No tournaments yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Status</th>
                  <th>Players</th>
                  <th>Rounds</th>
                  <th>Edit</th>
                </tr>
              </thead>
              <tbody>
                {tournaments.map((tournament) => (
                  <tr key={tournament.id}>
                    <td>{tournament.id}</td>
                    <td>{tournament.name}</td>
                    <td>{tournament.status}</td>
                    <td>{tournament.player_count}</td>
                    <td>{tournament.total_rounds}</td>
                    <td>
                      <button className="button secondary" type="button" onClick={() => editTournament(tournament)}>
                        Load
                      </button>
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
