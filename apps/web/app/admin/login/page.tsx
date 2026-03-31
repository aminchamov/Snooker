"use client";

import { FormEvent, useState } from "react";
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
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("12345678");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
    router.push("/admin");
  }

  return (
    <div className="grid" style={{ gap: "1rem" }}>
      <section>
        <h1 className="page-title">Admin Login</h1>
        <p className="page-subtitle">Use username <strong>admin</strong> and password <strong>12345678</strong>.</p>
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
                required
              />
            </div>
            <button className="button" type="submit" disabled={isLoading}>
              {isLoading ? "Signing in..." : "Sign in"}
            </button>
            {error ? <p style={{ color: "#c92a2a" }}>{error}</p> : null}
          </form>
        </div>
      </section>
    </div>
  );
}
