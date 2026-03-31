import Link from "next/link";

const links = [
  { href: "/players", label: "Players" },
  { href: "/tournaments", label: "Tournaments" },
  { href: "/live", label: "Live Match" },
  { href: "/admin/login", label: "Admin Login" }
];

export function NavBar() {
  return (
    <header className="site-header">
      <div className="site-shell site-header-inner">
        <Link className="brand" href="/players">
          <span className="brand-dot" />
          El Ocho
        </Link>
        <nav className="site-nav" aria-label="Main">
          {links.map((link) => (
            <Link key={link.href} href={link.href} className="site-nav-link">
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}
