# 学习笔记 06：配置 Redis 并测试连接

**日期：** 2026-01-27
**目标：** 使用 Docker 运行 Redis 并测试连接

---

## Redis 是什么？

**Redis** = Remote Dictionary Server（远程字典服务器）

**特点：**
- 内存数据库（极快）
- 键值存储（Key-Value）
- 支持数据持久化
- 常用于缓存、会话、消息队列

---

## 添加到 docker-compose.yml

```yaml
services:
  redis:
    image: redis:7-alpine        # Alpine 版本（体积小）
    container_name: promptops-redis
    ports:
      - "6379:6379"              # Redis 默认端口
    volumes:
      - ./redis/data:/data       # 数据持久化
    command: redis-server --appendonly yes  # 启用 AOF
    environment:
      TZ: America/Los_Angeles
```

---

## 配置解读

| 配置项 | 含义 |
|--------|------|
| `redis:7-alpine` | Redis 7.x，基于 Alpine Linux |
| `6379:6379` | 端口映射：本机 → 容器 |
| `./redis/data:/data` | 数据持久化到本地 |
| `--appendonly yes` | 开启 AOF 持久化 |

**持久化方式：**
- **RDB**：定期快照（默认）
- **AOF**：记录每个操作（更安全）

---

## 启动 Redis

```bash
# 启动 Redis
docker-compose up -d redis

# 查看状态
docker ps

# 查看日志
docker-compose logs -f redis
```

---

## 测试连接

### 方法 1：PING 测试

```bash
docker exec promptops-redis redis-cli ping
```

**输出：**
```
PONG  ← 表示连接成功
```

---

### 方法 2：进入 Redis CLI

```bash
# 进入 Redis 命令行
docker exec -it promptops-redis redis-cli

# 测试命令
127.0.0.1:6379> PING
PONG
127.0.0.1:6379> SET test "Hello Redis"
OK
127.0.0.1:6379> GET test
"Hello Redis"
127.0.0.1:6379> EXIT
```

---

### 方法 3：从主机连接

```bash
# 需要安装 redis-cli
brew install redis  # macOS

# 连接
redis-cli -h localhost -p 6379

# 测试
127.0.0.1:6379> PING
PONG
```

---

## 常用 Redis 命令

### 字符串操作
```bash
SET key value      # 设置值
GET key            # 获取值
DEL key            # 删除键
EXISTS key         # 检查是否存在
```

### 过期时间
```bash
EXPIRE key 60      # 60 秒后过期
TTL key            # 查看剩余时间（-1=永不过期，-2=不存在）
```

### 查看键
```bash
KEYS *             # 查看所有键（生产环境慎用）
KEYS test*         # 查看 test 开头的键
```

### 数据类型
```bash
# Hash（哈希表）
HSET user:1 name "John"
HGET user:1 name

# List（列表）
LPUSH mylist "item1"
LRANGE mylist 0 -1

# Set（集合）
SADD myset "value1"
SMEMBERS myset
```

---

## 在 Python 中使用

### 安装依赖

```bash
poetry add redis
```

### 代码示例

```python
import redis

# 连接 Redis
r = redis.Redis(host='localhost', port=6379, decode_responses=True)

# 写入
r.set('name', 'John')

# 读取
name = r.get('name')
print(name)  # John

# 设置过期时间
r.setex('session', 60, 'abc123')  # 60 秒后过期

# 检查是否存在
exists = r.exists('name')
print(exists)  # 1 (True)
```

---

## 数据持久化验证

```bash
# 查看数据目录
ls -la ./redis/data/appendonlydir/

# 输出
appendonly.aof.1.base.rdb    # RDB 快照
appendonly.aof.1.incr.aof    # AOF 增量日志
appendonly.aof.manifest      # 清单文件
```

**测试持久化：**
```bash
# 1. 写入数据
docker exec promptops-redis redis-cli SET persist_test "data"

# 2. 重启容器
docker-compose restart redis

# 3. 验证数据还在
docker exec promptops-redis redis-cli GET persist_test
# 输出：data ← 数据持久化成功
```

---

## 常见问题

**Q: 端口被占用？**
```bash
# 修改端口
ports:
  - "6380:6379"  # 本机用 6380
```

**Q: 连接失败？**
```bash
# 检查容器是否运行
docker ps | grep redis

# 检查日志
docker logs promptops-redis
```

**Q: 清空所有数据？**
```bash
docker exec promptops-redis redis-cli FLUSHALL
```

---

## 性能监控

```bash
# 查看信息
docker exec promptops-redis redis-cli INFO

# 查看内存使用
docker exec promptops-redis redis-cli INFO memory

# 实时监控
docker exec promptops-redis redis-cli MONITOR
```

---

## 参考

- [Redis 官方文档](https://redis.io/docs/)
- [Redis 命令参考](https://redis.io/commands/)
- [redis-py 文档](https://redis-py.readthedocs.io/)
