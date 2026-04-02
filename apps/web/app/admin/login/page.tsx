"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { browserSupabase } from "@/lib/supabase/browserClient";

const ADMIN_EMAIL = "admin@elocho.local";

function mapUsernameToEmail(username: string): string {
  const trimmed = username.trim();
  if (trimmed.toLowerCase() === "admin") return ADMIN_EMAIL;
  if (trimmed.includes("@")) return trimmed;
  return `${trimmed}@elocho.local`;
}

export default function AdminLoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [checkingExistingSession, setCheckingExistingSession] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function checkExistingSession() {
      const auth = await browserSupabase.auth.getSession();
      const session = auth.data.session;

      if (!session?.user) {
        if (isMounted) setCheckingExistingSession(false);
        return;
      }

      const profile = await browserSupabase
        .from("profiles")
        .select("role")
        .eq("id", session.user.id)
        .single();

      if (profile.data?.role === "admin") {
        router.replace("/admin");
        return;
      }

      if (isMounted) setCheckingExistingSession(false);
    }

    void checkExistingSession();
    return () => {
      isMounted = false;
    };
  }, [router]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsLoading(true);
    setError(null);

    const email = mapUsernameToEmail(username);

    const signIn = await browserSupabase.auth.signInWithPassword({
      email,
      password
    });

    if (signIn.error || !signIn.data.user) {
      setIsLoading(false);
      setError(signIn.error?.message ?? "Login failed");
      return;
    }

    const profile = await browserSupabase
      .from("profiles")
      .select("role")
      .eq("id", signIn.data.user.id)
      .single();

    if (profile.error || profile.data?.role !== "admin") {
      await browserSupabase.auth.signOut();
      setIsLoading(false);
      setError("Authenticated user is not an admin.");
      return;
    }

    setIsLoading(false);
    router.replace("/admin");
  }

  if (checkingExistingSession) {
    return (
      <div className="grid" style={{ gap: "1rem" }}>
        <section>
          <h1 className="page-title">Admin Login</h1>
          <p className="page-subtitle">Checking existing session...</p>
        </section>
      </div>
    );
  }

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Admin Login</h1>
        <p className="page-subtitle">Sign in with your admin credentials.</p>
      </section>

      <section className="panel" style={{ maxWidth: 460 }}>
        <div className="panel-body" style={{ paddingTop: "1rem" }}>
          <form onSubmit={onSubmit}>
            <div className="form-row">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                className="input"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                required
              />
            </div>
            <div className="form-row">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                className="input"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                required
              />
            </div>
            <button className="button" type="submit" disabled={isLoading}>
              {isLoading ? "Signing in..." : "Sign in"}
            </button>
            {error ? <p style={{ color: "#d14d4d" }}>{error}</p> : null}
          </form>
        </div>
      </section>
    </div>
  );
}
