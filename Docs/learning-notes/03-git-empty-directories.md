# 学习笔记 03：Git 如何处理空目录

**日期：** 2026-01-27
**目标：** 理解 Git 的文件跟踪机制，学会保留空目录结构

---

## 问题场景

**场景：** 你创建了项目目录结构

```
promptops-studio/
├── frontend/           ← 空目录
├── ai-runtime/         ← 空目录
└── platform-api/       ← 有文件
```

**执行：**
```bash
git add .
git commit -m "add directories"
git push
```

**问题：** 同事克隆仓库后，`frontend/` 和 `ai-runtime/` 目录**不见了**！

---

## 为什么会这样？

### Git 的设计原则

**关键概念：Git 只跟踪文件，不跟踪目录。**

```
目录本身 → Git 不管
目录里的文件 → Git 跟踪
```

**类比：**
- Git 就像图书馆管理员
- 管理员只登记**书**（文件），不登记**空书架**（空目录）
- 书架上有书，管理员才知道这个书架存在

---

### 验证实验

**创建目录：**
```bash
mkdir test-dir
git status
```

**输出：**
```
nothing to commit, working tree clean
```

Git 完全看不到 `test-dir/` ❌

**添加文件：**
```bash
touch test-dir/file.txt
git status
```

**输出：**
```
Untracked files:
  test-dir/file.txt
```

Git 现在看到了 `test-dir/` ✅（因为里面有文件）

---

## 解决方案：使用 .gitkeep

### 什么是 .gitkeep？

**`.gitkeep` 是一个约定俗成的空文件。**

- 文件名可以随便取（`.gitignore`、`README.md` 也行）
- 社区习惯用 `.gitkeep`，见名知意："保持这个目录"

**原理：**
```
空目录 → Git 忽略
空目录 + .gitkeep 文件 → Git 跟踪
```

---

### 具体操作

#### 方法 A：手动创建

```bash
# 在每个空目录中创建 .gitkeep
touch frontend/.gitkeep
touch ai-runtime/.gitkeep
touch deploy/.gitkeep

# 添加到 Git
git add frontend/.gitkeep ai-runtime/.gitkeep deploy/.gitkeep
git commit -m "chore: add directory structure with .gitkeep files"
git push
```

#### 方法 B：批量创建

```bash
# 使用 find 命令（找到所有空目录）
find . -type d -empty -not -path "./.git/*" -exec touch {}/.gitkeep \;

# 添加所有 .gitkeep
git add **/.gitkeep
git commit -m "chore: preserve empty directories"
```

---

### 验证效果

**提交后：**
```bash
git ls-tree -r HEAD --name-only
```

**输出：**
```
frontend/.gitkeep
ai-runtime/.gitkeep
deploy/.gitkeep
platform-api/src/main/java/...
```

**其他人克隆后：**
```bash
git clone <repo>
ls -la
```

**看到：**
```
frontend/          ← 目录存在！
ai-runtime/        ← 目录存在！
deploy/            ← 目录存在！
```

---

## 深入理解：Git 的对象模型

### Git 存储的三种对象

| 对象类型 | 存储内容 | 例子 |
|----------|----------|------|
| **Blob** | 文件内容 | `README.md` 的内容 |
| **Tree** | 目录结构 | 文件名 + Blob 的引用 |
| **Commit** | 提交信息 | 作者、时间、Tree 引用 |

**关键点：** Tree 对象包含文件列表，但不记录空目录。

### 查看 Git 内部结构

```bash
# 查看最新提交的 tree
git cat-file -p HEAD^{tree}
```

**输出：**
```
040000 tree abc123  frontend
040000 tree def456  ai-runtime
100644 blob 789abc  README.md
```

- `040000` - 目录（tree）
- `100644` - 文件（blob）
- 如果目录是空的，这里不会出现

---

## 其他解决方案

### 方案 A：使用 README.md

在空目录中放 `README.md`：

```bash
echo "# Frontend\n\nReact application will be here." > frontend/README.md
git add frontend/README.md
```

**优点：** 可以写说明文档
**缺点：** 文件有内容，将来可能需要删除

---

### 方案 B：使用 .gitignore 的反向规则

```bash
# 在空目录中创建 .gitignore
cat > frontend/.gitignore << EOF
# Ignore everything
*

# But keep this file
!.gitignore
EOF
```

**优点：** 一石二鸟，目录保留 + 可以配置忽略规则
**缺点：** 语义不如 `.gitkeep` 清晰

---

### 方案 C：什么都不做

有些团队选择不提交空目录，由开发者自己创建。

在文档中说明：
```markdown
## Setup

Clone the repo and create these directories:
\`\`\`bash
mkdir frontend ai-runtime deploy
\`\`\`
```

**优点：** 简单
**缺点：** 新人容易忘记，增加上手成本

---

## 实际项目建议

### 什么时候用 .gitkeep？

| 场景 | 是否使用 |
|------|----------|
| **规划好的目录结构** | ✅ 使用 |
| **临时测试目录** | ❌ 不用 |
| **构建输出目录** | ❌ 不用（用 .gitignore） |
| **日志目录** | ✅ 使用（但 .gitignore 日志文件） |

### 配合 .gitignore 使用

**场景：** `logs/` 目录需要存在，但不要提交日志文件

```bash
# 创建目录和 .gitkeep
mkdir logs
touch logs/.gitkeep

# .gitignore 中添加
cat >> .gitignore << EOF
# Logs directory - keep structure but ignore content
logs/*
!logs/.gitkeep
EOF
```

**解释：**
- `logs/*` - 忽略 logs 目录下的所有文件
- `!logs/.gitkeep` - 但保留 .gitkeep

---

## Git Worktree 与目录同步

### 问题：Worktree 看不到空目录

**场景：**
- 主仓库创建了空目录
- Worktree 中看不到

**原因：** 空目录没有提交到 Git

**解决方法：**

```bash
# 在主仓库
touch frontend/.gitkeep ai-runtime/.gitkeep
git add .
git commit -m "chore: add directory structure"

# 在 worktree 中
git merge main  # 或 git pull
```

现在 worktree 中也能看到目录了。

---

## 常见问题

### Q1: .gitkeep 文件内容写什么？

**答案：** 留空即可。

也可以写注释：
```bash
echo "# This file keeps the directory in Git" > .gitkeep
```

但通常留空，因为名字本身就说明了用途。

---

### Q2: 可以用其他文件名吗？

**答案：** 可以，但推荐用 `.gitkeep`。

**其他常见命名：**
- `.keep`
- `.placeholder`
- `.gitignore`（但语义不太对）

**社区标准：** `.gitkeep` 最常用，见名知意。

---

### Q3: 目录有了文件后，需要删除 .gitkeep 吗？

**答案：** 不需要，但可以删。

- **保留：** 提醒这个目录是有意规划的
- **删除：** 减少无用文件

团队统一即可。

---

### Q4: .gitkeep 会被打包到生产环境吗？

**答案：** 会，但影响极小（文件是空的）。

如果介意，可以在构建脚本中删除：
```bash
find . -name ".gitkeep" -delete
```

---

## 实战案例

### 项目初始化目录结构

```bash
# 创建标准目录
mkdir -p {frontend,backend,docs,scripts,tests}

# 添加 .gitkeep
for dir in frontend backend docs scripts tests; do
  touch $dir/.gitkeep
done

# 提交
git add .
git commit -m "chore: initialize project structure"
```

---

### Monorepo 多服务结构

```bash
# 创建服务目录
mkdir -p services/{api,worker,scheduler}

# 每个服务添加 .gitkeep
find services -type d -exec touch {}/.gitkeep \;

# 提交
git add services
git commit -m "chore: add services directory structure"
```

---

## 总结

### 核心要点

1. **Git 只跟踪文件，不跟踪空目录**
2. **`.gitkeep` 是社区约定，用于保留空目录**
3. **配合 `.gitignore` 可以保留目录但忽略内容**

### 最佳实践

```bash
# ✅ 好的做法
mkdir logs
touch logs/.gitkeep
echo "logs/*" >> .gitignore
echo "!logs/.gitkeep" >> .gitignore

# ❌ 不好的做法
mkdir logs
# 不提交，期望别人手动创建
```

---

## 术语表

| 术语 | 解释 |
|------|------|
| **Blob** | Git 中存储文件内容的对象 |
| **Tree** | Git 中存储目录结构的对象 |
| **Worktree** | Git 的工作树，代码实际存放的目录 |
| **约定俗成** | 社区习惯，不是强制规定 |

---

## 参考资料

- [Git Internals - Git Objects](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects)
- [Why doesn't Git track empty directories?](https://git.wiki.kernel.org/index.php/GitFaq#Can_I_add_empty_directories.3F)
