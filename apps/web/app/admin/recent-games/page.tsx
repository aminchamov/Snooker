"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { browserSupabase } from "@/lib/supabase/browserClient";
import type { MatchRow, PlayerRow, ProfileRow } from "@/lib/types";

type AdminSession = {
  userId: string;
  username: string;
};

type MatchDateGroup = {
  date: string;
  totalGames: number;
  matches: MatchRow[];
};

function toDateKeyLocal(ms: number): string {
  if (!Number.isFinite(ms) || ms <= 0) return "Unknown";
  const date = new Date(ms);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatClock(ms: number): string {
  if (!Number.isFinite(ms) || ms <= 0) return "-";
  return new Date(ms).toLocaleTimeString();
}

function matchEndMs(match: MatchRow): number {
  if (match.ended_at_ms && match.ended_at_ms > 0) return match.ended_at_ms;
  return match.started_at_ms + Math.max(0, match.duration_seconds) * 1000;
}

function formatDuration(durationSeconds: number): string {
  const total = Math.max(0, durationSeconds);
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export default function AdminRecentGamesPage() {
  const router = useRouter();
  const [session, setSession] = useState<AdminSession | null>(null);
  const [players, setPlayers] = useState<PlayerRow[]>([]);
  const [matches, setMatches] = useState<MatchRow[]>([]);
  const [selectedMatchDate, setSelectedMatchDate] = useState<string>("");
  const [isLoading, setIsLoading] = useState(true);
  const [statusError, setStatusError] = useState<string | null>(null);

  const playerNameById = useMemo(() => new Map<number, string>(players.map((player) => [player.id, player.name])), [players]);
  const recentMatches = useMemo(() => [...matches].sort((a, b) => b.started_at_ms - a.started_at_ms).slice(0, 25), [matches]);
  const matchesByDate = useMemo(() => {
    const grouped = new Map<string, MatchRow[]>();
    for (const match of matches) {
      const key = toDateKeyLocal(match.started_at_ms);
      const existing = grouped.get(key);
      if (existing) {
        existing.push(match);
      } else {
        grouped.set(key, [match]);
      }
    }

    return Array.from(grouped.entries())
      .map(([date, groupedMatches]) => ({
        date,
        totalGames: groupedMatches.length,
        matches: [...groupedMatches].sort((a, b) => b.started_at_ms - a.started_at_ms)
      } as MatchDateGroup))
      .sort((a, b) => b.date.localeCompare(a.date));
  }, [matches]);

  const selectedDateGroup = useMemo(() => {
    if (!selectedMatchDate) return matchesByDate[0] ?? null;
    return matchesByDate.find((group) => group.date === selectedMatchDate) ?? null;
  }, [matchesByDate, selectedMatchDate]);

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

  async function loadAdminRecentGames(showLoader = true) {
    if (showLoader) setIsLoading(true);
    setStatusError(null);

    const admin = await ensureAdmin();
    if (!admin) {
      if (showLoader) setIsLoading(false);
      return;
    }

    setSession(admin);

    const [playersRes, matchesRes] = await Promise.all([
      browserSupabase.from("players").select("*").order("name", { ascending: true }),
      browserSupabase.from("matches").select("*").order("started_at_ms", { ascending: false }).limit(2000)
    ]);

    if (playersRes.error) {
      setStatusError(playersRes.error.message);
    } else {
      setPlayers((playersRes.data ?? []) as PlayerRow[]);
    }

    if (matchesRes.error) {
      setStatusError(matchesRes.error.message);
    } else {
      setMatches((matchesRes.data ?? []) as MatchRow[]);
    }

    if (showLoader) setIsLoading(false);
  }

  useEffect(() => {
    void loadAdminRecentGames();
  }, []);

  useEffect(() => {
    if (!session?.userId) return;
    const intervalId = window.setInterval(() => {
      void loadAdminRecentGames(false);
    }, 5000);
    return () => window.clearInterval(intervalId);
  }, [session?.userId]);

  useEffect(() => {
    if (matchesByDate.length === 0) {
      setSelectedMatchDate("");
      return;
    }
    setSelectedMatchDate((current) => {
      if (current && matchesByDate.some((group) => group.date === current)) return current;
      return matchesByDate[0].date;
    });
  }, [matchesByDate]);

  if (isLoading || !session) {
    return <div className="empty">Loading admin recent games...</div>;
  }

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Admin Recent Games</h1>
        <p className="page-subtitle">
          Admin-only page. Signed in as {session.username}. Auto-refresh every 5 seconds.
        </p>
      </section>

      <section className="panel">
        <div className="panel-body" style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
          <Link className="button secondary" href="/admin">
            Back to Admin
          </Link>
          <button className="button" type="button" onClick={() => void loadAdminRecentGames(false)}>
            Refresh Now
          </button>
        </div>
        {statusError ? <p style={{ margin: "0 1rem 1rem", color: "#c92a2a" }}>{statusError}</p> : null}
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Recent Games</h2>
        </div>
        <div className="panel-body table-wrap">
          {recentMatches.length === 0 ? (
            <div className="empty">No matches synced yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Player 1</th>
                  <th>Score</th>
                  <th>Player 2</th>
                  <th>Winner</th>
                  <th>Duration</th>
                  <th>Type</th>
                </tr>
              </thead>
              <tbody>
                {recentMatches.map((match) => {
                  const winnerName = match.is_draw
                    ? "Draw"
                    : match.winner_player_id
                      ? (playerNameById.get(match.winner_player_id) ?? `#${match.winner_player_id}`)
                      : "-";
                  return (
                    <tr key={match.id}>
                      <td>{toDateKeyLocal(match.started_at_ms)}</td>
                      <td>{formatClock(match.started_at_ms)}</td>
                      <td>{formatClock(matchEndMs(match))}</td>
                      <td>{playerNameById.get(match.player1_id) ?? `#${match.player1_id}`}</td>
                      <td>{match.player1_score} - {match.player2_score}</td>
                      <td>{playerNameById.get(match.player2_id) ?? `#${match.player2_id}`}</td>
                      <td>{winnerName}</td>
                      <td>{formatDuration(match.duration_seconds)}</td>
                      <td>{match.match_type}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Total Games By Date (Admin)</h2>
        </div>
        <div className="panel-body" style={{ display: "grid", gap: "0.75rem" }}>
          {matchesByDate.length === 0 ? (
            <div className="empty">No daily totals available yet.</div>
          ) : (
            <>
              <div className="form-row" style={{ marginBottom: 0, maxWidth: "320px" }}>
                <label>Select Date</label>
                <select className="input" value={selectedDateGroup?.date ?? ""} onChange={(e) => setSelectedMatchDate(e.target.value)}>
                  {matchesByDate.map((group) => (
                    <option key={group.date} value={group.date}>
                      {group.date} ({group.totalGames} games)
                    </option>
                  ))}
                </select>
              </div>

              {selectedDateGroup ? (
                <div className="table-wrap">
                  <p style={{ marginTop: 0 }}>
                    <strong>Date:</strong> {selectedDateGroup.date} | <strong>Total Games:</strong> {selectedDateGroup.totalGames}
                  </p>
                  <table>
                    <thead>
                      <tr>
                        <th>Start</th>
                        <th>End</th>
                        <th>Player 1</th>
                        <th>Score</th>
                        <th>Player 2</th>
                        <th>Duration</th>
                        <th>Winner</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedDateGroup.matches.map((match) => {
                        const winnerName = match.is_draw
                          ? "Draw"
                          : match.winner_player_id
                            ? (playerNameById.get(match.winner_player_id) ?? `#${match.winner_player_id}`)
                            : "-";
                        return (
                          <tr key={`daily-${selectedDateGroup.date}-${match.id}`}>
                            <td>{formatClock(match.started_at_ms)}</td>
                            <td>{formatClock(matchEndMs(match))}</td>
                            <td>{playerNameById.get(match.player1_id) ?? `#${match.player1_id}`}</td>
                            <td>{match.player1_score} - {match.player2_score}</td>
                            <td>{playerNameById.get(match.player2_id) ?? `#${match.player2_id}`}</td>
                            <td>{formatDuration(match.duration_seconds)}</td>
                            <td>{winnerName}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </>
          )}
        </div>
      </section>
    </div>
  );
}
