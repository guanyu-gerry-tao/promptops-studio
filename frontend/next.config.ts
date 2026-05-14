import type { NextConfig } from "next";

const platformApiUrl = process.env.PLATFORM_API_URL ?? "http://localhost:8081";

const nextConfig: NextConfig = {
  output: "standalone",
  // Proxy /api requests to the Spring Boot backend during development
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${platformApiUrl}/:path*`,
      },
    ];
  },
};

export default nextConfig;
