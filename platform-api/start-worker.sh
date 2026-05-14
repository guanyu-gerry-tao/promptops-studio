#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== Platform API Run Worker (Kafka Consumer) ==="

echo "Starting dependent services (MySQL, Redis, Kafka)..."
docker compose -f ../docker-compose.yml up -d mysql redis kafka

echo "Waiting for MySQL to be ready..."
until docker exec promptops-mysql mysqladmin ping -h localhost -u root -ppassword --silent 2>/dev/null; do
  sleep 1
done
echo "MySQL is ready."

echo "Building and starting worker..."
PROMPTOPS_WORKER_ENABLED=true ./gradlew bootRun --args='--spring.main.web-application-type=none'
