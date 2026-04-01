alter table if exists public.live_match_state
  add column if not exists elapsed_seconds bigint not null default 0,
  add column if not exists queue_count integer not null default 0,
  add column if not exists points_left_to_147 integer not null default 147;
