# El Ocho Supabase + Web Sync Setup

## 1) Apply SQL migration

Run the migration in your Supabase SQL editor:

- `supabase/migrations/20260331160000_init_el_ocho_sync.sql`

This creates:
- sync tables (`players`, `tournaments`, `tournament_matches`, `matches`, `live_match_state`, `sync_metadata`)
- `player_rankings` view
- RLS policies (public read, admin write)
- profile role helper + auth user trigger

## 2) Create admin auth account

In Supabase Auth dashboard create user:
- Email: `admin@elocho.local`
- Password: `12345678`

The migration maps this account to role `admin` in `profiles`.

## 3) Android app

The Android app already includes:
- Supabase URL + publishable key in `BuildConfig`
- offline-first Room + sync layer
- live snapshot publishing
- home-screen `Sync Data` button

Build:
- `./gradlew :app:compileDebugKotlin`

## 4) Web app

In `apps/web`:

1. `npm install`
2. `npm run dev`
3. Open:
   - Public: `/players`, `/tournaments`, `/live`
   - Admin: `/admin/login` (username `admin`, password `12345678`)
