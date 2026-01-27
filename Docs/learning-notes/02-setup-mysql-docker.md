# 学习笔记 02：使用 Docker 启动 MySQL

**日期：** 2026-01-27
**目标：** 学会用 Docker 运行 MySQL 数据库

---

## 背景知识

### 为什么用 Docker？

**传统方式：**
```
下载 MySQL 安装包 → 安装 → 配置 → 占用系统空间
```
- 麻烦，不同系统步骤不同
- 卸载不干净
- 多个项目需要不同版本时很头疼

**Docker 方式：**
```
docker-compose up → MySQL 启动完成
```
- 一条命令启动
- 删除容器，数据还在（通过 volume）
- 不污染系统环境

---

### Docker 核心概念

| 概念 | 解释 | 类比 |
|------|------|------|
| **镜像 (Image)** | 软件安装包 | ISO 光盘 |
| **容器 (Container)** | 运行中的镜像实例 | 虚拟机 |
| **Volume** | 数据持久化 | 外接硬盘 |
| **端口映射** | 容器端口 → 电脑端口 | 门牌号映射 |
| **docker-compose** | 管理多个容器的工具 | 批处理脚本 |

---

## 准备工作

### 1. 安装 Docker Desktop

**macOS：**
1. 下载：https://www.docker.com/products/docker-desktop
2. 拖到 Applications
3. 启动 Docker Desktop（等状态栏鲸鱼图标稳定）

**验证安装：**
```bash
docker --version
# 输出：Docker version 24.x.x
```

---

## 方法 A：使用 docker-compose（推荐）

### 步骤 1：创建 docker-compose.yml

在项目根目录创建文件：

```yaml
services:
  mysql:
    image: mysql:8.0              # 使用 MySQL 8.0 镜像
    container_name: promptops-mysql
    ports:
      - "3306:3306"               # 端口映射：本机3306 → 容器3306
    volumes:
      - ./mysql/data:/var/lib/mysql  # 数据持久化
    environment:
      MYSQL_ROOT_PASSWORD: password  # root 密码
      MYSQL_DATABASE: promptops      # 自动创建数据库
      MYSQL_USER: promptops          # 创建用户
      MYSQL_PASSWORD: password       # 用户密码
      TZ: America/Los_Angeles        # 时区
    command:
      - --character-set-server=utf8mb4      # 字符集（支持中文）
      - --collation-server=utf8mb4_unicode_ci
```

---

### 步骤 2：逐行解读配置

#### `image: mysql:8.0`
- 使用的 Docker 镜像名称和版本
- `mysql` = 镜像名，`8.0` = 版本号
- 如果本地没有，Docker 会自动从 Docker Hub 下载

#### `container_name: promptops-mysql`
- 容器的名字（自定义）
- 方便用 `docker ps` 查看时识别

#### `ports: - "3306:3306"`
- 端口映射格式：`"宿主机端口:容器端口"`
- `3306` 是 MySQL 的默认端口
- 意思：访问你电脑的 3306 端口 = 访问容器的 3306 端口

#### `volumes: - ./mysql/data:/var/lib/mysql`
- 数据持久化，格式：`"宿主机路径:容器路径"`
- `./mysql/data` - 你电脑上的目录（相对路径）
- `/var/lib/mysql` - 容器内 MySQL 存放数据的位置
- **关键作用：** 删除容器后数据还在

#### `environment` - 环境变量
- `MYSQL_ROOT_PASSWORD` - root 用户密码（必须设置）
- `MYSQL_DATABASE` - 自动创建的数据库名
- `MYSQL_USER` 和 `MYSQL_PASSWORD` - 自动创建的普通用户
- `TZ` - 时区设置（不设置会比你当地时间差 8 小时）

#### `command` - 启动参数
- `--character-set-server=utf8mb4` - 字符集（支持 emoji 和中文）
- `--collation-server=utf8mb4_unicode_ci` - 排序规则

---

### 步骤 3：启动 MySQL

```bash
# 启动（后台运行）
docker-compose up -d mysql

# 查看日志
docker-compose logs -f mysql

# 查看运行状态
docker ps
```

**命令解释：**
- `up` - 启动服务
- `-d` - detached，后台运行（不占用终端）
- `logs -f` - 查看日志，`-f` 表示持续跟踪

---

### 步骤 4：验证 MySQL 是否启动

#### 方法 1：检查容器状态

```bash
docker ps
```

**预期输出：**
```
CONTAINER ID   IMAGE       STATUS         PORTS                    NAMES
abc123def456   mysql:8.0   Up 2 minutes   0.0.0.0:3306->3306/tcp   promptops-mysql
```

- `STATUS: Up` - 运行中 ✅
- `PORTS: 0.0.0.0:3306->3306/tcp` - 端口映射成功 ✅

#### 方法 2：测试连接

```bash
# 使用 docker exec 进入容器
docker exec -it promptops-mysql mysql -u promptops -ppassword

# 看到 mysql> 提示符说明成功
mysql> SHOW DATABASES;
```

**输出：**
```
+--------------------+
| Database           |
+--------------------+
| promptops          |  ← 我们创建的数据库
| information_schema |
| performance_schema |
+--------------------+
```

---

## 方法 B：直接用 docker run（不推荐）

```bash
docker run -d \
  --name promptops-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=promptops \
  -e MYSQL_USER=promptops \
  -e MYSQL_PASSWORD=password \
  -v ./mysql/data:/var/lib/mysql \
  mysql:8.0
```

**缺点：** 命令太长，不方便管理

---

## 连接 MySQL 的方式

### 1. 命令行客户端

```bash
# 使用 docker exec
docker exec -it promptops-mysql mysql -u promptops -ppassword

# 使用本机 mysql 客户端（如果安装了）
mysql -h 127.0.0.1 -P 3306 -u promptops -ppassword
```

### 2. GUI 工具

**推荐工具：**
- **DBeaver**（免费，跨平台）
- **MySQL Workbench**（MySQL 官方）
- **TablePlus**（macOS，收费但好用）
- **DataGrip**（JetBrains，付费）

**连接信息：**
```
Host: localhost (或 127.0.0.1)
Port: 3306
Username: promptops
Password: password
Database: promptops
```

### 3. 在代码中连接（Spring Boot）

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/promptops
    username: promptops
    password: password
```

---

## 常用 Docker 命令

### 容器管理

```bash
# 启动容器
docker-compose up -d mysql

# 停止容器
docker-compose stop mysql

# 删除容器（数据在 volume 中，不会丢失）
docker-compose down

# 查看运行中的容器
docker ps

# 查看所有容器（包括停止的）
docker ps -a

# 查看容器日志
docker-compose logs -f mysql
```

### 进入容器

```bash
# 进入 MySQL 命令行
docker exec -it promptops-mysql mysql -u root -ppassword

# 进入容器的 bash
docker exec -it promptops-mysql bash
```

### 清理

```bash
# 停止并删除容器
docker-compose down

# 删除容器 + 数据卷（⚠️ 会删除数据！）
docker-compose down -v

# 清理无用镜像
docker image prune
```

---

## 常见问题

### Q1: 启动失败，提示端口被占用

**错误：**
```
bind: address already in use
```

**原因：** 3306 端口已被占用

**解决方法 A：** 更改端口映射
```yaml
ports:
  - "3307:3306"  # 本机用 3307，容器内还是 3306
```

**解决方法 B：** 找到并关闭占用端口的程序
```bash
lsof -i :3306
kill -9 <PID>
```

---

### Q2: 连接被拒绝

**错误：**
```
Can't connect to MySQL server on 'localhost'
```

**检查清单：**
1. 容器是否运行？`docker ps`
2. 端口映射是否正确？看 `docker ps` 的 PORTS 列
3. 用户名密码是否正确？
4. 主机名是否正确？（用 `localhost` 或 `127.0.0.1`，不要用 `mysql`）

---

### Q3: 数据丢失了

**原因：** 可能用了 `docker-compose down -v` 删除了 volume

**预防方法：**
- 正常停止用 `docker-compose stop`
- 删除容器不删除数据：`docker-compose down`（不加 `-v`）
- 定期备份数据

---

### Q4: 镜像下载很慢

**原因：** Docker Hub 在国外

**解决方法：** 配置国内镜像源（阿里云、DaoCloud）

在 Docker Desktop → Settings → Docker Engine 添加：
```json
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com"
  ]
}
```

---

## Volume 数据位置

### 查看数据存储位置

```bash
ls -la ./mysql/data/
```

**内容：**
```
promptops/          ← 你的数据库
mysql/              ← 系统数据库
ib_logfile0         ← 日志文件
```

**备份数据：**
```bash
# 简单方法：复制整个 data 目录
cp -r ./mysql/data ./mysql/backup-2026-01-27

# 标准方法：使用 mysqldump
docker exec promptops-mysql mysqldump -u root -ppassword promptops > backup.sql
```

---

## 下一步

MySQL 启动后，接下来要做：
1. 在 Spring Boot 中配置数据库连接
2. 创建数据表（JPA 实体类）
3. 测试 CRUD 操作

---

## 术语表

| 术语 | 英文 | 解释 |
|------|------|------|
| 镜像 | Image | 静态的软件包，包含运行环境和代码 |
| 容器 | Container | 镜像运行起来的实例 |
| 卷 | Volume | 持久化存储，容器删除数据还在 |
| 宿主机 | Host | 你的电脑（相对于容器） |
| 端口映射 | Port Mapping | 把容器端口暴露到宿主机 |
| 环境变量 | Environment Variable | 配置参数 |

---

## 参考资料

- [Docker 官方文档](https://docs.docker.com/)
- [MySQL Docker 镜像](https://hub.docker.com/_/mysql)
- [docker-compose 文档](https://docs.docker.com/compose/)
