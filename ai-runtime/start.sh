#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== AI Runtime (FastAPI) ==="

# Start dependent services (Weaviate)
echo "Starting dependent services (Weaviate)..."
docker compose -f ../docker-compose.yml up -d weaviate

echo "Waiting for Weaviate to be ready..."
until curl -sf http://localhost:8080/v1/.well-known/ready >/dev/null 2>&1; do
  sleep 1
done
echo "Weaviate is ready."

echo "Installing dependencies..."
poetry install

echo "Starting dev server on http://localhost:8000 ..."
poetry run uvicorn ai_runtime.main:app --host 0.0.0.0 --port 8000 --reload
