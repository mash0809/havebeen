import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "껄무새 - 그때살껄",
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
        className="min-h-screen bg-charcoal text-white"
      >
        {children}
      </body>
    </html>
  );
}
