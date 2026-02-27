"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getToken, listProjects, createProject, type Project } from "@/lib/api";

export default function ProjectsPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // New project form state
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      router.push("/login");
      return;
    }
    fetchProjects();
  }, [router]);

  async function fetchProjects() {
    try {
      const data = await listProjects();
      setProjects(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load projects");
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    setCreating(true);
    try {
      await createProject(name.trim(), description.trim() || undefined);
      setName("");
      setDescription("");
      setShowForm(false);
      await fetchProjects();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to create project");
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navbar */}
      <nav className="flex items-center justify-between bg-white px-6 py-4 shadow-sm">
        <h1 className="text-lg font-bold text-gray-900">PromptOps Studio</h1>
        <button
          onClick={() => router.push("/")}
          className="text-sm text-gray-500 hover:text-gray-700"
        >
          ← Dashboard
        </button>
      </nav>

      <main className="mx-auto max-w-3xl px-6 py-10">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-2xl font-bold text-gray-900">Projects</h2>
          <button
            onClick={() => setShowForm(!showForm)}
            className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            + New Project
          </button>
        </div>

        {/* New project form */}
        {showForm && (
          <form
            onSubmit={handleCreate}
            className="mb-6 rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
          >
            <h3 className="mb-4 font-semibold text-gray-800">Create Project</h3>
            <div className="mb-3">
              <label className="mb-1 block text-sm font-medium text-gray-700">Name *</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="My Project"
                required
              />
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm font-medium text-gray-700">Description</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Optional description"
              />
            </div>
            <div className="flex gap-2">
              <button
                type="submit"
                disabled={creating}
                className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {creating ? "Creating..." : "Create"}
              </button>
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="rounded bg-gray-100 px-4 py-2 text-sm text-gray-700 hover:bg-gray-200"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {error && (
          <div className="mb-4 rounded bg-red-50 p-3 text-sm text-red-600">{error}</div>
        )}

        {loading ? (
          <p className="text-gray-400">Loading...</p>
        ) : projects.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-300 p-10 text-center text-gray-400">
            No projects yet. Create your first project above.
          </div>
        ) : (
          <ul className="space-y-3">
            {projects.map((p) => (
              <li
                key={p.id}
                className="flex items-center justify-between rounded-lg border border-gray-200 bg-white px-5 py-4 shadow-sm"
              >
                <div>
                  <p className="font-medium text-gray-900">{p.name}</p>
                  {p.description && (
                    <p className="text-sm text-gray-500">{p.description}</p>
                  )}
                </div>
                <button
                  onClick={() => router.push(`/projects/${p.id}/kb`)}
                  className="rounded bg-gray-100 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-200"
                >
                  Knowledge Base →
                </button>
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  );
}
