 "use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/players", label: "Players" },
  { href: "/tournaments", label: "Tournaments" },
  { href: "/live", label: "Live Match" },
  { href: "/admin/login", label: "Admin Login" }
];

export function NavBar() {
  const pathname = usePathname();

  function isActive(href: string): boolean {
    if (!pathname) return false;
    if (href === "/admin/login") return pathname.startsWith("/admin");
    return pathname === href || pathname.startsWith(`${href}/`);
  }

  return (
    <header className="site-header">
      <div className="site-shell site-header-inner">
        <Link className="brand" href="/players">
          <img src="/elocho_logo.png" alt="El Ocho logo" className="brand-logo" />
          <span className="brand-text-wrap">
            <span className="brand-text-main">EL OCHO</span>
            <span className="brand-text-sub">SNOOKER LOUNGE</span>
          </span>
        </Link>
        <nav className="site-nav" aria-label="Main">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`site-nav-link ${isActive(link.href) ? "is-active" : ""}`.trim()}
            >
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}
