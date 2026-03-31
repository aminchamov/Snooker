create extension if not exists pgcrypto;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null unique,
  role text not null default 'viewer' check (role in ('admin', 'viewer')),
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.players (
  id bigint primary key,
  name text not null,
  image_uri text,
  archived boolean not null default false,
  total_matches integer not null default 0,
  wins integer not null default 0,
  losses integer not null default 0,
  draws integer not null default 0,
  tournaments_played integer not null default 0,
  tournaments_won integer not null default 0,
  source_created_at_ms bigint not null,
  source_updated_at_ms bigint not null,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  deleted_at timestamptz
);

create table if not exists public.tournaments (
  id bigint primary key,
  name text not null,
  status text not null default 'created',
  champion_player_id bigint,
  total_rounds integer not null default 0,
  player_count integer not null default 0,
  source_created_at_ms bigint not null,
  source_updated_at_ms bigint not null,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  deleted_at timestamptz
);

create table if not exists public.tournament_matches (
  id bigint primary key,
  tournament_id bigint not null,
  round_number integer not null,
  bracket_position integer not null,
  player1_id bigint,
  player2_id bigint,
  winner_player_id bigint,
  linked_match_id bigint,
  state text not null default 'pending',
  source_created_at_ms bigint not null,
  source_updated_at_ms bigint not null,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  deleted_at timestamptz
);

create table if not exists public.matches (
  id bigint primary key,
  player1_id bigint not null,
  player2_id bigint not null,
  player1_score integer not null default 0,
  player2_score integer not null default 0,
  started_at_ms bigint not null,
  ended_at_ms bigint,
  duration_seconds bigint not null default 0,
  winner_player_id bigint,
  is_draw boolean not null default false,
  player1_highest_break integer not null default 0,
  player2_highest_break integer not null default 0,
  match_highest_break integer not null default 0,
  break_history_summary text,
  match_type text not null default 'quick_match',
  tournament_id bigint,
  tournament_round integer,
  source_created_at_ms bigint not null,
  source_updated_at_ms bigint not null,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  deleted_at timestamptz
);

create table if not exists public.live_match_state (
  id text primary key default 'active',
  is_active boolean not null default false,
  player1_id bigint,
  player2_id bigint,
  player1_name text,
  player2_name text,
  player1_score integer not null default 0,
  player2_score integer not null default 0,
  active_player_number integer,
  current_break_player1 integer not null default 0,
  current_break_player2 integer not null default 0,
  highest_break_player1 integer not null default 0,
  highest_break_player2 integer not null default 0,
  highest_break_in_match integer not null default 0,
  reds_remaining integer not null default 15,
  yellow_visible boolean not null default true,
  green_visible boolean not null default true,
  brown_visible boolean not null default true,
  blue_visible boolean not null default true,
  pink_visible boolean not null default true,
  black_visible boolean not null default true,
  tournament_id bigint,
  tournament_round integer,
  tournament_match_id bigint,
  source_updated_at_ms bigint not null,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  deleted_at timestamptz
);

create table if not exists public.sync_metadata (
  client_id text primary key,
  device_label text,
  last_synced_at timestamptz not null default timezone('utc', now()),
  payload_summary jsonb,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create or replace function public.is_admin(user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.profiles p
    where p.id = user_id
      and p.role = 'admin'
  );
$$;

revoke all on function public.is_admin(uuid) from public;
grant execute on function public.is_admin(uuid) to authenticated;

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists trg_players_updated_at on public.players;
create trigger trg_players_updated_at
before update on public.players
for each row execute function public.set_updated_at();

drop trigger if exists trg_tournaments_updated_at on public.tournaments;
create trigger trg_tournaments_updated_at
before update on public.tournaments
for each row execute function public.set_updated_at();

drop trigger if exists trg_tournament_matches_updated_at on public.tournament_matches;
create trigger trg_tournament_matches_updated_at
before update on public.tournament_matches
for each row execute function public.set_updated_at();

drop trigger if exists trg_matches_updated_at on public.matches;
create trigger trg_matches_updated_at
before update on public.matches
for each row execute function public.set_updated_at();

drop trigger if exists trg_live_match_updated_at on public.live_match_state;
create trigger trg_live_match_updated_at
before update on public.live_match_state
for each row execute function public.set_updated_at();

drop trigger if exists trg_sync_metadata_updated_at on public.sync_metadata;
create trigger trg_sync_metadata_updated_at
before update on public.sync_metadata
for each row execute function public.set_updated_at();

alter table public.profiles enable row level security;
alter table public.players enable row level security;
alter table public.tournaments enable row level security;
alter table public.tournament_matches enable row level security;
alter table public.matches enable row level security;
alter table public.live_match_state enable row level security;
alter table public.sync_metadata enable row level security;

drop policy if exists profiles_select_self_or_admin on public.profiles;
create policy profiles_select_self_or_admin
on public.profiles
for select
to authenticated
using (auth.uid() = id or public.is_admin(auth.uid()));

drop policy if exists profiles_update_self_or_admin on public.profiles;
create policy profiles_update_self_or_admin
on public.profiles
for update
to authenticated
using (auth.uid() = id or public.is_admin(auth.uid()))
with check (
  (auth.uid() = id and role in ('viewer', 'admin'))
  or public.is_admin(auth.uid())
);

drop policy if exists profiles_insert_self_or_admin on public.profiles;
create policy profiles_insert_self_or_admin
on public.profiles
for insert
to authenticated
with check (auth.uid() = id or public.is_admin(auth.uid()));

drop policy if exists public_read_players on public.players;
create policy public_read_players
on public.players
for select
to anon, authenticated
using (deleted_at is null);

drop policy if exists admin_write_players on public.players;
create policy admin_write_players
on public.players
for all
to authenticated
using (public.is_admin(auth.uid()))
with check (public.is_admin(auth.uid()));

drop policy if exists public_read_tournaments on public.tournaments;
create policy public_read_tournaments
on public.tournaments
for select
to anon, authenticated
using (deleted_at is null);

drop policy if exists admin_write_tournaments on public.tournaments;
create policy admin_write_tournaments
on public.tournaments
for all
to authenticated
using (public.is_admin(auth.uid()))
with check (public.is_admin(auth.uid()));

drop policy if exists public_read_tournament_matches on public.tournament_matches;
create policy public_read_tournament_matches
on public.tournament_matches
for select
to anon, authenticated
using (deleted_at is null);

drop policy if exists admin_write_tournament_matches on public.tournament_matches;
create policy admin_write_tournament_matches
on public.tournament_matches
for all
to authenticated
using (public.is_admin(auth.uid()))
with check (public.is_admin(auth.uid()));

drop policy if exists public_read_matches on public.matches;
create policy public_read_matches
on public.matches
for select
to anon, authenticated
using (deleted_at is null);

drop policy if exists admin_write_matches on public.matches;
create policy admin_write_matches
on public.matches
for all
to authenticated
using (public.is_admin(auth.uid()))
with check (public.is_admin(auth.uid()));

drop policy if exists public_read_live_match on public.live_match_state;
create policy public_read_live_match
on public.live_match_state
for select
to anon, authenticated
using (true);

drop policy if exists admin_write_live_match on public.live_match_state;
create policy admin_write_live_match
on public.live_match_state
for all
to authenticated
using (public.is_admin(auth.uid()))
with check (public.is_admin(auth.uid()));

drop policy if exists admin_sync_metadata on public.sync_metadata;
create policy admin_sync_metadata
on public.sync_metadata
for all
to authenticated
using (public.is_admin(auth.uid()))
with check (public.is_admin(auth.uid()));

create or replace view public.player_rankings as
with completed_matches as (
  select *
  from public.matches m
  where m.deleted_at is null
    and m.ended_at_ms is not null
)
select
  p.id as player_id,
  p.name,
  p.image_uri,
  p.archived,
  count(cm.id)::int as games_played,
  sum(case when cm.winner_player_id = p.id and coalesce(cm.is_draw, false) = false then 1 else 0 end)::int as wins,
  sum(case when coalesce(cm.is_draw, false) = true then 1 else 0 end)::int as draws,
  sum(case when cm.id is not null and coalesce(cm.is_draw, false) = false and cm.winner_player_id <> p.id then 1 else 0 end)::int as losses,
  greatest(
    coalesce(max(case when cm.player1_id = p.id then cm.player1_highest_break end), 0),
    coalesce(max(case when cm.player2_id = p.id then cm.player2_highest_break end), 0)
  )::int as max_break,
  coalesce(avg(case when cm.player1_id = p.id then cm.player1_score::numeric when cm.player2_id = p.id then cm.player2_score::numeric end), 0)::numeric(10,2) as average_points_per_match,
  greatest(
    p.tournaments_won,
    coalesce((
      select count(*)::int
      from public.tournaments t
      where t.deleted_at is null and t.champion_player_id = p.id
    ), 0)
  ) as tournaments_won
from public.players p
left join completed_matches cm
  on cm.player1_id = p.id
  or cm.player2_id = p.id
where p.deleted_at is null
group by p.id, p.name, p.image_uri, p.archived, p.tournaments_won;

grant select on public.player_rankings to anon, authenticated;

create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, username, role)
  values (
    new.id,
    coalesce(nullif(split_part(new.email, '@', 1), ''), 'user_' || substr(new.id::text, 1, 8)),
    case when lower(coalesce(new.email, '')) = 'admin@elocho.local' then 'admin' else 'viewer' end
  )
  on conflict (id) do update
    set username = excluded.username,
        role = excluded.role,
        updated_at = timezone('utc', now());

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

insert into public.profiles (id, username, role)
select
  u.id,
  coalesce(nullif(split_part(u.email, '@', 1), ''), 'user_' || substr(u.id::text, 1, 8)),
  case when lower(coalesce(u.email, '')) = 'admin@elocho.local' then 'admin' else 'viewer' end
from auth.users u
on conflict (id) do update
set
  username = excluded.username,
  role = excluded.role,
  updated_at = timezone('utc', now());
