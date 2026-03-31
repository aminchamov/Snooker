import { LiveMatchClient } from "@/components/LiveMatchClient";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import type { LiveMatchRow } from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function LiveMatchPage() {
  const supabase = createPublicSupabaseClient();
  const { data } = await supabase
    .from("live_match_state")
    .select("*")
    .eq("id", "active")
    .limit(1);

  const initialLive = ((data ?? []) as LiveMatchRow[])[0] ?? null;
  return <LiveMatchClient initialLive={initialLive} />;
}
