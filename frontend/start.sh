#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== Frontend (Next.js) ==="
echo "Installing dependencies..."
npm install

echo "Starting dev server on http://localhost:3000 ..."
npm run dev
