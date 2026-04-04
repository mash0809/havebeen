import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "HaveBeen - 투자 시뮬레이터",
  description: "과거 투자 시뮬레이션 서비스",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body
        className="min-h-screen"
        style={{ backgroundColor: "#1a1a2e", color: "#ffffff" }}
      >
        {children}
      </body>
    </html>
  );
}
