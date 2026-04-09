import { LiveAccessGate } from "@/components/LiveAccessGate";
import { LiveMatchClient } from "@/components/LiveMatchClient";
import { hasLiveAccess } from "@/lib/liveAccessServer";
import { createPublicSupabaseClient } from "@/lib/supabase/publicClient";
import type { LiveMatchRow } from "@/lib/types";

export const dynamic = "force-dynamic";

type Props = {
  searchParams?: Promise<{ error?: string }>;
};

export default async function LiveMatchPage({ searchParams }: Props) {
  const unlocked = await hasLiveAccess();
  const resolvedSearchParams = (await searchParams) ?? {};
  if (!unlocked) {
    return <LiveAccessGate returnTo="/live" error={resolvedSearchParams.error === "1"} />;
  }

  const supabase = createPublicSupabaseClient();
  const { data } = await supabase
    .from("live_match_state")
    .select("*")
    .eq("id", "active")
    .limit(1);

  const initialLive = ((data ?? []) as LiveMatchRow[])[0] ?? null;
  return <LiveMatchClient initialLive={initialLive} />;
}
