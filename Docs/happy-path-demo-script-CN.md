# PromptOps Studio Happy Path 演示脚本

这个脚本用于 3-5 分钟快速展示 PromptOps Studio 的端到端能力。当前 M5 版本已经支持 Dataset 上传、批量 Run、case 结果落库、trace 展示入口，以及 `run.requested` 事件边界；为了保证本地 demo 稳定，Platform API 现在采用同步执行 fallback，后续可以把同一个事件 payload 接到 Kafka Worker。

## 演示前准备

1. 启动 Docker Desktop。
2. 在三个终端分别启动服务：

```bash
cd platform-api && ./start.sh
cd ai-runtime && ./start.sh
cd frontend && ./start.sh
```

3. 打开 `http://localhost:3000`，使用 seed 账号登录：
   - username: `admin`
   - password: `password`

## 3 分钟讲解路径

1. 进入 Projects 页面，说明 Project 是隔离的 AI 应用测试空间。
2. 打开某个 Project 的 KB 页面，上传一段 Markdown 规则文档，再用检索调试区展示 hybrid search 结果。
3. 打开 Workflow 页面，创建 `RAG JSON Workflow`，说明它代表一条可追踪的 LangGraph 执行链路。
4. 打开 Runs 页面，上传示例 JSONL Dataset：

```jsonl
{"case_id":"case-1","input":"What is the refund window?"}
{"case_id":"case-2","input":"Which sources support the answer?"}
```

5. 点击 Start Run。Run 会经历 `QUEUED`、`RUNNING`、`SUCCESS/FAILED` 状态，并生成每条 case 的结果。
6. 选中最新 Run，看 case 列表和 output JSON。这里强调平台不只返回最终答案，还保存了每条 case 的状态、引用和节点 trace。

## 英文口播版本

PromptOps Studio is a full-stack AI evaluation platform. The user can upload knowledge documents, create a reusable RAG workflow, upload a JSONL dataset, and launch a batch run. The platform persists run metadata, case-level results, and execution traces so developers can inspect not only what the model answered, but also how each answer was produced.

In this milestone, I added the Dataset and Run layer. The Platform API now accepts JSONL datasets, creates batch runs, records case outputs and trace rows, and exposes a Runs UI for starting and inspecting executions. The Kafka event boundary is defined through the `run.requested` payload, with a synchronous fallback for a stable local demo; the next production step is to move the same payload into a Kafka consumer worker.

## 当前可诚实表达的边界

- 已完成：Dataset 上传、批量 Run、case 结果表、Run 状态、前端 Runs 页面、CI 基础门禁。
- 已完成：Kafka topic/config 和 `run.requested` 事件 payload 的代码边界。
- 待补强：真正的 Kafka producer/consumer worker、异步轮询体验、审计日志深度串联、Docker image build。
