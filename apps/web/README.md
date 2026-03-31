# El Ocho Web App

## Run

1. Copy `.env.example` to `.env.local`.
2. Install dependencies:
   - `npm install`
3. Start dev server:
   - `npm run dev`

## Admin auth

- Username: `admin`
- Password: `12345678`
- Supabase email mapping: `admin@elocho.local`

Create the auth account once in Supabase Auth dashboard (Email provider):
- Email: `admin@elocho.local`
- Password: `12345678`

The SQL migration automatically maps this account to `profiles.role = 'admin'`.

## Public routes

- `/players`
- `/tournaments`
- `/live`

## Admin routes

- `/admin/login`
- `/admin`
