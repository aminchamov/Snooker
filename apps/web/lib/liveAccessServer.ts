import { cookies } from "next/headers";
import { LIVE_ACCESS_COOKIE } from "@/lib/liveAccessShared";

export async function hasLiveAccess(): Promise<boolean> {
  const cookieStore = await cookies();
  return cookieStore.get(LIVE_ACCESS_COOKIE)?.value === "granted";
}
