# PromptOps Studio 全栈技术栈练习版MVP项目计划

## **0. 目标与范围**

目标：根据[埃林哲技术路线](./埃林哲技术路线(1).pdf)以及[埃豆AI平台](./埃豆AI-企业级智能应用构建平台.pdf)介绍，模仿做一个“平台能力演示型”MVP，用自定义场景覆盖并练熟以下技术栈：前端 React + TypeScript + Ice + Less，平台后端 Java + Spring Boot/Spring Cloud + Gradle + Nacos + 云梯后端架构，AI 侧 Python + FastAPI + LangChain/LangGraph + Azure OpenAI + Uvicorn + Poetry，数据与中间件 MySQL、Redis、Milvus、Kafka，以及 DevOps 与基础设施 Docker/docker-compose、Kubernetes、Helm、云梯平台、CI/CD（GitHub Actions）。

非目标：不追求行业业务闭环、不对接真实 ERP/CRM、不做复杂多租户与安全合规全套。重点是“跑通、可演示、可追溯、可扩展”。

MVP 定义：用户能在 Web 平台上创建一个 Workflow（基于模板）、上传知识文档建立 KB、上传测试集 Dataset，触发一次 Run（批量执行），系统异步执行（Kafka），并在 UI 查看每条 case 的结果、引用、节点级 trace、审计日志与简单指标。

---

## **1. 产品形态（你要展示的“平台能力”）**

### **1.1 核心概念（平台对象模型）**

- Project：项目空间（隔离资源与数据）
- KnowledgeBase（KB）：文档集合 + 向量索引（Milvus）
- Workflow：LangGraph 工作流定义（模板化 + 参数化）
- Dataset：测试集（JSONL）
- Run：一次批量执行（异步队列）
- Trace：节点级执行轨迹（可追溯）
- AuditLog：审计日志（谁在何时对什么做了什么）
### **1.2 UI 页面（MVP 必备）**

1. 登录/注册
2. Project 列表/创建
3. KnowledgeBase：上传文档、索引状态、检索调试
4. Workflow：从模板创建、查看节点结构与参数
5. Dataset：上传 JSONL、预览样本
6. Runs：发起 Run、查看状态（`QUEUED/RUNNING/SUCCESS/FAILED`）
7. Run Detail：每条 case 的输出 + citations + 节点 trace
8. Audit Log：按 runId/sessionId 查询审计记录

---

## **2. 总体架构（最贴“企业平台”的最小拆分）**

### **2.1 技术栈总览**

- 前端
    - React + TypeScript：构建 Web UI，与后端 API 交互
    - Ice：基于 React 的前端工程方案，提供物料和脚手架，加速中后台页面开发
    - Less：CSS 预处理语言，支持变量、嵌套等特性，提高样式可维护性
    - HTML CSS JS：基础 Web 技术栈
- 平台后端
    - Java + Spring Boot / Spring Cloud：实现用户、项目、Workflow、Run 等业务接口和服务治理
    - Gradle：Java 构建工具，用于依赖管理、编译、测试与打包
    - Nacos：服务注册与配置中心，用于服务发现、配置管理
    - 云梯后端架构：公司内部微服务脚手架与规范，封装监控、日志、权限等基础能力
- AI 服务与大模型
    - Python + FastAPI：提供 AI 推理与工作流编排 HTTP 接口
    - LangGraph：基于 LangChain 的工作流编排框架，用于构建可追踪的多节点 LLM 流程
    - LangChain：大模型应用开发框架，提供 Prompt、工具调用、RAG 等抽象
    - Azure OpenAI：托管大模型服务，提供 GPT 等模型，并集成企业级网络与权限控制
    - Uvicorn：异步 Python ASGI 服务器，用于部署 FastAPI 服务
    - Poetry：Python 依赖与虚拟环境管理工具，保证 AI 侧依赖可重复安装
- 数据与存储
    - MySQL：关系型数据库，存储平台元数据、运行结果、审计与 trace
    - Redis：内存 KV 存储，用于会话缓存、run 状态缓存、幂等控制
    - Milvus：向量数据库，用于知识库向量检索（KB 检索）
- 中间件与消息队列
    - Kafka：分布式消息队列，实现 Run 异步执行与解耦
- DevOps 与基础设施
    - Docker / `docker-compose`：本地与测试环境容器化与一键启动
    - Kubernetes（k8s）：容器编排平台，负责服务部署、伸缩和滚动升级（可作为后续演进目标）
    - Helm：Kubernetes 包管理工具，用 Chart 管理整套服务的部署（可作为后续演进目标）
    - 云梯平台：公司内部云平台，承载服务部署、监控与运维（可选对接）

### **2.2 服务组件架构**

- Frontend：负责 Web UI 展示与交互，调用 Platform API
- Platform API：对外提供项目、Workflow、Dataset、Run 等 REST API，并统一落库到 MySQL
- AI Runtime：负责知识库索引、检索与 Workflow 执行，对接向量库 Milvus 与大模型
- Worker：消费 `run.requested` 消息，按 dataset 逐条执行 Workflow 并写回结果
- MySQL：作为平台的关系型主数据存储
- Redis：作为缓存与幂等控制组件，减少数据库压力
- Milvus：作为 KB 的向量检索引擎
- Kafka：作为 Run 异步编排的消息总线

### **2.3 数据流（端到端）**

1. UI 发起 Run
2. Spring 创建 Run 记录（QUEUED），写入 MySQL
3. Spring 发 Kafka 消息 `run.requested`（包含 `runId`、`workflowId`、`datasetId`、`projectId`）
4. Python Worker 消费消息：逐条读取 dataset，调用 LangGraph 执行（含 Milvus 检索、工具调用、schema 校验）
5. Worker 把每条 case 的结果与 trace 回写到 Spring（HTTP callback）或直接写 DB（建议回调，便于权限与审计统一）
6. Spring 更新 Run 状态为 `SUCCESS/FAILED`，UI 轮询/刷新展示

---

## **3. 仓库结构与工程组织**

推荐 monorepo（便于一次性交付与 Docker Compose 一键起）：
- `frontend/`：React
- `platform-api/`：Spring Boot
- `ai-runtime/`：FastAPI + LangGraph + Milvus client + Kafka consumer（可拆 worker/）
- `deploy/`：`docker-compose`、初始化脚本、示例数据
- `.github/workflows/`：CI/CD

---

## **4. 数据与假数据准备（保证“像平台”）**

### **4.1 知识库文档（10 份 Markdown/PDF）**

主题建议：输出规范与规则库（完全自造也合理）

- `Output JSON Spec.md`（输出字段、schema 示例）
- `Safety Rules.md`（禁止输出敏感信息、PII masking 示例）
- `Style Guide.md`（固定写作风格规则）
- `Tool Usage Guide.md`（工具使用说明）
- `FAQ.md`（10–20 条问答）
- `Bad Examples.md`（错误示例与纠正方式）
- `Evaluation Rubric.md`（评分规则）

### **4.2 Dataset（20–50 条 JSONL）**

每条包含：

- `case_id`
- `input`：用户请求
- `expected_schema_id`：固定一个 schema
- `tags`：extract/summarize/classify/transform 等

### **4.3 Workflow 模板（至少 2 个）**

- Template A：RAG + JSON 输出（最典型）
- Template B：Tool-heavy（工具调用 + 校验 + 输出）

---

## **5. Workflow 设计（LangGraph 模板化）**

### **5.1 State（统一状态结构）**

- `project_id`, `run_id`, `case_id`
- `user_input`
- `retrieval_query`
- `retrieved_chunks[]`（含 `doc_id`、`chunk_id`、`text`、`score`）
- `draft_output_json`
- `validation_result`（pass/fail + reasons）
- `final_output_json`
- `trace[]`（节点级记录：`node_name`、`in`/`out`、`latency`、`token`、`citations`）

### **5.2 节点（最小 5–6 个）**

1. `route_intent`：决定是否需要检索（规则/模型）
2. `retrieve_kb`：Milvus 召回 topK
3. `generate_json`：LLM 输出结构化 JSON
4. `validate_schema`：JSON schema 校验（失败则进入修复）
5. `repair_json`：根据错误提示修复输出（最多 N 次）
6. `finalize`：写入 trace、形成最终结果

### **5.3 工具（Tool Registry）**

- `kb_search(query, top_k)`：向量检索
- `schema_validate(json, schema_id)`：校验并返回错误列表
  -（可选）`mock_http_get(url)`：模拟外部系统调用

---

## **6. Kafka 设计（MVP 只做“Run 异步执行”）**

### **6.1 Topics**

- `run.requested`：Spring -> Worker（必须）
  -（可选）`run.dlq`：失败消息（建议加，企业味道强）

### **6.2 Message Schema（JSON）**

- `event_id`（uuid）
- `run_id`
- `project_id`
- `workflow_id`
- `dataset_id`
- `requested_by`
- `created_at`

### **6.3 幂等策略（必须实现其一）**

- 处理前查 MySQL：若 `run.status` 已是 `SUCCESS/FAILED`，则跳过
- 或 Redis 去重：`SETNX` `event_id` 成功才处理

### **6.4 错误与重试（MVP 简化）**

- 执行失败：写 run `FAILED` + `error_message`
  -（可选）发送到 `run.dlq`

---

## **7. API 设计（可直接按此实现）**

### **7.1 Platform API（Spring）**

Auth
- `POST /auth/register`
- `POST /auth/login`

Project
- `POST /projects`
- `GET /projects`

KnowledgeBase
- `POST /projects/{id}/kb/docs`（上传文档元数据与文件）
- `POST /projects/{id}/kb/index`（触发索引：调用 ai-runtime /index）
- `GET /projects/{id}/kb/search?q=...`（调试检索）

Workflow
- `POST /projects/{id}/workflows`（from template）
- `GET /projects/{id}/workflows`
- `GET /workflows/{workflowId}`

Dataset
- `POST /projects/{id}/datasets`（上传 JSONL）
- `GET /projects/{id}/datasets`
- `GET /datasets/{datasetId}`

Run
- `POST /runs`（body: projectId, workflowId, datasetId）→ 创建 run + 发 Kafka
- `GET /runs?projectId=...`
- `GET /runs/{runId}`
- `GET /runs/{runId}/cases`（分页）
- `GET /runs/{runId}/case/{caseId}`（含 trace/citations）

Audit
- `GET /audit?runId=...`

Callback（AI runtime 回写）
- `POST /internal/runs/{runId}/case-result`（写入 case 结果与 trace）
- `POST /internal/runs/{runId}/status`（更新 run 状态）

### **7.2 AI Runtime（FastAPI）**
- `POST /index`（projectId + docs/chunks）
- `POST /retrieve`（projectId + query）
- `POST /execute-case`（workflowId + projectId + case input）→ 返回 output + trace
- Worker 消费 `run.requested`：批量调用 execute-case 并回写 platform

---

## **8. 数据库设计（MySQL，MVP 必要表）**

最小表集合：
- `users`：账号、hash、role
- `projects`：项目
- `kb_docs`：文档元数据（`project_id`、`doc_id`、`title`、`visibility`、`status`）
- `workflows`：workflow 定义（json）
- `datasets`：dataset 元数据
- `dataset_items`：每条 case（`input`、`tags`）
- `runs`：run 元数据（`status`、`started_at`、`ended_at`、`error`）
- `run_cases`：每条 case 结果（`output_json`、`status`、`citations_summary`）
- `run_traces`：节点级 trace（`run_id`、`case_id`、`node_name`、`in_json`、`out_json`、`latency_ms`、`token`、`citations`）
- `audit_logs`：审计（`actor`、`action`、`resource`、`ts`、`metadata_json`）

---

## **9. Docker Compose 与本地一键启动**

`docker-compose` 至少包含：
- `mysql`
- `redis`
- `milvus`（standalone）
- `kafka` + `zookeeper`（或 `kraft` 单节点）
- `platform-api`
- `ai-runtime`
- `frontend`

启动后演示路径：

1. 登录
2. 创建 project
3. 上传 docs → index
4. 创建 workflow（模板）
5. 上传 dataset
6. `Start Run`
7. 查看 run 状态与 case trace
8. 审计日志回放

---

## **10. CI/CD（最小但标准）**

GitHub Actions：
- 前端：lint + build
- Spring：test + build jar + docker build
- FastAPI：pytest + docker build
  -（可选）push 镜像到 GHCR

---

## **11. 里程碑计划（2 个月交付节奏）**

整体节奏：按 2 个月（约 8 周）规划，优先保证每个阶段都有“可演示”的成果，其次再逐步加深技术栈（LangGraph、Milvus、Kafka、K8s/Helm 等）。

### **Milestone 1：基础环境与脚手架（第 1–2 周）**

- 本地开发环境与仓库
    - 初始化 monorepo 结构：`frontend/`、`platform-api/`、`ai-runtime/`、`deploy/`
    - 配置 Gradle 基础构建（Spring Boot）与 Poetry/FastAPI 基础依赖
    - `docker-compose` 起 `mysql` / `redis` / `kafka` / `milvus`（如遇困难可先起前 2–3 个）
- 基础应用骨架
    - Spring Boot 起一个基础 API（健康检查 + 简单 `GET /hello`）
    - React 起一个最小页面，能调用 Platform API 并显示结果

验收：
- 一条命令启动基础依赖与三个服务骨架（Frontend、Platform API、AI Runtime）
- 浏览器可访问前端页面，并通过 Platform API 返回一段测试数据

### **Milestone 2：平台主体 & 账号项目管理（第 3–4 周）**

- 平台后端（Platform API）
    - 使用 Spring Boot + Gradle 实现基础 Domain：`users`、`projects`
    - 实现最简登录/注册（可先用内存/简单加密），Project CRUD，落库到 MySQL
    - 设计并实现基础实体表结构（users、projects、audit_logs 雏形）
- 前端页面
    - 登录页 + Project 列表/创建页
    - 对接后端登录与 Project CRUD API
- 配置与服务治理（可先文档+基础实践）
    - 了解 Nacos / 云梯后端架构的用法，形成“未来接入方案”文档

验收：
- 用户可以注册 / 登录，并在 UI 中创建、查看自己的 Project
- MySQL 中能看到用户和项目数据，基础审计日志表结构已就绪

### **Milestone 3：AI Runtime 与知识库闭环（第 5–6 周）**

- AI Runtime 能力
    - 使用 FastAPI + Uvicorn 提供 `POST /index` 与 `POST /retrieve` 接口
    - 接入 Milvus：完成文档分段、embedding 写入、向量检索（可先用开源 embedding 模型）
    - 集成 LangChain + Azure OpenAI（或占位 LLM 接口），返回简单回答 + citations
- 数据与 Platform API 对接
    - Platform API 支持上传 KB 文档元数据，并调用 AI Runtime 完成索引
    - 设计并落库 `kb_docs` 表，与 projects 关联
- 前端支持
    - 知识库页面：上传 docs、查看索引状态、简单检索调试

验收：
- 从前端上传一份 Markdown 文档后，可以在页面输入 query，看到 Milvus 检索出的片段与来源
- 日志中可以追踪一次完整的“上传 doc → 索引 → 检索”的链路

### **Milestone 4：Workflow & LangGraph Trace（第 7 周）**

- Workflow 单条执行链路
    - 使用 LangGraph/LangChain 实现一个单 case Workflow（Template A：RAG + JSON 输出）
    - 实现统一 State 结构（`project_id`、`run_id`、`case_id`、`retrieved_chunks[]`、`final_output_json`、`trace[]` 等）
    - 返回节点级 trace（包含 node 名称、输入输出摘要、latency 等）
- Platform API 集成
    - 增加 `workflows`、`runs` 的基本表结构与 CRUD（先支持单条 case）
    - 提供 `POST /execute-case` 封装单条 Workflow 调用
- 前端展示
    - 简单 Workflow 配置 / 详情页
    - Run Detail 页面展示单条 case 的 output + trace

验收：
- 在 UI 中选定 Project + Workflow，输入一个 case，能得到结构化 JSON 输出和节点 trace
- 数据库中能查到相应的 run、run_traces 记录

### **Milestone 5：Kafka 异步 Run & Dataset（第 8 周）**

- 异步 Run 编排
    - 定义 `run.requested` 事件 schema，并由 Platform API 发送到 Kafka
    - 实现 Worker（Python Kafka Consumer），消费 dataset，逐条调用 Workflow
    - Worker 回写结果与状态到 Platform API（`/internal/runs/...`）或直接写 DB
- Dataset 与批量 Run
    - 支持 Dataset 元数据与条目（`datasets`、`dataset_items`）的上传与落库
    - Run 列表与详情页：可以看到 Run 从 `QUEUED` → `RUNNING` → `SUCCESS/FAILED`，以及 case 列表
- 基础审计与 CI/CD
    - 审计日志：记录关键操作（登录、创建项目、发起 Run 等）
    - GitHub Actions：前端 lint + build，后端/AI Runtime 测试 + Docker build

验收：
- 从 UI 上传一个 dataset，发起一个 Run，能看到 Kafka 驱动的异步执行流程；Run 状态随执行推进更新
- 能通过简单的审计与指标（例如 Run 数量、成功率）复盘一次执行

---

## **12. 风险与降级策略（确保按时交付）**

高风险点与替代方案：

- Kafka 调不通：先用 Celery/Redis Queue 或 Spring 定时拉取（但保留 Kafka 设计文档），确保演示不断档
- Milvus 配置复杂：先用本地向量库（FAISS/Chroma）跑通流程，再切回 Milvus
- LangGraph 上手慢：先固定链路实现，再替换为 LangGraph（保持接口不变）

交付优先级（从高到低）：

1. Run 异步闭环（Kafka）+ trace 展示
2. Milvus 检索 + citations
3. Workflow 模板化（LangGraph）
4. 权限/审计（最简）
5. 评测指标与 UI 打磨

---

## **13. 你最终交付物清单（对公司最“可展示”的东西）**

- 一张架构图（服务、数据流、Kafka 事件流）
- 可运行 Demo（docker-compose 一键启动）
- 2 个 workflow 模板
- 10 份假文档 + 1 份 dataset（JSONL）
- Run 页面：状态、结果、trace、审计
- README：启动方式、演示脚本（3 分钟）