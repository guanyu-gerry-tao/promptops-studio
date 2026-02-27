"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getUser, getToken, clearAuth, type UserInfo } from "@/lib/api";

export default function Dashboard() {
  const router = useRouter();
  const [user, setUser] = useState<UserInfo | null>(null);

  useEffect(() => {
    // Client-side auth check: no token → redirect to login
    if (!getToken()) {
      router.push("/login");
      return;
    }
    setUser(getUser());
  }, [router]);

  function handleLogout() {
    clearAuth();
    router.push("/login");
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="flex items-center justify-between bg-white px-6 py-4 shadow-sm">
        <h1 className="text-lg font-bold text-gray-900">PromptOps Studio</h1>
        <div className="flex items-center gap-4">
          {user && (
            <span className="text-sm text-gray-600">
              Welcome, {user.username}
            </span>
          )}
          <button
            onClick={handleLogout}
            className="rounded bg-gray-100 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-200"
          >
            Logout
          </button>
        </div>
      </nav>

      <main className="mx-auto max-w-4xl px-6 py-16 text-center">
        <h2 className="mb-4 text-3xl font-bold text-gray-900">Dashboard</h2>
        <p className="mb-8 text-gray-500">
          Welcome to PromptOps Studio. Manage your projects and knowledge bases below.
        </p>
        <button
          onClick={() => router.push("/projects")}
          className="rounded-lg bg-blue-600 px-6 py-3 text-base font-medium text-white hover:bg-blue-700"
        >
          Go to Projects →
        </button>
      </main>
    </div>
  );
}
