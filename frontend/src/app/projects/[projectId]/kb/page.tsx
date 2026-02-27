"use client";

import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";
import {
  getToken,
  uploadDoc,
  listDocs,
  deleteDoc,
  searchKb,
  type KbDoc,
  type ChunkResult,
} from "@/lib/api";

interface PageProps {
  params: Promise<{ projectId: string }>;
}

export default function KbPage({ params }: PageProps) {
  const { projectId: projectIdStr } = use(params);
  const projectId = Number(projectIdStr);
  const router = useRouter();

  // --- Doc list state ---
  const [docs, setDocs] = useState<KbDoc[]>([]);
  const [docsLoading, setDocsLoading] = useState(true);

  // --- Upload form state ---
  const [uploadTitle, setUploadTitle] = useState("");
  const [uploadContent, setUploadContent] = useState("");
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");

  // --- Search debug panel state ---
  const [query, setQuery] = useState("");
  const [alpha, setAlpha] = useState(0.5);
  const [topK, setTopK] = useState(5);
  const [searching, setSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<ChunkResult[] | null>(null);
  const [searchError, setSearchError] = useState("");

  useEffect(() => {
    if (!getToken()) {
      router.push("/login");
      return;
    }
    fetchDocs();
  }, [projectId, router]);

  async function fetchDocs() {
    setDocsLoading(true);
    try {
      const data = await listDocs(projectId);
      setDocs(data);
    } finally {
      setDocsLoading(false);
    }
  }

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    if (!uploadTitle.trim() || !uploadContent.trim()) return;
    setUploading(true);
    setUploadError("");
    try {
      await uploadDoc(projectId, uploadTitle.trim(), uploadContent.trim());
      setUploadTitle("");
      setUploadContent("");
      await fetchDocs();
    } catch (e: unknown) {
      setUploadError(e instanceof Error ? e.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(docId: number) {
    try {
      await deleteDoc(projectId, docId);
      await fetchDocs();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
  }

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setSearching(true);
    setSearchError("");
    setSearchResults(null);
    try {
      const result = await searchKb(projectId, query.trim(), topK, alpha, false);
      setSearchResults(result.results);
    } catch (e: unknown) {
      setSearchError(e instanceof Error ? e.message : "Search failed");
    } finally {
      setSearching(false);
    }
  }

  function statusBadge(status: string) {
    const colors: Record<string, string> = {
      INDEXED: "bg-green-100 text-green-700",
      INDEXING: "bg-yellow-100 text-yellow-700",
      FAILED: "bg-red-100 text-red-600",
    };
    return (
      <span className={`rounded px-2 py-0.5 text-xs font-medium ${colors[status] ?? "bg-gray-100 text-gray-600"}`}>
        {status}
      </span>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navbar */}
      <nav className="flex items-center justify-between bg-white px-6 py-4 shadow-sm">
        <h1 className="text-lg font-bold text-gray-900">PromptOps Studio</h1>
        <button
          onClick={() => router.push("/projects")}
          className="text-sm text-gray-500 hover:text-gray-700"
        >
          ← Projects
        </button>
      </nav>

      <main className="mx-auto max-w-4xl px-6 py-10 space-y-10">

        {/* ── Section 1: Upload Document ── */}
        <section>
          <h2 className="mb-4 text-xl font-bold text-gray-900">Upload Document</h2>
          <form
            onSubmit={handleUpload}
            className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm space-y-4"
          >
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">Title *</label>
              <input
                type="text"
                value={uploadTitle}
                onChange={(e) => setUploadTitle(e.target.value)}
                className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g. Python Guide"
                required
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">Content *</label>
              <textarea
                value={uploadContent}
                onChange={(e) => setUploadContent(e.target.value)}
                rows={6}
                className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Paste document content here (Markdown supported)..."
                required
              />
            </div>
            {uploadError && (
              <p className="text-sm text-red-500">{uploadError}</p>
            )}
            <button
              type="submit"
              disabled={uploading}
              className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {uploading ? "Indexing..." : "Upload & Index"}
            </button>
          </form>
        </section>

        {/* ── Section 2: Document List ── */}
        <section>
          <h2 className="mb-4 text-xl font-bold text-gray-900">Documents</h2>
          {docsLoading ? (
            <p className="text-gray-400">Loading...</p>
          ) : docs.length === 0 ? (
            <div className="rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-400">
              No documents yet. Upload your first document above.
            </div>
          ) : (
            <ul className="space-y-2">
              {docs.map((doc) => (
                <li
                  key={doc.id}
                  className="flex items-center justify-between rounded-lg border border-gray-200 bg-white px-5 py-3 shadow-sm"
                >
                  <div className="flex items-center gap-3">
                    {statusBadge(doc.status)}
                    <span className="font-medium text-gray-800">{doc.title}</span>
                    {doc.status === "INDEXED" && (
                      <span className="text-xs text-gray-400">{doc.chunksCount} chunks</span>
                    )}
                    {doc.status === "FAILED" && doc.errorMessage && (
                      <span className="text-xs text-red-400">{doc.errorMessage}</span>
                    )}
                  </div>
                  <button
                    onClick={() => handleDelete(doc.id)}
                    className="text-xs text-gray-400 hover:text-red-500"
                  >
                    Delete
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* ── Section 3: Search Debug Panel ── */}
        <section>
          <h2 className="mb-1 text-xl font-bold text-gray-900">Search Debug Panel</h2>
          <p className="mb-4 text-sm text-gray-500">
            Tune alpha to compare pure keyword (0.0) vs pure vector (1.0) retrieval.
          </p>
          <form
            onSubmit={handleSearch}
            className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm space-y-4"
          >
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">Query</label>
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Ask something..."
                required
              />
            </div>

            {/* Alpha slider */}
            <div>
              <div className="mb-1 flex items-center justify-between">
                <label className="text-sm font-medium text-gray-700">
                  Alpha (BM25 ← → Vector)
                </label>
                <span className="text-sm font-mono text-blue-600">{alpha.toFixed(1)}</span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.1}
                value={alpha}
                onChange={(e) => setAlpha(Number(e.target.value))}
                className="w-full accent-blue-600"
              />
              <div className="mt-1 flex justify-between text-xs text-gray-400">
                <span>0.0 Pure BM25</span>
                <span>0.5 Balanced</span>
                <span>1.0 Pure Vector</span>
              </div>
            </div>

            {/* Top K */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">Top K</label>
              <input
                type="number"
                min={1}
                max={20}
                value={topK}
                onChange={(e) => setTopK(Number(e.target.value))}
                className="w-24 rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <button
              type="submit"
              disabled={searching}
              className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {searching ? "Searching..." : "Search"}
            </button>
          </form>

          {/* Results */}
          {searchError && (
            <div className="mt-4 rounded bg-red-50 p-3 text-sm text-red-600">{searchError}</div>
          )}
          {searchResults !== null && (
            <div className="mt-4">
              <p className="mb-2 text-sm text-gray-500">
                {searchResults.length} result{searchResults.length !== 1 ? "s" : ""} — alpha={alpha.toFixed(1)}
              </p>
              {searchResults.length === 0 ? (
                <div className="rounded-lg border border-dashed border-gray-300 p-6 text-center text-gray-400">
                  No results found. Try a different query or upload more documents.
                </div>
              ) : (
                <ul className="space-y-3">
                  {searchResults.map((r, i) => (
                    <li
                      key={`${r.doc_id}-${r.chunk_id}`}
                      className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm"
                    >
                      <div className="mb-2 flex items-center justify-between">
                        <span className="text-xs font-medium text-gray-500">
                          #{i + 1} · {r.title}
                        </span>
                        <span className="rounded bg-blue-50 px-2 py-0.5 text-xs font-mono text-blue-600">
                          score: {r.score.toFixed(4)}
                        </span>
                      </div>
                      <p className="text-sm leading-relaxed text-gray-700">{r.text}</p>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
