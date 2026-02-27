// Auth token management + API request helpers

const TOKEN_KEY = "promptops_token";
const USER_KEY = "promptops_user";

export interface UserInfo {
  userId: number;
  username: string;
}

export interface LoginResult {
  token: string;
  userId: number;
  username: string;
}

// --- Token management ---

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function getUser(): UserInfo | null {
  if (typeof window === "undefined") return null;
  const str = localStorage.getItem(USER_KEY);
  if (!str) return null;
  return JSON.parse(str);
}

export function saveAuth(result: LoginResult) {
  localStorage.setItem(TOKEN_KEY, result.token);
  localStorage.setItem(USER_KEY, JSON.stringify({
    userId: result.userId,
    username: result.username,
  }));
  // Also set a cookie so middleware.ts can check auth on the server side
  document.cookie = `token=${result.token}; path=/; max-age=${60 * 60 * 24 * 7}`; // 7 days
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  document.cookie = "token=; path=/; max-age=0";
}

// --- API helpers ---

async function apiFetch(path: string, options: RequestInit = {}) {
  const token = getToken();
  const res = await fetch(`/api${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Request failed" }));
    throw new Error(error.message || `HTTP ${res.status}`);
  }

  // Handle empty responses (e.g., DELETE)
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export async function login(username: string, password: string): Promise<LoginResult> {
  const result = await apiFetch("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  saveAuth(result);
  return result;
}

export async function register(username: string, email: string, password: string, displayName?: string) {
  return apiFetch("/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, email, password, displayName }),
  });
}

// --- Projects ---

export interface Project {
  id: number;
  name: string;
  description?: string;
}

export async function listProjects(): Promise<Project[]> {
  return apiFetch("/projects");
}

export async function createProject(name: string, description?: string): Promise<Project> {
  return apiFetch("/projects", {
    method: "POST",
    body: JSON.stringify({ name, description }),
  });
}

// --- Knowledge Base ---

export interface KbDoc {
  id: number;
  projectId: number;
  title: string;
  status: string;        // "INDEXING" | "INDEXED" | "FAILED"
  chunksCount: number;
  errorMessage?: string;
}

export interface ChunkResult {
  doc_id: number;
  chunk_id: number;
  text: string;
  score: number;
  title: string;
}

export interface SearchResult {
  project_id: number;
  query: string;
  results: ChunkResult[];
  answer?: string;
}

export async function uploadDoc(projectId: number, title: string, content: string): Promise<KbDoc> {
  return apiFetch(`/projects/${projectId}/kb/docs`, {
    method: "POST",
    body: JSON.stringify({ title, content }),
  });
}

export async function listDocs(projectId: number): Promise<KbDoc[]> {
  return apiFetch(`/projects/${projectId}/kb/docs`);
}

export async function deleteDoc(projectId: number, docId: number): Promise<void> {
  return apiFetch(`/projects/${projectId}/kb/docs/${docId}`, {
    method: "DELETE",
  });
}

export async function searchKb(
  projectId: number,
  query: string,
  topK: number = 5,
  alpha?: number,
  generateAnswer: boolean = false,
): Promise<SearchResult> {
  return apiFetch(`/projects/${projectId}/kb/search`, {
    method: "POST",
    body: JSON.stringify({ query, top_k: topK, alpha, generate_answer: generateAnswer }),
  });
}
