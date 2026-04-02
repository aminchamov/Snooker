 "use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/", label: "Home" },
  { href: "/players", label: "Players" },
  { href: "/tournaments", label: "Tournaments" },
  { href: "/live", label: "Live Match" },
  { href: "/admin", label: "Admin" }
];

export function NavBar() {
  const pathname = usePathname();

  function isActive(href: string): boolean {
    if (!pathname) return false;
    if (href === "/admin") return pathname.startsWith("/admin");
    if (href === "/") return pathname === "/";
    return pathname === href || pathname.startsWith(`${href}/`);
  }

  return (
    <header className="site-header">
      <div className="site-shell site-header-inner">
        <Link className="brand" href="/">
          <img src="/elocho_logo.png" alt="El Ocho logo" className="brand-logo" />
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
