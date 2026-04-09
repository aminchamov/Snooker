"use client";

import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { formatTableSizeLabel, normalizeTableSize } from "@/lib/liveAccessShared";
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

const SUPPORTED_PLAYER_COUNTS = [2, 4, 8, 16, 32] as const;

function normalizePlayerCount(value: number): (typeof SUPPORTED_PLAYER_COUNTS)[number] {
  return SUPPORTED_PLAYER_COUNTS.includes(value as (typeof SUPPORTED_PLAYER_COUNTS)[number])
    ? (value as (typeof SUPPORTED_PLAYER_COUNTS)[number])
    : 2;
}

function computeTotalRounds(playerCount: number): number {
  return Math.ceil(Math.log2(playerCount));
}

function resizeSlots(slots: Array<number | null>, playerCount: number): Array<number | null> {
  return Array.from({ length: playerCount }, (_, index) => slots[index] ?? null);
}

type MatchDateGroup = {
  date: string;
  totalGames: number;
  matches: MatchRow[];
};

type AdminPeriodPreset =
  | "last_7_days"
  | "last_30_days"
  | "last_90_days"
  | "this_month"
  | "specific_date"
  | "date_range"
  | "specific_month"
  | "year_to_date"
  | "full_year";

type AdminPeriodRange = {
  currentStart: number;
  currentEnd: number;
  compareStart: number;
  compareEnd: number;
  periodLabel: string;
  compareLabel: string;
};

type AdminDashboardMetrics = {
  totalRevenue: number;
  totalGames: number;
  totalPlayers: number;
  activePlayers: number;
  compareRevenue: number;
  compareGames: number;
  revenueGrowthPercent: number;
  gamesGrowthPercent: number;
  periodLabel: string;
  compareLabel: string;
  games10ft: number;
  games12ft: number;
};

const ADMIN_PERIOD_OPTIONS: Array<{ value: AdminPeriodPreset; label: string }> = [
  { value: "last_7_days", label: "Last 7 Days" },
  { value: "last_30_days", label: "Last 30 Days" },
  { value: "last_90_days", label: "Last 90 Days" },
  { value: "this_month", label: "This Month" },
  { value: "specific_date", label: "Specific Date" },
  { value: "date_range", label: "Date Interval" },
  { value: "specific_month", label: "Specific Month" },
  { value: "year_to_date", label: "Year To Date" },
  { value: "full_year", label: "Whole Year" }
];

const MONTH_OPTIONS = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December"
] as const;

function isTrackableMatch(match: MatchRow): boolean {
  return !(match.player1_score === 0 && match.player2_score === 0);
}

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

function startOfDayLocal(ms: number): number {
  const date = new Date(ms);
  date.setHours(0, 0, 0, 0);
  return date.getTime();
}

function endOfDayLocal(ms: number): number {
  const date = new Date(ms);
  date.setHours(23, 59, 59, 999);
  return date.getTime();
}

function formatDateInput(ms: number): string {
  return toDateKeyLocal(ms);
}

function parseDateInput(value: string): number | null {
  if (!value) return null;
  const parsed = new Date(`${value}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) return null;
  return parsed.getTime();
}

function previousWindow(currentStart: number, currentEnd: number): { start: number; end: number } {
  const windowSize = Math.max(1, currentEnd - currentStart + 1);
  return {
    start: currentStart - windowSize,
    end: currentStart - 1
  };
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0
  }).format(value);
}

function formatPercent(value: number): string {
  const rounded = Number.isFinite(value) ? value : 0;
  const sign = rounded > 0 ? "+" : "";
  return `${sign}${rounded.toFixed(1)}%`;
}

function computeGrowth(current: number, previous: number): number {
  if (previous <= 0) return current > 0 ? 100 : 0;
  return ((current - previous) / previous) * 100;
}

function computeAdminPeriodRange(
  period: AdminPeriodPreset,
  specificDate: string,
  rangeStart: string,
  rangeEnd: string,
  monthSelection: number,
  yearSelection: number,
  nowMs: number = Date.now()
): AdminPeriodRange {
  const now = new Date(nowMs);
  const todayStart = startOfDayLocal(nowMs);
  const todayEnd = endOfDayLocal(nowMs);

  if (period === "last_7_days") {
    const currentStart = startOfDayLocal(todayStart - 6 * 24 * 60 * 60 * 1000);
    const currentEnd = todayEnd;
    const previous = previousWindow(currentStart, currentEnd);
    return { currentStart, currentEnd, compareStart: previous.start, compareEnd: previous.end, periodLabel: "Last 7 Days", compareLabel: "Previous 7 Days" };
  }

  if (period === "last_30_days") {
    const currentStart = startOfDayLocal(todayStart - 29 * 24 * 60 * 60 * 1000);
    const currentEnd = todayEnd;
    const previous = previousWindow(currentStart, currentEnd);
    return { currentStart, currentEnd, compareStart: previous.start, compareEnd: previous.end, periodLabel: "Last 30 Days", compareLabel: "Previous 30 Days" };
  }

  if (period === "last_90_days") {
    const currentStart = startOfDayLocal(todayStart - 89 * 24 * 60 * 60 * 1000);
    const currentEnd = todayEnd;
    const previous = previousWindow(currentStart, currentEnd);
    return { currentStart, currentEnd, compareStart: previous.start, compareEnd: previous.end, periodLabel: "Last 90 Days", compareLabel: "Previous 90 Days" };
  }

  if (period === "this_month") {
    const currentStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
    const currentEnd = todayEnd;
    const prevMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1).getTime();
    const prevMonthEnd = endOfDayLocal(new Date(now.getFullYear(), now.getMonth(), 0).getTime());
    return { currentStart, currentEnd, compareStart: prevMonthStart, compareEnd: prevMonthEnd, periodLabel: "This Month", compareLabel: "Previous Month" };
  }

  if (period === "specific_date") {
    const day = parseDateInput(specificDate) ?? todayStart;
    const currentStart = startOfDayLocal(day);
    const currentEnd = endOfDayLocal(day);
    const compareDay = startOfDayLocal(day - 24 * 60 * 60 * 1000);
    return {
      currentStart,
      currentEnd,
      compareStart: compareDay,
      compareEnd: endOfDayLocal(compareDay),
      periodLabel: formatDateInput(day),
      compareLabel: formatDateInput(compareDay)
    };
  }

  if (period === "date_range") {
    const parsedStart = parseDateInput(rangeStart) ?? todayStart;
    const parsedEnd = parseDateInput(rangeEnd) ?? parsedStart;
    const currentStart = startOfDayLocal(Math.min(parsedStart, parsedEnd));
    const currentEnd = endOfDayLocal(Math.max(parsedStart, parsedEnd));
    const previous = previousWindow(currentStart, currentEnd);
    return {
      currentStart,
      currentEnd,
      compareStart: previous.start,
      compareEnd: previous.end,
      periodLabel: `${formatDateInput(currentStart)} to ${formatDateInput(currentEnd)}`,
      compareLabel: `${formatDateInput(previous.start)} to ${formatDateInput(previous.end)}`
    };
  }

  if (period === "specific_month") {
    const safeMonth = Math.min(12, Math.max(1, monthSelection));
    const currentStart = new Date(yearSelection, safeMonth - 1, 1).getTime();
    const currentEnd = endOfDayLocal(new Date(yearSelection, safeMonth, 0).getTime());
    const compareDate = new Date(yearSelection, safeMonth - 2, 1);
    const compareStart = compareDate.getTime();
    const compareEnd = endOfDayLocal(new Date(compareDate.getFullYear(), compareDate.getMonth() + 1, 0).getTime());
    return {
      currentStart,
      currentEnd,
      compareStart,
      compareEnd,
      periodLabel: `${MONTH_OPTIONS[safeMonth - 1]} ${yearSelection}`,
      compareLabel: `${MONTH_OPTIONS[compareDate.getMonth()]} ${compareDate.getFullYear()}`
    };
  }

  if (period === "year_to_date") {
    const currentStart = new Date(yearSelection, 0, 1).getTime();
    const currentEnd = yearSelection === now.getFullYear() ? todayEnd : endOfDayLocal(new Date(yearSelection, 11, 31).getTime());
    const compareStart = new Date(yearSelection - 1, 0, 1).getTime();
    const compareEnd = yearSelection === now.getFullYear()
      ? endOfDayLocal(new Date(yearSelection - 1, now.getMonth(), now.getDate()).getTime())
      : endOfDayLocal(new Date(yearSelection - 1, 11, 31).getTime());
    return {
      currentStart,
      currentEnd,
      compareStart,
      compareEnd,
      periodLabel: `Year To Date ${yearSelection}`,
      compareLabel: `Year To Date ${yearSelection - 1}`
    };
  }

  const currentStart = new Date(yearSelection, 0, 1).getTime();
  const currentEnd = endOfDayLocal(new Date(yearSelection, 11, 31).getTime());
  const compareStart = new Date(yearSelection - 1, 0, 1).getTime();
  const compareEnd = endOfDayLocal(new Date(yearSelection - 1, 11, 31).getTime());
  return {
    currentStart,
    currentEnd,
    compareStart,
    compareEnd,
    periodLabel: `Full Year ${yearSelection}`,
    compareLabel: `Full Year ${yearSelection - 1}`
  };
}

function computeAdminDashboardMetrics(params: {
  matches: MatchRow[];
  totalPlayers: number;
  tableSizeFilter: "all" | "10ft" | "12ft";
  period: AdminPeriodPreset;
  specificDate: string;
  rangeStart: string;
  rangeEnd: string;
  monthSelection: number;
  yearSelection: number;
  fee10: number;
  fee12: number;
}): AdminDashboardMetrics {
  const {
    matches,
    totalPlayers,
    tableSizeFilter,
    period,
    specificDate,
    rangeStart,
    rangeEnd,
    monthSelection,
    yearSelection,
    fee10,
    fee12
  } = params;

  const range = computeAdminPeriodRange(period, specificDate, rangeStart, rangeEnd, monthSelection, yearSelection);
  const filteredRows = matches.filter((match) => {
    const normalizedSize = normalizeTableSize(match.table_size);
    if (tableSizeFilter !== "all" && normalizedSize !== tableSizeFilter) return false;
    return true;
  });

  const currentRows = filteredRows.filter((match) => match.started_at_ms >= range.currentStart && match.started_at_ms <= range.currentEnd);
  const compareRows = filteredRows.filter((match) => match.started_at_ms >= range.compareStart && match.started_at_ms <= range.compareEnd);
  const getRevenue = (match: MatchRow) => normalizeTableSize(match.table_size) === "10ft" ? fee10 : fee12;
  const activePlayers = new Set(
    currentRows.flatMap((match) => [match.player1_id, match.player2_id]).filter((value): value is number => Number.isFinite(value))
  ).size;

  return {
    totalRevenue: currentRows.reduce((sum, match) => sum + getRevenue(match), 0),
    totalGames: currentRows.length,
    totalPlayers,
    activePlayers,
    compareRevenue: compareRows.reduce((sum, match) => sum + getRevenue(match), 0),
    compareGames: compareRows.length,
    revenueGrowthPercent: computeGrowth(
      currentRows.reduce((sum, match) => sum + getRevenue(match), 0),
      compareRows.reduce((sum, match) => sum + getRevenue(match), 0)
    ),
    gamesGrowthPercent: computeGrowth(currentRows.length, compareRows.length),
    periodLabel: range.periodLabel,
    compareLabel: range.compareLabel,
    games10ft: currentRows.filter((match) => normalizeTableSize(match.table_size) === "10ft").length,
    games12ft: currentRows.filter((match) => normalizeTableSize(match.table_size) === "12ft").length
  };
}

type TournamentMatchInsert = {
  id: number;
  tournament_id: number;
  round_number: number;
  bracket_position: number;
  player1_id: number | null;
  player2_id: number | null;
  winner_player_id: number | null;
  linked_match_id: number | null;
  state: string;
  source_created_at_ms: number;
  source_updated_at_ms: number;
  deleted_at: null;
};

function buildTournamentMatchesPayload(
  tournamentId: number,
  playerCount: number,
  firstRoundSlots: Array<number | null>,
  timestampMs: number
): TournamentMatchInsert[] {
  const rows: TournamentMatchInsert[] = [];
  const totalRounds = computeTotalRounds(playerCount);
  let idCursor = timestampMs * 1000;

  const firstRoundMatchCount = playerCount / 2;
  for (let i = 0; i < firstRoundMatchCount; i++) {
    const player1Id = firstRoundSlots[i * 2] ?? null;
    const player2Id = firstRoundSlots[i * 2 + 1] ?? null;
    rows.push({
      id: idCursor++,
      tournament_id: tournamentId,
      round_number: 1,
      bracket_position: i,
      player1_id: player1Id,
      player2_id: player2Id,
      winner_player_id: null,
      linked_match_id: null,
      state: player1Id !== null && player2Id !== null ? "ready" : "pending",
      source_created_at_ms: timestampMs,
      source_updated_at_ms: timestampMs,
      deleted_at: null
    });
  }

  for (let round = 2; round <= totalRounds; round++) {
    const matchCount = Math.floor(playerCount / 2 ** round);
    for (let position = 0; position < matchCount; position++) {
      rows.push({
        id: idCursor++,
        tournament_id: tournamentId,
        round_number: round,
        bracket_position: position,
        player1_id: null,
        player2_id: null,
        winner_player_id: null,
        linked_match_id: null,
        state: "pending",
        source_created_at_ms: timestampMs,
        source_updated_at_ms: timestampMs,
        deleted_at: null
      });
    }
  }

  return rows;
}

export default function AdminDashboardPage() {
  const router = useRouter();

  const [session, setSession] = useState<AdminSession | null>(null);
  const [players, setPlayers] = useState<PlayerRow[]>([]);
  const [tournaments, setTournaments] = useState<TournamentRow[]>([]);
  const [tournamentMatches, setTournamentMatches] = useState<TournamentMatchRow[]>([]);
  const [matches, setMatches] = useState<MatchRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [selectedMatchDate, setSelectedMatchDate] = useState<string>("");
  const [dashboardPeriod, setDashboardPeriod] = useState<AdminPeriodPreset>("last_30_days");
  const [dashboardSpecificDate, setDashboardSpecificDate] = useState<string>(() => formatDateInput(Date.now()));
  const [dashboardRangeStart, setDashboardRangeStart] = useState<string>(() => formatDateInput(Date.now() - 6 * 24 * 60 * 60 * 1000));
  const [dashboardRangeEnd, setDashboardRangeEnd] = useState<string>(() => formatDateInput(Date.now()));
  const [dashboardMonth, setDashboardMonth] = useState<number>(() => new Date().getMonth() + 1);
  const [dashboardYear, setDashboardYear] = useState<number>(() => new Date().getFullYear());
  const [dashboardTableSize, setDashboardTableSize] = useState<"all" | "10ft" | "12ft">("all");
  const [fee10Text, setFee10Text] = useState("100");
  const [fee12Text, setFee12Text] = useState("140");

  const [playerForm, setPlayerForm] = useState({ id: "", name: "", image_uri: "" });
  const [tournamentForm, setTournamentForm] = useState({ id: "", name: "", player_count: "2", total_rounds: "1", status: "in_progress" });
  const [firstRoundSlots, setFirstRoundSlots] = useState<Array<number | null>>(resizeSlots([], 2));

  const availableTournamentPlayers = useMemo(
    () => players.filter((player) => !player.archived),
    [players]
  );
  const selectedPlayerCount = useMemo(
    () => normalizePlayerCount(Number(tournamentForm.player_count)),
    [tournamentForm.player_count]
  );
  const filledSlots = useMemo(
    () => firstRoundSlots.filter((id): id is number => id !== null).length,
    [firstRoundSlots]
  );
  const playerNameById = useMemo(() => {
    return new Map<number, string>(players.map((player) => [player.id, player.name]));
  }, [players]);
  const recentMatches = useMemo(
    () => [...matches].sort((a, b) => b.started_at_ms - a.started_at_ms).slice(0, 12),
    [matches]
  );
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
  const fee10 = useMemo(() => {
    const parsed = Number(fee10Text);
    return Number.isFinite(parsed) && parsed >= 0 ? parsed : 100;
  }, [fee10Text]);
  const fee12 = useMemo(() => {
    const parsed = Number(fee12Text);
    return Number.isFinite(parsed) && parsed >= 0 ? parsed : 140;
  }, [fee12Text]);
  const dashboardMetrics = useMemo(
    () =>
      computeAdminDashboardMetrics({
        matches,
        totalPlayers: players.filter((player) => !player.archived).length,
        tableSizeFilter: dashboardTableSize,
        period: dashboardPeriod,
        specificDate: dashboardSpecificDate,
        rangeStart: dashboardRangeStart,
        rangeEnd: dashboardRangeEnd,
        monthSelection: dashboardMonth,
        yearSelection: dashboardYear,
        fee10,
        fee12
      }),
    [matches, players, dashboardTableSize, dashboardPeriod, dashboardSpecificDate, dashboardRangeStart, dashboardRangeEnd, dashboardMonth, dashboardYear, fee10, fee12]
  );

  async function ensureAdmin() {
    const auth = await browserSupabase.auth.getSession();
    const user = auth.data.session?.user;
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

  async function loadAdminData(showLoader = true) {
    if (showLoader) {
      setIsLoading(true);
    }
    setStatusError(null);

    const admin = await ensureAdmin();
    if (!admin) {
      if (showLoader) setIsLoading(false);
      return;
    }

    setSession(admin);

    const [playersRes, tournamentsRes, tournamentMatchesRes, matchesRes] = await Promise.all([
      browserSupabase.from("players").select("*").order("name", { ascending: true }),
      browserSupabase.from("tournaments").select("*").order("source_created_at_ms", { ascending: false }),
      browserSupabase
        .from("tournament_matches")
        .select("*")
        .order("tournament_id", { ascending: true })
        .order("round_number", { ascending: true })
        .order("bracket_position", { ascending: true }),
      browserSupabase.from("matches").select("*").order("started_at_ms", { ascending: false }).limit(1000)
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

    if (tournamentMatchesRes.error) {
      setStatusError(tournamentMatchesRes.error.message);
    } else {
      setTournamentMatches((tournamentMatchesRes.data ?? []) as TournamentMatchRow[]);
    }

    if (matchesRes.error) {
      setStatusError(matchesRes.error.message);
    } else {
      setMatches(((matchesRes.data ?? []) as MatchRow[]).filter(isTrackableMatch));
    }

    if (showLoader) {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadAdminData();
  }, []);

  useEffect(() => {
    if (!session?.userId) return;
    const intervalId = window.setInterval(() => {
      void loadAdminData(false);
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

  function updateTournamentPlayerCount(rawCount: number) {
    const count = normalizePlayerCount(rawCount);
    setTournamentForm((state) => ({
      ...state,
      player_count: String(count),
      total_rounds: String(computeTotalRounds(count))
    }));
    setFirstRoundSlots((previous) => resizeSlots(previous, count));
  }

  function assignPlayerToSlot(slotIndex: number, rawValue: string) {
    const parsed = Number(rawValue);
    const playerId = Number.isNaN(parsed) || parsed <= 0 ? null : parsed;

    setFirstRoundSlots((previous) => {
      const next = [...previous];
      if (slotIndex < 0 || slotIndex >= next.length) return previous;

      if (playerId !== null) {
        for (let i = 0; i < next.length; i++) {
          if (i !== slotIndex && next[i] === playerId) {
            next[i] = null;
          }
        }
      }

      next[slotIndex] = playerId;
      return next;
    });
  }

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
    setStatusMessage(null);

    const normalizedName = tournamentForm.name.trim();
    if (!normalizedName) {
      setStatusError("Tournament name is required.");
      return;
    }

    if (availableTournamentPlayers.length < selectedPlayerCount) {
      setStatusError(`Need at least ${selectedPlayerCount} active players to build this bracket.`);
      return;
    }

    if (filledSlots !== selectedPlayerCount) {
      setStatusError("Please fill all round-1 player slots.");
      return;
    }

    const selectedIds = firstRoundSlots.filter((id): id is number => id !== null);
    if (new Set(selectedIds).size !== selectedIds.length) {
      setStatusError("Each player can only be used once in round 1.");
      return;
    }

    const parsedId = Number(tournamentForm.id);
    const effectiveId = Number.isNaN(parsedId) || parsedId <= 0 ? nowMs() : parsedId;
    const timestamp = nowMs();
    const totalRounds = computeTotalRounds(selectedPlayerCount);

    const payload = {
      id: effectiveId,
      name: normalizedName,
      status: tournamentForm.status,
      champion_player_id: null,
      total_rounds: totalRounds,
      player_count: selectedPlayerCount,
      source_created_at_ms: timestamp,
      source_updated_at_ms: timestamp,
      deleted_at: null
    };

    const result = await browserSupabase.from("tournaments").upsert(payload, { onConflict: "id" });
    if (result.error) {
      setStatusError(result.error.message);
      return;
    }

    const existingRows = tournamentMatches.filter((row) => row.tournament_id === effectiveId);
    if (existingRows.length === 0) {
      const bracketRows = buildTournamentMatchesPayload(effectiveId, selectedPlayerCount, firstRoundSlots, timestamp);
      const matchesRes = await browserSupabase
        .from("tournament_matches")
        .upsert(bracketRows, { onConflict: "id" });

      if (matchesRes.error) {
        setStatusError(matchesRes.error.message);
        return;
      }
      setStatusMessage("Tournament and bracket created.");
    } else {
      setStatusMessage("Tournament saved. Existing bracket kept unchanged.");
    }

    setTournamentForm({ id: "", name: "", player_count: "2", total_rounds: "1", status: "in_progress" });
    setFirstRoundSlots(resizeSlots([], 2));
    await loadAdminData();
  }

  function editPlayer(row: PlayerRow) {
    setPlayerForm({ id: String(row.id), name: row.name, image_uri: row.image_uri ?? "" });
  }

  function editTournament(row: TournamentRow) {
    const count = normalizePlayerCount(row.player_count);
    const slots = resizeSlots([], count);
    const roundOneRows = tournamentMatches
      .filter((match) => match.tournament_id === row.id && match.round_number === 1)
      .sort((a, b) => a.bracket_position - b.bracket_position);

    for (const match of roundOneRows) {
      const slotBase = match.bracket_position * 2;
      if (slotBase < slots.length) slots[slotBase] = match.player1_id ?? null;
      if (slotBase + 1 < slots.length) slots[slotBase + 1] = match.player2_id ?? null;
    }

    setTournamentForm({
      id: String(row.id),
      name: row.name,
      player_count: String(count),
      total_rounds: String(row.total_rounds || computeTotalRounds(count)),
      status: row.status
    });
    setFirstRoundSlots(slots);
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
        <p className="page-subtitle">
          Logged in as {currentSession.username}. Public data writes are protected by RLS. Tablet/mobile sync data auto-refreshes here every 5 seconds.
        </p>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2 style={{ margin: 0 }}>Admin Dashboard</h2>
        </div>
        <div className="panel-body">
          <div className="admin-filter-grid">
            <div className="form-row" style={{ marginBottom: 0 }}>
              <label>Period</label>
              <select className="input" value={dashboardPeriod} onChange={(e) => setDashboardPeriod(e.target.value as AdminPeriodPreset)}>
                {ADMIN_PERIOD_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </div>
            <div className="form-row" style={{ marginBottom: 0 }}>
              <label>Table Size</label>
              <select className="input" value={dashboardTableSize} onChange={(e) => setDashboardTableSize(e.target.value as "all" | "10ft" | "12ft")}>
                <option value="all">All</option>
                <option value="10ft">10 FT</option>
                <option value="12ft">12 FT</option>
              </select>
            </div>
            <div className="form-row" style={{ marginBottom: 0 }}>
              <label>10ft Fee / Game</label>
              <input className="input" inputMode="numeric" value={fee10Text} onChange={(e) => setFee10Text(e.target.value)} />
            </div>
            <div className="form-row" style={{ marginBottom: 0 }}>
              <label>12ft Fee / Game</label>
              <input className="input" inputMode="numeric" value={fee12Text} onChange={(e) => setFee12Text(e.target.value)} />
            </div>

            {dashboardPeriod === "specific_date" ? (
              <div className="form-row" style={{ marginBottom: 0 }}>
                <label>Date</label>
                <input className="input" type="date" value={dashboardSpecificDate} onChange={(e) => setDashboardSpecificDate(e.target.value)} />
              </div>
            ) : null}

            {dashboardPeriod === "date_range" ? (
              <>
                <div className="form-row" style={{ marginBottom: 0 }}>
                  <label>Start</label>
                  <input className="input" type="date" value={dashboardRangeStart} onChange={(e) => setDashboardRangeStart(e.target.value)} />
                </div>
                <div className="form-row" style={{ marginBottom: 0 }}>
                  <label>End</label>
                  <input className="input" type="date" value={dashboardRangeEnd} onChange={(e) => setDashboardRangeEnd(e.target.value)} />
                </div>
              </>
            ) : null}

            {dashboardPeriod === "specific_month" ? (
              <>
                <div className="form-row" style={{ marginBottom: 0 }}>
                  <label>Month</label>
                  <select className="input" value={dashboardMonth} onChange={(e) => setDashboardMonth(Number(e.target.value))}>
                    {MONTH_OPTIONS.map((month, index) => (
                      <option key={month} value={index + 1}>{month}</option>
                    ))}
                  </select>
                </div>
                <div className="form-row" style={{ marginBottom: 0 }}>
                  <label>Year</label>
                  <input className="input" inputMode="numeric" value={dashboardYear} onChange={(e) => setDashboardYear(Number(e.target.value) || new Date().getFullYear())} />
                </div>
              </>
            ) : null}

            {dashboardPeriod === "year_to_date" || dashboardPeriod === "full_year" ? (
              <div className="form-row" style={{ marginBottom: 0 }}>
                <label>Year</label>
                <input className="input" inputMode="numeric" value={dashboardYear} onChange={(e) => setDashboardYear(Number(e.target.value) || new Date().getFullYear())} />
              </div>
            ) : null}
          </div>

          <div className="admin-dashboard-grid">
            <article className="admin-metric-card">
              <span className="label">Total Revenue</span>
              <strong className="admin-metric-value">{formatMoney(dashboardMetrics.totalRevenue)}</strong>
              <span className="sub">{dashboardMetrics.periodLabel}</span>
            </article>
            <article className="admin-metric-card">
              <span className="label">Total Games</span>
              <strong className="admin-metric-value">{dashboardMetrics.totalGames}</strong>
              <span className="sub">10ft: {dashboardMetrics.games10ft} · 12ft: {dashboardMetrics.games12ft}</span>
            </article>
            <article className="admin-metric-card">
              <span className="label">Total Players</span>
              <strong className="admin-metric-value">{dashboardMetrics.totalPlayers}</strong>
              <span className="sub">{dashboardMetrics.activePlayers} active in selected period</span>
            </article>
            <article className="admin-metric-card">
              <span className="label">Growth Rate</span>
              <strong className="admin-metric-value">{formatPercent(dashboardMetrics.revenueGrowthPercent)}</strong>
              <span className="sub">Revenue vs {dashboardMetrics.compareLabel}</span>
            </article>
          </div>

          <div className="admin-compare-grid">
            <article className="admin-compare-card">
              <h3>Revenue Comparison</h3>
              <p>{dashboardMetrics.periodLabel}: <strong>{formatMoney(dashboardMetrics.totalRevenue)}</strong></p>
              <p>{dashboardMetrics.compareLabel}: <strong>{formatMoney(dashboardMetrics.compareRevenue)}</strong></p>
              <p className="admin-growth">Change: {formatPercent(dashboardMetrics.revenueGrowthPercent)}</p>
            </article>
            <article className="admin-compare-card">
              <h3>Games Comparison</h3>
              <p>{dashboardMetrics.periodLabel}: <strong>{dashboardMetrics.totalGames}</strong></p>
              <p>{dashboardMetrics.compareLabel}: <strong>{dashboardMetrics.compareGames}</strong></p>
              <p className="admin-growth">Change: {formatPercent(dashboardMetrics.gamesGrowthPercent)}</p>
            </article>
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel-body" style={{ paddingTop: "1rem", display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
          <Link className="button secondary" href="/admin/recent-games">Recent Games Page</Link>
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
                  <select
                    className="input"
                    value={String(selectedPlayerCount)}
                    onChange={(e) => updateTournamentPlayerCount(Number(e.target.value))}
                  >
                    {SUPPORTED_PLAYER_COUNTS.map((count) => (
                      <option key={count} value={count}>
                        {count}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-row">
                  <label>Total Rounds</label>
                  <input className="input" value={String(computeTotalRounds(selectedPlayerCount))} readOnly />
                </div>
                <div className="form-row">
                  <label>Status</label>
                  <select className="input" value={tournamentForm.status} onChange={(e) => setTournamentForm((s) => ({ ...s, status: e.target.value }))}>
                    <option value="created">created</option>
                  <option value="in_progress">in_progress</option>
                    <option value="completed">completed</option>
                  </select>
                </div>
                <div className="form-row">
                  <label>Round 1 Matchups</label>
                  {availableTournamentPlayers.length < selectedPlayerCount ? (
                    <div className="empty" style={{ padding: "0.6rem 0.75rem" }}>
                      Need {selectedPlayerCount} active players, but only {availableTournamentPlayers.length} available.
                    </div>
                  ) : null}
                  <div className="grid" style={{ gap: "0.6rem" }}>
                    {Array.from({ length: selectedPlayerCount / 2 }).map((_, matchIndex) => {
                      const slot1 = matchIndex * 2;
                      const slot2 = slot1 + 1;

                      const slot1UsedByOthers = new Set(
                        firstRoundSlots.filter((id, index): id is number => index !== slot1 && id !== null)
                      );
                      const slot2UsedByOthers = new Set(
                        firstRoundSlots.filter((id, index): id is number => index !== slot2 && id !== null)
                      );

                      return (
                        <div key={`pair-${matchIndex}`} className="panel" style={{ padding: "0.7rem" }}>
                          <p style={{ margin: "0 0 0.55rem", color: "var(--gold-soft)", fontWeight: 700 }}>
                            Match {matchIndex + 1}
                          </p>
                          <div className="grid grid-2">
                            <div className="form-row" style={{ marginBottom: 0 }}>
                              <label>Player A</label>
                              <select
                                className="input"
                                value={firstRoundSlots[slot1] ?? ""}
                                onChange={(e) => assignPlayerToSlot(slot1, e.target.value)}
                              >
                                <option value="">Select player</option>
                                {availableTournamentPlayers.map((player) => (
                                  <option
                                    key={`slot1-${matchIndex}-${player.id}`}
                                    value={player.id}
                                    disabled={slot1UsedByOthers.has(player.id)}
                                  >
                                    {player.name}
                                    {slot1UsedByOthers.has(player.id) ? " (In use)" : ""}
                                  </option>
                                ))}
                              </select>
                            </div>
                            <div className="form-row" style={{ marginBottom: 0 }}>
                              <label>Player B</label>
                              <select
                                className="input"
                                value={firstRoundSlots[slot2] ?? ""}
                                onChange={(e) => assignPlayerToSlot(slot2, e.target.value)}
                              >
                                <option value="">Select player</option>
                                {availableTournamentPlayers.map((player) => (
                                  <option
                                    key={`slot2-${matchIndex}-${player.id}`}
                                    value={player.id}
                                    disabled={slot2UsedByOthers.has(player.id)}
                                  >
                                    {player.name}
                                    {slot2UsedByOthers.has(player.id) ? " (In use)" : ""}
                                  </option>
                                ))}
                              </select>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                  <small style={{ color: "var(--ink-soft)" }}>
                    Filled slots: {filledSlots}/{selectedPlayerCount}. Players are unique across all round-1 slots.
                  </small>
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
                  <th>Table</th>
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
                      <td>{formatTableSizeLabel(match.table_size)}</td>
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
                        <th>Table</th>
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
                            <td>{formatTableSizeLabel(match.table_size)}</td>
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
