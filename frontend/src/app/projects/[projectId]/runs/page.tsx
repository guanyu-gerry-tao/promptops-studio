"use client";

import { use, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createDataset,
  createWorkflow,
  getToken,
  listDatasets,
  listRunCases,
  listRuns,
  listWorkflows,
  startRun,
  type Dataset,
  type Run,
  type RunCase,
  type Workflow,
} from "@/lib/api";

interface PageProps {
  params: Promise<{ projectId: string }>;
}

const SAMPLE_JSONL = `{"case_id":"case-1","input":"What is the refund window?"}
{"case_id":"case-2","input":"Which sources support the answer?"}`;

function prettyJson(value?: string) {
  if (!value) return "";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

export default function RunsPage({ params }: PageProps) {
  const { projectId: projectIdStr } = use(params);
  const projectId = Number(projectIdStr);
  const router = useRouter();

  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [runs, setRuns] = useState<Run[]>([]);
  const [cases, setCases] = useState<RunCase[]>([]);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<number | "">("");
  const [selectedDatasetId, setSelectedDatasetId] = useState<number | "">("");
  const [activeRun, setActiveRun] = useState<Run | null>(null);
  const [datasetName, setDatasetName] = useState("Demo Dataset");
  const [datasetContent, setDatasetContent] = useState(SAMPLE_JSONL);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const selectedWorkflow = useMemo(
    () => workflows.find((item) => item.id === selectedWorkflowId),
    [selectedWorkflowId, workflows],
  );

  const refresh = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [workflowData, datasetData, runData] = await Promise.all([
        listWorkflows(projectId),
        listDatasets(projectId),
        listRuns(projectId),
      ]);
      setWorkflows(workflowData);
      setDatasets(datasetData);
      setRuns(runData);
      setSelectedWorkflowId((current) => current || workflowData[0]?.id || "");
      setSelectedDatasetId((current) => current || datasetData[0]?.id || "");
      if (runData[0]) {
        await selectRun(runData[0]);
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load runs");
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

  async function selectRun(run: Run) {
    setActiveRun(run);
    try {
      const runCases = await listRunCases(run.id);
      setCases(runCases);
    } catch {
      setCases([]);
    }
  }

  async function handleCreateWorkflow() {
    setBusy(true);
    setError("");
    try {
      const workflow = await createWorkflow(projectId, "RAG JSON Workflow");
      setWorkflows((items) => [workflow, ...items]);
      setSelectedWorkflowId(workflow.id);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to create workflow");
    } finally {
      setBusy(false);
    }
  }

  async function handleUploadDataset(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const dataset = await createDataset(projectId, datasetName.trim(), datasetContent.trim());
      setDatasets((items) => [dataset, ...items]);
      setSelectedDatasetId(dataset.id);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to upload dataset");
    } finally {
      setBusy(false);
    }
  }

  async function handleStartRun() {
    if (!selectedWorkflowId || !selectedDatasetId) return;
    setBusy(true);
    setError("");
    try {
      const run = await startRun(projectId, Number(selectedWorkflowId), Number(selectedDatasetId));
      setRuns((items) => [run, ...items.filter((item) => item.id !== run.id)]);
      await selectRun(run);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to start run");
    } finally {
      setBusy(false);
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

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="flex items-center justify-between bg-white px-6 py-4 shadow-sm">
        <h1 className="text-lg font-bold text-gray-900">PromptOps Studio</h1>
        <div className="flex gap-4">
          <button onClick={() => router.push(`/projects/${projectId}/workflow`)} className="text-sm text-gray-500 hover:text-gray-700">
            Workflow
          </button>
          <button onClick={() => router.push("/projects")} className="text-sm text-gray-500 hover:text-gray-700">
            Projects
          </button>
        </div>
      </nav>

      <main className="mx-auto max-w-6xl px-6 py-10 space-y-8">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Dataset Runs</h2>
          <p className="mt-1 text-sm text-gray-500">
            Upload JSONL cases, start a batch run, and inspect per-case results.
          </p>
        </div>

        {error && <div className="rounded bg-red-50 p-3 text-sm text-red-600">{error}</div>}

        {loading ? (
          <p className="text-gray-400">Loading...</p>
        ) : (
          <>
            <section className="grid gap-6 lg:grid-cols-[1fr_1fr]">
              <form onSubmit={handleUploadDataset} className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm space-y-4">
                <h3 className="font-semibold text-gray-900">1. Upload Dataset</h3>
                <input
                  value={datasetName}
                  onChange={(e) => setDatasetName(e.target.value)}
                  className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
                <textarea
                  value={datasetContent}
                  onChange={(e) => setDatasetContent(e.target.value)}
                  rows={7}
                  className="w-full rounded border border-gray-300 px-3 py-2 font-mono text-xs focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
                <button disabled={busy} className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
                  Upload JSONL
                </button>
              </form>

              <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm space-y-4">
                <h3 className="font-semibold text-gray-900">2. Start Run</h3>
                <div>
                  <div className="mb-1 flex items-center justify-between">
                    <label className="text-sm font-medium text-gray-700">Workflow</label>
                    <button onClick={handleCreateWorkflow} disabled={busy} className="text-xs font-medium text-blue-600 hover:text-blue-700">
                      + RAG JSON
                    </button>
                  </div>
                  <select
                    value={selectedWorkflowId}
                    onChange={(e) => setSelectedWorkflowId(e.target.value ? Number(e.target.value) : "")}
                    className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Select workflow</option>
                    {workflows.map((workflow) => (
                      <option key={workflow.id} value={workflow.id}>
                        {workflow.name} ({workflow.templateId})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700">Dataset</label>
                  <select
                    value={selectedDatasetId}
                    onChange={(e) => setSelectedDatasetId(e.target.value ? Number(e.target.value) : "")}
                    className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Select dataset</option>
                    {datasets.map((dataset) => (
                      <option key={dataset.id} value={dataset.id}>
                        {dataset.name} ({dataset.itemsCount} cases)
                      </option>
                    ))}
                  </select>
                </div>

                {selectedWorkflow && (
                  <div className="rounded bg-blue-50 px-3 py-2 text-xs text-blue-700">
                    Template: {selectedWorkflow.templateId} · schema: qa_answer
                  </div>
                )}

                <button
                  onClick={handleStartRun}
                  disabled={busy || !selectedWorkflowId || !selectedDatasetId}
                  className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:opacity-50"
                >
                  {busy ? "Working..." : "Start Batch Run"}
                </button>
              </div>
            </section>

            <section className="grid gap-6 lg:grid-cols-[320px_1fr]">
              <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="font-semibold text-gray-900">Runs</h3>
                  <button onClick={() => void refresh()} disabled={busy} className="text-xs font-medium text-blue-600 hover:text-blue-700">
                    Refresh
                  </button>
                </div>
                {runs.length === 0 ? (
                  <p className="text-sm text-gray-400">No runs yet.</p>
                ) : (
                  <ul className="space-y-2">
                    {runs.map((run) => (
                      <li key={run.id}>
                        <button
                          onClick={() => void selectRun(run)}
                          className="w-full rounded border border-gray-200 px-3 py-2 text-left hover:bg-gray-50"
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-gray-900">Run #{run.id}</span>
                            {statusBadge(run.status)}
                          </div>
                          <p className="mt-1 text-xs text-gray-500">
                            {run.successCases ?? 0}/{run.totalCases ?? 0} passed
                          </p>
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
                <div className="mb-4 flex items-center justify-between">
                  <h3 className="font-semibold text-gray-900">
                    {activeRun ? `Run #${activeRun.id} Cases` : "Run Cases"}
                  </h3>
                  {activeRun && statusBadge(activeRun.status)}
                </div>

                {cases.length === 0 ? (
                  <p className="text-sm text-gray-400">
                    {activeRun?.status === "QUEUED" || activeRun?.status === "RUNNING"
                      ? "Worker is still processing this run. Refresh to load case results."
                      : "Select or start a run to see cases."}
                  </p>
                ) : (
                  <div className="space-y-3">
                    {cases.map((item) => (
                      <article key={item.id} className="rounded border border-gray-200 p-4">
                        <div className="mb-2 flex items-center justify-between">
                          <span className="text-sm font-medium text-gray-900">{item.caseId}</span>
                          {statusBadge(item.status)}
                        </div>
                        <p className="mb-3 text-sm text-gray-600">{item.inputText}</p>
                        {item.errorMessage ? (
                          <p className="rounded bg-red-50 p-2 text-xs text-red-600">{item.errorMessage}</p>
                        ) : (
                          <pre className="max-h-56 overflow-auto rounded bg-gray-950 p-3 text-xs text-gray-100">
                            {prettyJson(item.outputJson) || "(no output)"}
                          </pre>
                        )}
                      </article>
                    ))}
                  </div>
                )}
              </div>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
