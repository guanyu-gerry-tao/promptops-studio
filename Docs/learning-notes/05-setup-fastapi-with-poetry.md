# 学习笔记 05：使用 Poetry 配置 FastAPI 项目

**日期：** 2026-01-27
**目标：** 用 Poetry 管理 FastAPI 项目依赖

---

## 概念澄清

| 工具 | 用途 |
|------|------|
| **pipx** | 安装 CLI 工具（Poetry、Black 等） |
| **Poetry** | 管理项目依赖（FastAPI、requests 等） |
| **pip** | 传统包管理（不推荐混用） |

**流程：**
```
pipx install poetry  → 用 pipx 安装 Poetry
poetry add fastapi   → 用 Poetry 管理项目依赖
```

---

## Poetry 核心概念

| 文件/概念 | 作用 | 类比 |
|-----------|------|------|
| `pyproject.toml` | 项目配置 | package.json |
| `poetry.lock` | 锁定依赖版本 | package-lock.json |
| 虚拟环境 | 隔离项目依赖 | node_modules |

---

## Python 项目结构（Src Layout）

### 推荐的目录结构

```
ai-runtime/              # 项目根目录（文件夹名，可以有 -）
├── src/                 # 源码目录
│   └── ai_runtime/      # Python 包（import 时的名字，必须用 _）
│       ├── __init__.py  # 标记为 Python 包
│       └── main.py      # FastAPI 应用
├── tests/               # 测试代码
├── .venv/               # 虚拟环境
├── pyproject.toml       # Poetry 配置
├── poetry.lock          # 依赖锁文件
└── README.md            # 项目说明
```

### 为什么要有两层目录？

| 层级 | 名称 | 作用 | 命名规则 |
|------|------|------|----------|
| `ai-runtime/` | **项目目录** | Git 仓库/文件夹名 | 可以有连字符 `-` |
| `ai_runtime/` | **Python 包名** | `import` 时的名字 | 必须是合法标识符（用 `_`） |

**为什么包名不能有连字符？**
```python
import ai-runtime  # ❌ 语法错误！Python 理解为 ai 减去 runtime
import ai_runtime  # ✅ 正确
```

**为什么需要包目录？**
```python
# 有包名：清晰的命名空间
from ai_runtime.main import app  # ✅ 知道来自 ai_runtime 项目

# 没有包名：容易冲突
from main import app  # ❌ 不知道是哪个项目的 main
```

---

## 创建 FastAPI 项目

### 1. 初始化项目目录

```bash
cd ai-runtime
mkdir -p src/ai_runtime tests
touch src/ai_runtime/__init__.py
```

**目录说明：**
- `src/ai_runtime/` - Python 包目录
- `tests/` - 测试代码目录
- `__init__.py` - 标记 ai_runtime 为 Python 包

---

### 2. 配置 Poetry（纯 Poetry 模式）

**创建 `pyproject.toml`：**
```toml
[tool.poetry]
name = "ai-runtime"                                    # 项目名（可以有 -）
version = "0.1.0"
description = "AI Runtime Service with FastAPI"
authors = ["Your Name <your@email.com>"]
license = "MIT"
readme = "README.md"
packages = [{ include = "ai_runtime", from = "src" }]  # ⭐ 关键配置

[tool.poetry.dependencies]
python = ">=3.13"
fastapi = ">=0.128.0,<0.129.0"
uvicorn = {extras = ["standard"], version = ">=0.40.0,<0.41.0"}

[build-system]
requires = ["poetry-core>=2.0.0,<3.0.0"]
build-backend = "poetry.core.masonry.api"
```

**⭐ 关键配置说明：**
```toml
packages = [{ include = "ai_runtime", from = "src" }]
```
- `include = "ai_runtime"` - 包名（目录名）
- `from = "src"` - 告诉 Poetry 包在 `src/` 目录下
- 这样 Poetry 知道要安装 `src/ai_runtime/` 这个包

---

### 3. Poetry 配置格式对比

| 格式 | 适用场景 | PyCharm/Poetry 兼容性 |
|------|----------|----------------------|
| **纯 Poetry** ([tool.poetry]) | Poetry 项目 | ✅ 最佳 |
| **PEP 621** ([project]) | 通用 Python 项目 | ⚠️ 可能有问题 |

**常见错误（混用两种格式）：**
```toml
# ❌ 错误：混用 [project] 和 poetry.core.masonry.api
[project]
name = "ai-runtime"
...

[build-system]
build-backend = "poetry.core.masonry.api"  # Poetry 不知道要装哪个包
```

**解决方法：**
- 统一用 `[tool.poetry]` 格式
- 或完全不用 Poetry（改用 pip + setuptools）

---

### 4. 配置本地虚拟环境

```bash
cd ai-runtime
poetry config virtualenvs.in-project true --local
```

**这会创建 `poetry.toml`：**
```toml
[virtualenvs]
in-project = true
```

---

### 5. 安装依赖

```bash
# 方式 A：从 pyproject.toml 安装（推荐）
poetry install

# 方式 B：添加新依赖
poetry add fastapi "uvicorn[standard]"
```

**自动发生：**
- 创建 `.venv/` 虚拟环境
- 下载依赖包
- 生成 `poetry.lock`
- 安装当前项目（ai_runtime 包）

---

### 6. 创建应用代码

**文件：`src/ai_runtime/main.py`**
```python
from datetime import datetime
from fastapi import FastAPI

app = FastAPI(
    title="AI Runtime Service",
    description="AI Runtime Service with FastAPI and LangChain",
    version="0.1.0",
)

@app.get("/health")
def health_check():
    return {
        "status": "OK",
        "timestamp": datetime.now().isoformat(),
        "service": "ai-runtime",
    }

@app.get("/")
def root():
    return {
        "message": "AI Runtime Service",
        "version": "0.1.0",
        "docs": "/docs",
    }
```

---

### 7. 启动应用

```bash
poetry run uvicorn ai_runtime.main:app --reload
```

**⚠️ 注意命令变化：**
```bash
# 扁平结构（main.py 在根目录）
poetry run uvicorn main:app --reload  # ❌ 旧方式

# Src Layout（main.py 在 src/ai_runtime/）
poetry run uvicorn ai_runtime.main:app --reload  # ✅ 新方式
```

**命令解读：**
- `poetry run` - 在虚拟环境中运行
- `uvicorn` - ASGI 服务器
- `ai_runtime.main` - 包名.模块名
- `:app` - 模块中的 FastAPI 实例变量
- `--reload` - 代码变化自动重启

**输出：**
```
INFO: Uvicorn running on http://127.0.0.1:8000
INFO: Application startup complete.
```

---

### 8. 测试应用

**启动服务器：**
```bash
cd ai-runtime
poetry run uvicorn ai_runtime.main:app --reload
```

**测试方法 1：浏览器**
- Health: `http://localhost:8000/health`
- Root: `http://localhost:8000/`
- 文档: `http://localhost:8000/docs` ← **自动生成的 Swagger UI**

**测试方法 2：curl**
```bash
curl http://localhost:8000/health
```

**输出：**
```json
{
  "status": "OK",
  "timestamp": "2026-01-27T18:56:44.720514",
  "service": "ai-runtime"
}
```

**测试方法 3：Python 代码**
```python
import requests

response = requests.get("http://localhost:8000/health")
print(response.json())
```

---

## Poetry 常用命令

### 依赖管理

```bash
# 添加依赖
poetry add fastapi

# 添加开发依赖
poetry add --group dev pytest

# 安装所有依赖（从 lock 文件）
poetry install

# 更新依赖
poetry update

# 更新 lock 文件（不安装）
poetry lock

# 查看依赖
poetry show

# 查看依赖树
poetry show --tree
```

### 虚拟环境管理

```bash
# 查看虚拟环境路径
poetry env info --path

# 列出所有虚拟环境
poetry env list

# 删除虚拟环境
poetry env remove python3

# 删除所有虚拟环境
poetry env remove --all

# 进入虚拟环境
poetry shell

# 退出虚拟环境
exit
```

### 运行命令

```bash
# 在虚拟环境中运行命令
poetry run uvicorn ai_runtime.main:app --reload

# 在虚拟环境中运行 Python
poetry run python script.py

# 在虚拟环境中运行测试
poetry run pytest
```

---

## 快速参考

### 从零创建项目

```bash
# 1. 创建目录结构
mkdir -p ai-runtime/src/ai_runtime ai-runtime/tests
cd ai-runtime
touch src/ai_runtime/__init__.py

# 2. 创建 pyproject.toml（见上面的配置）
# 手动创建或使用 poetry init

# 3. 配置本地虚拟环境
poetry config virtualenvs.in-project true --local

# 4. 安装依赖
poetry add fastapi "uvicorn[standard]"

# 5. 创建应用代码
# 编辑 src/ai_runtime/main.py

# 6. 运行应用
poetry run uvicorn ai_runtime.main:app --reload
```

### 克隆项目后的设置

```bash
# 1. 进入项目目录
cd ai-runtime

# 2. 配置本地虚拟环境（可选）
poetry config virtualenvs.in-project true --local

# 3. 安装依赖
poetry install

# 4. 运行应用
poetry run uvicorn ai_runtime.main:app --reload
```

---

## FastAPI 自动生成的文档

**访问 `/docs`：**
- 交互式 API 文档（Swagger UI）
- 可以直接测试 API
- 自动显示参数、响应格式

**访问 `/redoc`：**
- 另一种风格的文档（ReDoc）

---

## 虚拟环境配置方式

### 方式 A：全局缓存（Poetry 默认）

**位置：** `~/Library/Caches/pypoetry/virtualenvs/`

**优点：**
- ✅ 节省磁盘空间（多项目可共享）
- ✅ 项目目录干净
- ✅ 不会误提交到 Git

**缺点：**
- ❌ PyCharm/VSCode 难以自动识别
- ❌ 路径很深，不直观

---

### 方式 B：项目本地 `.venv/`（推荐）

**位置：** `项目/.venv/`

**配置方法：**
```bash
# 全局设置（影响所有新项目）
poetry config virtualenvs.in-project true

# 或在项目中创建 poetry.toml
[virtualenvs]
in-project = true
```

**重新创建虚拟环境：**
```bash
poetry env remove python  # 删除旧环境
poetry install            # 重新创建
```

**优点：**
- ✅ PyCharm/VSCode 自动识别
- ✅ 路径清晰（在项目目录）
- ✅ 符合 Python 传统习惯

**缺点：**
- ❌ 每个项目占用几百 MB
- ⚠️ 需要确保 `.gitignore` 包含 `.venv/`

---

### 选择建议

| 场景 | 推荐方式 |
|------|----------|
| **个人学习项目** | 项目本地（IDE 友好） |
| **团队小项目** | 项目本地（降低配置成本） |
| **大型团队项目** | 全局缓存（有统一规范） |
| **CI/CD 环境** | 全局缓存（构建更快） |

---

## PyCharm 配置

### 推荐方式：将 ai-runtime 作为独立项目打开

**操作步骤：**
1. `File > Open`
2. 选择 `ai-runtime` 文件夹（不是整个仓库根目录）
3. PyCharm 会自动识别：
   - ✅ Poetry 项目
   - ✅ 自动找到 `.venv/bin/python`
   - ✅ `src/` 自动标记为 Sources Root

**为什么这样做？**
- ✅ 避免多个 `pyproject.toml` 冲突
- ✅ PyCharm 知道正确的项目根目录
- ✅ Poetry 命令都在正确的目录执行
- ✅ 不会在仓库根目录误创建 `.venv/`

**缺点：**
- ⚠️ 看不到整个 monorepo（看不到 platform-api、frontend 等）

---

### 如果需要看整个 monorepo

如果想在 PyCharm 中同时看到多个子项目：

1. 打开整个仓库根目录
2. **不要**让 PyCharm 把根目录当作 Poetry 项目
3. 手动配置 Interpreter：
   - `Settings > Project > Python Interpreter`
   - `Add Interpreter > Add Local Interpreter`
   - 选择 `Existing`
   - 路径：`ai-runtime/.venv/bin/python`
4. 标记 Sources Root：
   - 右键 `ai-runtime/src/` → `Mark Directory as > Sources Root`

---

## 常见问题

### Q: 虚拟环境在哪里？

```bash
poetry env info --path

# 全局缓存：~/Library/Caches/pypoetry/virtualenvs/xxx
# 项目本地：/path/to/project/.venv
```

---

### Q: 如何删除虚拟环境？

```bash
# 删除当前环境
poetry env remove python3

# 删除所有环境
poetry env remove --all
```

---

### Q: 报错 "No file/folder found for package xxx"

**原因：**
- Poetry 找不到要安装的包目录

**解决方法：**
1. 检查 `pyproject.toml` 是否有：
   ```toml
   [tool.poetry]
   packages = [{ include = "ai_runtime", from = "src" }]
   ```
2. 确认目录结构：
   ```
   ai-runtime/
   └── src/
       └── ai_runtime/
           └── __init__.py  ← 必须有这个文件
   ```
3. 重新安装：
   ```bash
   poetry lock
   poetry install
   ```

---

### Q: PyCharm 在根目录创建了 .venv/

**原因：**
- PyCharm 认为根目录是 Poetry 项目
- 在根目录运行了 `poetry install`

**解决方法：**
1. 删除根目录的 `pyproject.toml`、`poetry.lock`、`.venv/`
2. 将 `ai-runtime` 作为独立项目打开（推荐）
3. 或配置 `poetry config virtualenvs.in-project true --local`

---

## 版本约束

```toml
[tool.poetry.dependencies]
fastapi = "^0.128.0"  # >=0.128.0, <1.0.0（caret）
requests = "~2.31.0"  # >=2.31.0, <2.32.0（tilde）
pytest = ">=7.0"      # 7.0 及以上
```

**符号说明：**
- `^` (caret) - 兼容更新，不改变最左非零版本号
- `~` (tilde) - 小版本更新，只更新最右版本号
- `>=` - 大于等于指定版本

---

## Poetry 配置文件详解

### poetry.toml vs pyproject.toml

| 文件 | 作用 | 提交到 Git？ |
|------|------|------------|
| `pyproject.toml` | 项目配置和依赖 | ✅ 是 |
| `poetry.toml` | 本地 Poetry 设置 | ❌ 否（可选） |
| `poetry.lock` | 锁定依赖版本 | ✅ 是 |

**poetry.toml 示例：**
```toml
[virtualenvs]
in-project = true  # 虚拟环境创建在项目本地
```

**作用域：**
- `poetry.toml` - 只影响当前项目
- `poetry config` 全局设置 - 影响所有项目

---

### 完整的 pyproject.toml 示例

```toml
[tool.poetry]
name = "ai-runtime"
version = "0.1.0"
description = "AI Runtime Service"
authors = ["Your Name <email@example.com>"]
license = "MIT"
readme = "README.md"
packages = [{ include = "ai_runtime", from = "src" }]

[tool.poetry.dependencies]
python = ">=3.13"
fastapi = ">=0.128.0,<0.129.0"
uvicorn = {extras = ["standard"], version = ">=0.40.0,<0.41.0"}

[tool.poetry.group.dev.dependencies]
pytest = "^8.0.0"
black = "^24.0.0"

[build-system]
requires = ["poetry-core>=2.0.0,<3.0.0"]
build-backend = "poetry.core.masonry.api"
```

**字段说明：**
- `packages` - 告诉 Poetry 包在哪里（src layout 必须）
- `[tool.poetry.dependencies]` - 生产依赖
- `[tool.poetry.group.dev.dependencies]` - 开发依赖（不会安装到生产环境）

---

## 参考

- [Poetry 文档](https://python-poetry.org/docs/)
- [FastAPI 文档](https://fastapi.tiangolo.com/)
- [Python Packaging 用户指南](https://packaging.python.org/)
