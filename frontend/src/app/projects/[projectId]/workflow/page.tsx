"use client";

import { use, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createWorkflow,
  executeCase,
  getToken,
  listRuns,
  listWorkflows,
  type Run,
  type RunTrace,
  type Workflow,
} from "@/lib/api";

interface PageProps {
  params: Promise<{ projectId: string }>;
}

function parseJson(value?: string) {
  if (!value) return null;
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function prettyJson(value?: string) {
  const parsed = parseJson(value);
  if (parsed === null) return "";
  return typeof parsed === "string" ? parsed : JSON.stringify(parsed, null, 2);
}

export default function WorkflowPage({ params }: PageProps) {
  const { projectId: projectIdStr } = use(params);
  const projectId = Number(projectIdStr);
  const router = useRouter();

  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [runs, setRuns] = useState<Run[]>([]);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<number | "">("");
  const [caseId, setCaseId] = useState("case-1");
  const [userInput, setUserInput] = useState("");
  const [running, setRunning] = useState(false);
  const [creatingWorkflow, setCreatingWorkflow] = useState(false);
  const [activeRun, setActiveRun] = useState<Run | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const selectedWorkflow = useMemo(
    () => workflows.find((item) => item.id === selectedWorkflowId),
    [workflows, selectedWorkflowId],
  );

  const refresh = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [workflowData, runData] = await Promise.all([
        listWorkflows(projectId),
        listRuns(projectId),
      ]);
      setWorkflows(workflowData);
      setRuns(runData);
      setSelectedWorkflowId((current) => current || workflowData[0]?.id || "");
      setActiveRun((current) => current || runData[0] || null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load workflow data");
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    if (!getToken()) {
      router.push("/login");
      return;
    }
    void refresh();
  }, [refresh, router]);

  async function handleCreateDefaultWorkflow() {
    setCreatingWorkflow(true);
    setError("");
    try {
      const workflow = await createWorkflow(projectId, "RAG JSON Workflow");
      setWorkflows((items) => [workflow, ...items]);
      setSelectedWorkflowId(workflow.id);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to create workflow");
    } finally {
      setCreatingWorkflow(false);
    }
  }

  async function handleExecute(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedWorkflowId || !caseId.trim() || !userInput.trim()) return;
    setRunning(true);
    setError("");
    try {
      const run = await executeCase(
        projectId,
        Number(selectedWorkflowId),
        caseId.trim(),
        userInput.trim(),
      );
      setActiveRun(run);
      setRuns((items) => [run, ...items.filter((item) => item.id !== run.id)]);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Workflow execution failed");
    } finally {
      setRunning(false);
    }
  }

  function statusBadge(status: string) {
    const colors: Record<string, string> = {
      SUCCESS: "bg-green-100 text-green-700",
      FAILED: "bg-red-100 text-red-600",
      RUNNING: "bg-yellow-100 text-yellow-700",
      QUEUED: "bg-gray-100 text-gray-600",
    };
    return (
      <span className={`rounded px-2 py-0.5 text-xs font-medium ${colors[status] ?? "bg-gray-100 text-gray-600"}`}>
        {status}
      </span>
    );
  }

  function traceSummary(trace: RunTrace) {
    return trace.outputSummary || trace.inputSummary || "";
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="flex items-center justify-between bg-white px-6 py-4 shadow-sm">
        <h1 className="text-lg font-bold text-gray-900">PromptOps Studio</h1>
        <div className="flex gap-4">
          <button
            onClick={() => router.push(`/projects/${projectId}/kb`)}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            Knowledge Base
          </button>
          <button
            onClick={() => router.push("/projects")}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            Projects
          </button>
        </div>
      </nav>

      <main className="mx-auto max-w-5xl px-6 py-10 space-y-8">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Workflow Run</h2>
            <p className="mt-1 text-sm text-gray-500">
              Execute one RAG JSON case and inspect the node-level trace.
            </p>
          </div>
          <button
            onClick={handleCreateDefaultWorkflow}
            disabled={creatingWorkflow}
            className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {creatingWorkflow ? "Creating..." : "+ RAG JSON"}
          </button>
        </div>

        {error && (
          <div className="rounded bg-red-50 p-3 text-sm text-red-600">{error}</div>
        )}

        {loading ? (
          <p className="text-gray-400">Loading...</p>
        ) : (
          <>
            <form
              onSubmit={handleExecute}
              className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm space-y-4"
            >
              <div className="grid gap-4 md:grid-cols-[2fr_1fr]">
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700">Workflow</label>
                  <select
                    value={selectedWorkflowId}
                    onChange={(e) => setSelectedWorkflowId(e.target.value ? Number(e.target.value) : "")}
                    className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Create or select a workflow</option>
                    {workflows.map((workflow) => (
                      <option key={workflow.id} value={workflow.id}>
                        {workflow.name} ({workflow.templateId})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700">Case ID</label>
                  <input
                    type="text"
                    value={caseId}
                    onChange={(e) => setCaseId(e.target.value)}
                    className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>
              </div>

              {selectedWorkflow && (
                <div className="rounded bg-blue-50 px-3 py-2 text-xs text-blue-700">
                  Template: {selectedWorkflow.templateId} · Output schema: qa_answer
                </div>
              )}

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">Case Input</label>
                <textarea
                  value={userInput}
                  onChange={(e) => setUserInput(e.target.value)}
                  rows={5}
                  className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Ask a question that should be answered from the uploaded KB documents..."
                  required
                />
              </div>

              <button
                type="submit"
                disabled={running || !selectedWorkflowId}
                className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {running ? "Running..." : "Execute Case"}
              </button>
            </form>

            {activeRun && (
              <section className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-xl font-bold text-gray-900">Run #{activeRun.id}</h3>
                  {statusBadge(activeRun.status)}
                </div>

                {activeRun.errorMessage && (
                  <div className="rounded bg-red-50 p-3 text-sm text-red-600">{activeRun.errorMessage}</div>
                )}

                <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                  <h4 className="mb-3 font-semibold text-gray-900">Output JSON</h4>
                  <pre className="max-h-80 overflow-auto rounded bg-gray-950 p-4 text-xs leading-relaxed text-gray-100">
                    {prettyJson(activeRun.outputJson) || "(no output)"}
                  </pre>
                </div>

                <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                  <h4 className="mb-3 font-semibold text-gray-900">Node Trace</h4>
                  {!activeRun.traces || activeRun.traces.length === 0 ? (
                    <p className="text-sm text-gray-400">No trace records yet.</p>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full border-collapse text-left text-sm">
                        <thead>
                          <tr className="border-b text-xs uppercase text-gray-400">
                            <th className="py-2 pr-4">Node</th>
                            <th className="py-2 pr-4">Latency</th>
                            <th className="py-2 pr-4">Tokens</th>
                            <th className="py-2">Summary</th>
                          </tr>
                        </thead>
                        <tbody>
                          {activeRun.traces.map((trace, index) => (
                            <tr key={`${trace.nodeName}-${index}`} className="border-b last:border-0">
                              <td className="py-3 pr-4 font-medium text-gray-900">{trace.nodeName}</td>
                              <td className="py-3 pr-4 text-gray-600">{trace.latencyMs ?? 0} ms</td>
                              <td className="py-3 pr-4 text-gray-600">{trace.tokenCount ?? "-"}</td>
                              <td className="py-3 text-gray-700">{traceSummary(trace)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </section>
            )}

            {runs.length > 0 && (
              <section>
                <h3 className="mb-3 text-lg font-bold text-gray-900">Recent Runs</h3>
                <ul className="space-y-2">
                  {runs.slice(0, 5).map((run) => (
                    <li
                      key={run.id}
                      className="flex items-center justify-between rounded-lg border border-gray-200 bg-white px-4 py-3 shadow-sm"
                    >
                      <button
                        onClick={() => setActiveRun(run)}
                        className="text-left text-sm font-medium text-gray-900 hover:text-blue-700"
                      >
                        #{run.id} · {run.caseId}
                      </button>
                      {statusBadge(run.status)}
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </>
        )}
      </main>
    </div>
  );
}
