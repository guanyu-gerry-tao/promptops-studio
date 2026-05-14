import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "PromptOps Studio",
  description: "Enterprise AI Application Platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
