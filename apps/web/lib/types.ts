export type PlayerRow = {
  id: number;
  name: string;
  image_uri: string | null;
  archived: boolean;
  total_matches: number;
  wins: number;
  losses: number;
  draws: number;
  tournaments_played: number;
  tournaments_won: number;
  source_created_at_ms: number;
  source_updated_at_ms: number;
};

export type TournamentRow = {
  id: number;
  name: string;
  status: string;
  champion_player_id: number | null;
  total_rounds: number;
  player_count: number;
  source_created_at_ms: number;
  source_updated_at_ms: number;
};

export type TournamentMatchRow = {
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
};

export type MatchRow = {
  id: number;
  player1_id: number;
  player2_id: number;
  player1_score: number;
  player2_score: number;
  started_at_ms: number;
  ended_at_ms: number | null;
  duration_seconds: number;
  winner_player_id: number | null;
  is_draw: boolean;
  player1_highest_break: number;
  player2_highest_break: number;
  match_highest_break: number;
  break_history_summary: string | null;
  match_type: string;
  tournament_id: number | null;
  tournament_round: number | null;
  source_created_at_ms: number;
  source_updated_at_ms: number;
};

export type PlayerRankingRow = {
  player_id: number;
  name: string;
  image_uri: string | null;
  archived: boolean;
  games_played: number;
  wins: number;
  draws: number;
  losses: number;
  max_break: number;
  average_points_per_match: number;
  tournaments_won: number;
};

export type LiveMatchRow = {
  id: string;
  is_active: boolean;
  player1_id: number | null;
  player2_id: number | null;
  player1_name: string | null;
  player2_name: string | null;
  player1_score: number;
  player2_score: number;
  active_player_number: number | null;
  current_break_player1: number;
  current_break_player2: number;
  highest_break_player1: number;
  highest_break_player2: number;
  highest_break_in_match: number;
  reds_remaining: number;
  yellow_visible: boolean;
  green_visible: boolean;
  brown_visible: boolean;
  blue_visible: boolean;
  pink_visible: boolean;
  black_visible: boolean;
  tournament_id: number | null;
  tournament_round: number | null;
  tournament_match_id: number | null;
  source_updated_at_ms: number;
};

export type ProfileRow = {
  id: string;
  username: string;
  role: "admin" | "viewer";
};
