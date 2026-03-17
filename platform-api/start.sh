#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== Platform API (Spring Boot) ==="

# Start dependent services (MySQL, Redis)
echo "Starting dependent services (MySQL, Redis)..."
docker compose -f ../docker-compose.yml up -d mysql redis

echo "Waiting for MySQL to be ready..."
until docker exec promptops-mysql mysqladmin ping -h localhost -u root -ppassword --silent 2>/dev/null; do
  sleep 1
done
echo "MySQL is ready."

echo "Building and starting..."
./gradlew bootRun
