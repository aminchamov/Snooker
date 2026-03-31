import type { Metadata } from "next";
import { NavBar } from "@/components/NavBar";
import "./globals.css";

export const metadata: Metadata = {
  title: "El Ocho Public Board",
  description: "Public rankings, tournaments, and live snooker match board for El Ocho"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <NavBar />
        <main className="site-shell page-root">{children}</main>
      </body>
    </html>
  );
}
