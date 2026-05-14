# PromptOps Studio Resume Project Descriptions

下面是 10 套可复用的中英文项目介绍。写简历时不要一次全塞进去，可以按岗位重点挑 1-2 套组合：全栈岗位用 1/2/4，AI 平台岗位用 3/5/8，后端/平台岗位用 6/7/9。

## 1. Full-Stack AI Evaluation Platform

中文：设计并实现 PromptOps Studio，一个用于批量测试和评估 AI 应用的全栈平台，覆盖知识库上传、RAG Workflow、Dataset 批量执行、Run 状态追踪和节点级 trace 展示。

English: Designed and built PromptOps Studio, a full-stack platform for evaluating AI applications with knowledge base ingestion, RAG workflows, dataset-based batch runs, run status tracking, and node-level execution traces.

## 2. Monorepo System Design

中文：采用 monorepo 组织 Next.js 前端、Spring Boot Platform API 和 FastAPI AI Runtime，通过清晰的服务边界把 UI、业务元数据、AI 执行链路分层解耦。

English: Structured a monorepo with a Next.js frontend, Spring Boot Platform API, and FastAPI AI Runtime, separating UI, platform metadata, and AI execution responsibilities through clear service boundaries.

## 3. RAG and LangGraph Workflow

中文：实现基于 LangGraph 的 RAG Workflow，支持文档检索、结构化 JSON 生成、输出校验和 trace 记录，让每次模型执行都可观察、可复盘。

English: Implemented a LangGraph-based RAG workflow with document retrieval, structured JSON generation, output validation, and trace recording to make model executions observable and debuggable.

## 4. Dataset-Based Batch Evaluation

中文：新增 Dataset 和批量 Run 能力，支持 JSONL 测试集上传、case 级执行结果落库、成功/失败统计，并在前端提供 Runs 页面查看批量评估结果。

English: Added dataset-based batch evaluation with JSONL uploads, case-level result persistence, success/failure metrics, and a Runs UI for inspecting batch execution outcomes.

## 5. Hybrid Search Knowledge Base

中文：接入 Weaviate 作为知识库检索引擎，支持向量检索、BM25 关键词检索和可调 alpha 的 hybrid search，提升不同查询类型下的召回质量。

English: Integrated Weaviate as the knowledge base retrieval engine, supporting vector search, BM25 keyword search, and tunable alpha-based hybrid search for stronger retrieval quality across query types.

## 6. Spring Boot Platform API

中文：使用 Spring Boot 和 JPA 实现项目、知识库、workflow、dataset、run、trace 等核心平台对象的 REST API 与持久化模型。

English: Built Spring Boot and JPA-based REST APIs and persistence models for core platform objects including projects, knowledge documents, workflows, datasets, runs, and traces.

## 7. Run Orchestration Boundary

中文：定义 `run.requested` 事件 schema，并实现 Kafka producer/consumer worker，把批量 Run 从 API 请求线程中拆出，支持异步消费 dataset、逐条执行 workflow、回写结果。

English: Defined a `run.requested` event schema and implemented a Kafka producer/consumer worker to move batch runs out of the API request path, asynchronously consume datasets, execute workflows, and write back results.

## 8. AI Runtime Service

中文：构建 FastAPI AI Runtime，封装文档索引、检索、单 case workflow 执行和结构化响应，使 AI 能力以稳定 HTTP 接口暴露给平台层。

English: Built a FastAPI AI Runtime that exposes document indexing, retrieval, single-case workflow execution, and structured responses through stable HTTP interfaces for the platform layer.

## 9. Quality and CI

中文：为前端、后端和 AI Runtime 建立基础质量门禁，包含 ESLint、Next.js build、Gradle test 和 pytest，并通过 GitHub Actions 固化 CI 流程。

English: Established baseline quality gates across frontend, backend, and AI Runtime with ESLint, Next.js build, Gradle tests, pytest, and GitHub Actions CI.

## 10. Interview Pitch

中文：这是一个偏平台工程的 AI 项目，不只是调用大模型 API，而是把知识库、workflow、dataset、批量执行、trace 和 CI 串成一个可演示的工程系统。

English: This is an AI platform engineering project, not just a model API wrapper. It connects knowledge bases, workflows, datasets, batch execution, traces, and CI into a demonstrable engineering system.
