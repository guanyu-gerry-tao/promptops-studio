# PromptOps Studio Kubernetes 部署指南

这份指南用于 M6 的本地 k8s 演示：把已经跑通的 `frontend`、`platform-api`、`platform-worker`、`ai-runtime` 和 demo 依赖服务部署到一个 kind/minikube 集群里。

这里的重点是作品集展示和学习平台工程路径：容器化、Deployment、Service、ConfigMap、Secret、Ingress、API/Worker 分离和 Kafka 异步流转。MySQL、Redis、Kafka、Weaviate 在本目录里是 demo 级部署，使用 `emptyDir` 临时存储，不代表生产级配置。

## 1. 构建本地镜像

在仓库根目录执行：

```bash
docker build -t promptops-frontend:latest ./frontend
docker build -t promptops-platform-api:latest ./platform-api
docker build -t promptops-ai-runtime:latest ./ai-runtime
```

如果你的 Platform API service 名称不是 `platform-api`，构建前端镜像时要覆盖 build arg：

```bash
docker build --build-arg PLATFORM_API_URL=http://platform-api:8081 -t promptops-frontend:latest ./frontend
```

`platform-api` 和 `platform-worker` 使用同一个镜像：API 进程正常启动 Web 服务，Worker 进程通过 k8s args 加 `--spring.main.web-application-type=none`，并设置 `PROMPTOPS_WORKER_ENABLED=true`。

## 2. 准备本地集群

kind 示例：

```bash
kind create cluster --name promptops
```

如果使用 kind，需要把本地镜像导入集群：

```bash
kind load docker-image promptops-frontend:latest --name promptops
kind load docker-image promptops-platform-api:latest --name promptops
kind load docker-image promptops-ai-runtime:latest --name promptops
```

minikube 示例：

```bash
minikube start
minikube image load promptops-frontend:latest
minikube image load promptops-platform-api:latest
minikube image load promptops-ai-runtime:latest
```

## 3. 部署 k8s manifests

```bash
kubectl apply -f deploy/k8s/
```

查看资源状态：

```bash
kubectl get pods -n promptops
kubectl get svc -n promptops
kubectl get ingress -n promptops
```

等待核心 Pod 进入 `Running`：

```bash
kubectl wait --for=condition=available deployment/frontend -n promptops --timeout=180s
kubectl wait --for=condition=available deployment/platform-api -n promptops --timeout=180s
kubectl wait --for=condition=available deployment/platform-worker -n promptops --timeout=180s
kubectl wait --for=condition=available deployment/ai-runtime -n promptops --timeout=180s
```

## 4. 访问前端

最稳定的本地方式是 port-forward：

```bash
kubectl port-forward -n promptops svc/frontend 3000:3000
```

然后打开：

```text
http://localhost:3000
```

Ingress 清单使用 `promptops.local` 作为演示 host。如果你已启用 ingress controller，可以把下面这行加到本机 hosts：

```text
127.0.0.1 promptops.local
```

## 5. Happy Path 演示

1. 打开前端，注册或登录用户。
2. 创建 Project。
3. 上传 KB 文档，触发 AI Runtime 索引。
4. 创建 `RAG JSON Workflow`。
5. 在 Runs 页面上传 JSONL dataset：

```jsonl
{"case_id":"case-1","input":"What is the refund window?"}
{"case_id":"case-2","input":"Which sources support the answer?"}
```

6. 点击 Start Run。
7. Platform API 创建 `QUEUED` Run，并发 Kafka `run.requested`。
8. `platform-worker` 消费事件，逐条调用 AI Runtime `/execute-case`，写入 `run_cases` / `run_traces`。
9. 在 Runs 页面点击 Refresh，查看 Run 状态和 case 输出。

## 6. 调试命令

查看 API 日志：

```bash
kubectl logs -n promptops deployment/platform-api
```

查看 Worker 日志：

```bash
kubectl logs -n promptops deployment/platform-worker
```

查看 AI Runtime 日志：

```bash
kubectl logs -n promptops deployment/ai-runtime
```

进入 MySQL：

```bash
kubectl exec -it -n promptops deployment/mysql -- mysql -u promptops -ppassword promptops
```

## 7. 当前边界

- 当前 k8s 依赖服务使用 `emptyDir`，Pod 重启后数据可能丢失。
- `deploy/k8s/11-secret.example.yaml` 只适合本地 demo，真实部署要用集群 Secret 管理系统。
- GitHub Actions 目前做镜像 build 和 manifest dry-run，不负责真正部署到集群。
- 下一步生产化可以改成 Helm Chart、StatefulSet/PVC、GHCR 镜像推送和环境分层 values。
