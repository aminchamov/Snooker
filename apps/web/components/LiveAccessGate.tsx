type Props = {
  returnTo?: string;
  error?: boolean;
  title?: string;
  subtitle?: string;
};

export function LiveAccessGate({
  returnTo = "/live",
  error = false,
  title = "Live Match Access Locked",
  subtitle = "Enter the live-view password to open the live scoreboard and other protected live match views."
}: Props) {
  return (
    <section className="panel live-lock-panel">
      <div className="panel-header">
        <h1 className="page-title">{title}</h1>
        <p className="page-subtitle">{subtitle}</p>
      </div>
      <div className="panel-body">
        <form action="/live/unlock" method="post" className="live-lock-form">
          <input type="hidden" name="returnTo" value={returnTo} />
          <label className="form-row" htmlFor="live-password">
            <span>Live Password</span>
            <input
              id="live-password"
              name="password"
              type="password"
              className="input"
              placeholder="Enter password"
              autoComplete="current-password"
              required
            />
          </label>
          {error ? <p className="live-lock-error">Incorrect password. Please try again.</p> : null}
          <button className="button" type="submit">Unlock Live Matches</button>
        </form>
      </div>
    </section>
  );
}
