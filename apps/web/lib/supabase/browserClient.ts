"use client";

import { createClient } from "@supabase/supabase-js";

const supabaseUrl =
  process.env.NEXT_PUBLIC_SUPABASE_URL ?? "https://blcgiuseyrkjbtitpvxm.supabase.co";
const supabaseKey =
  process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY ?? "sb_publishable_oiFK-MsEoyiUWqH7lU_h0w_wpeVvNYb";

export const browserSupabase = createClient(supabaseUrl, supabaseKey);
