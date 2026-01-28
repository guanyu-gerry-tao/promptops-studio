# AI Runtime Service

AI Runtime Service with FastAPI and LangChain.

## Setup

```bash
# Install dependencies
poetry install

# Run server
poetry run uvicorn ai_runtime.main:app --reload
```

## API Endpoints

- `GET /` - Root endpoint
- `GET /health` - Health check
- `GET /docs` - Swagger UI documentation
