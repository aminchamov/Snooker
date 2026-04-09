import { NextRequest, NextResponse } from "next/server";
import { LIVE_ACCESS_COOKIE, liveAccessCookieValue, validateLiveAccessPassword } from "@/lib/liveAccessShared";

function sanitizeReturnTo(value: string | null): string {
  if (!value || !value.startsWith("/")) return "/live";
  if (value.startsWith("//")) return "/live";
  return value;
}

export async function POST(request: NextRequest) {
  const formData = await request.formData();
  const password = String(formData.get("password") ?? "");
  const returnTo = sanitizeReturnTo(String(formData.get("returnTo") ?? "/live"));
  const redirectUrl = new URL(returnTo, request.url);

  if (!validateLiveAccessPassword(password)) {
    redirectUrl.searchParams.set("error", "1");
    return NextResponse.redirect(redirectUrl);
  }

  const response = NextResponse.redirect(redirectUrl);
  response.cookies.set(LIVE_ACCESS_COOKIE, liveAccessCookieValue(), {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60 * 24 * 7
  });
  return response;
}
