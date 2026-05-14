import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "LeadSpark AI",
  description: "AI-powered proactive sales system",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
