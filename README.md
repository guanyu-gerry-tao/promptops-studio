# PromptOps Studio

An AI application testing and evaluation platform. Users can upload knowledge base documents, create AI workflows (e.g. RAG Q&A), prepare test datasets, and run batch evaluations. The system executes all test cases asynchronously and displays results, citations, node-level execution traces, and audit logs.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16 + React 19 + TypeScript + Tailwind CSS v4 |
| Platform API | Java 17 + Spring Boot 4 + Gradle |
| AI Runtime | Python 3.13 + FastAPI + LangChain + Poetry |
| Vector DB | Weaviate (hybrid search: vector + BM25 + Reranking) |
| Relational DB | MySQL 8 |
| Cache | Redis 7 |
| Message Queue | Kafka (async run execution) |
| DevOps | Docker + docker-compose |

## Project Structure

```
promptops-studio/
├── frontend/               # Next.js web UI
├── platform-api/           # Spring Boot REST API
├── ai-runtime/             # FastAPI + LangChain AI service
├── deploy/
│   └── init-db.sql         # MySQL schema + seed data
├── docker-compose.yml      # MySQL, Redis, Weaviate
└── Docs/
    └── plan-CN.md          # Full project plan and milestone tracker
```

## Prerequisites

Install these tools before starting:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Java 17+](https://adoptium.net/) (for platform-api)
- [Python 3.13](https://www.python.org/) via [pyenv](https://github.com/pyenv/pyenv) (for ai-runtime)
- [Poetry](https://python-poetry.org/docs/#installation) (for ai-runtime dependency management)
- [Node.js 20+](https://nodejs.org/) + npm (for frontend)

## Infrastructure Setup

Start all backing services (MySQL, Redis, Weaviate) with a single command:

```bash
docker compose up -d
```

Initialize the MySQL schema and seed data:

```bash
docker exec -i promptops-mysql mysql -u promptops -ppassword promptops < deploy/init-db.sql
```

Default test accounts created by seed data:

| Username | Password | Role |
|---|---|---|
| `admin` | `password` | Admin |
| `testuser` | `password` | User |

## Service Setup & Run

### Frontend (`frontend/`)

```bash
cd frontend
npm install
npm run dev
```

Runs at `http://localhost:3000`. API calls to `/api/*` are proxied to the Platform API at `http://localhost:8081`.

### Platform API (`platform-api/`)

No `.env` file needed — all config is in `src/main/resources/application.yml` with sensible defaults.

Optional environment variable overrides:

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | `PRODUCTION_MODE_DEFAULT_JWT_SECRET` | JWT signing secret (set a real value in production) |
| `AI_RUNTIME_URL` | `http://localhost:8000` | AI Runtime base URL |

```bash
cd platform-api
./gradlew bootRun
```

Runs at `http://localhost:8081`.

### AI Runtime (`ai-runtime/`)

**Step 1: Create the `.env` file**

Copy the example and fill in your real values:

```bash
cd ai-runtime
cp .env.example .env
```

Edit `.env`:

```env
# Required — app will refuse to start without this
OPENAI_API_KEY=sk-...

# Optional — set to true to enable Bedrock Cohere Reranking
RERANK_ENABLED=false
AWS_REGION=us-east-1
```

> **AWS Reranking note**: When `RERANK_ENABLED=true`, the service reads AWS credentials from `~/.aws/credentials` via boto3. No AWS keys go in `.env`. Make sure `aws configure` has been run on your machine.

**Step 2: Install dependencies**

```bash
cd ai-runtime
poetry install
```

> **Important**: Always run `poetry install` (not just `poetry run`) after cloning or after migrating from a different directory. The `.venv` contains hardcoded absolute paths — if the project folder is renamed or moved, the old `.venv` will be broken and must be regenerated:
> ```bash
> rm -rf .venv
> poetry install
> ```

**Step 3: Run the service**

```bash
cd ai-runtime
poetry run uvicorn ai_runtime.main:app --reload
```

Runs at `http://localhost:8000`. API docs available at `http://localhost:8000/docs`.

> **Shell environment note**: If you have a pyenv virtualenv active in your shell (e.g. the prompt shows `(ai-runtime-py3.13)`), Poetry may use that instead of `.venv`. Either `deactivate` first, or call pytest directly via `.venv/bin/pytest`.

## Running Tests

### Platform API (Java)

```bash
cd platform-api
./gradlew test
```

Tests run against a real MySQL instance (make sure `docker compose up -d` is running first).

### AI Runtime (Python)

```bash
cd ai-runtime
poetry run pytest
# or, if a pyenv virtualenv is active in your shell:
.venv/bin/pytest
```

All tests use mocks — no live OpenAI or Weaviate connection required.

## Key API Endpoints

### Platform API (`http://localhost:8081`)

| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/register` | Register a new user |
| `POST` | `/auth/login` | Login, returns JWT |
| `GET` | `/projects` | List projects |
| `POST` | `/projects` | Create project |
| `POST` | `/projects/{id}/kb/docs` | Upload KB document |
| `POST` | `/projects/{id}/kb/index` | Trigger indexing via AI Runtime |

### AI Runtime (`http://localhost:8000`)

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Health check |
| `POST` | `/index-document` | Chunk, embed, and store a document in Weaviate |
| `POST` | `/retrieve-document` | Hybrid search + optional rerank + LLM answer |

## Environment File Reference

Only `ai-runtime` uses a `.env` file. `platform-api` and `frontend` do not.

`ai-runtime/.env` (based on `.env.example`):

```env
# Required
OPENAI_API_KEY=

# Reranking (optional, default: false)
RERANK_ENABLED=false
AWS_REGION=us-east-1

# Weaviate (defaults match docker-compose)
WEAVIATE_HOST=localhost
WEAVIATE_PORT=8080

# Milvus — not in use (frozen, kept for rollback reference)
MILVUS_HOST=localhost
MILVUS_PORT=19530
```

## Common Issues

**`poetry run pytest` → `Command not found: pytest`**
You are likely running from the wrong directory or a pyenv virtualenv is interfering. See the [AI Runtime setup section](#ai-runtime-ai-runtime) above.

**`.venv/bin/pytest: bad interpreter: ... no such file or directory`**
The `.venv` was built in a different directory (e.g. after a worktree migration or folder rename). Regenerate it:
```bash
cd ai-runtime && rm -rf .venv && poetry install
```

**MySQL connection refused**
The `docker compose up -d` containers are not running. Check with `docker ps`.

**Weaviate connection error in AI Runtime**
Weaviate must be running (`docker compose up -d`). Confirm with `curl http://localhost:8080/v1/.well-known/ready`.
