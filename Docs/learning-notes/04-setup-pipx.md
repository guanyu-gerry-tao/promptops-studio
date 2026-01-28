# 学习笔记 04：配置 pipx

**日期：** 2026-01-27
**目标：** 学会使用 pipx 管理 Python CLI 工具

---

## 什么是 pipx？

**pipx** 是专门用来安装 Python CLI 工具的包管理器。

**类比：**
- pipx = npm install -g（全局安装）
- pip = npm install（项目安装）

---

## 为什么用 pipx？

| 问题 | pip 的缺点 | pipx 的优势 |
|------|-----------|------------|
| **环境隔离** | 污染全局 Python | 每个工具独立虚拟环境 |
| **依赖冲突** | 工具之间可能冲突 | 完全隔离 |
| **更新管理** | 难以管理 | `pipx upgrade` 统一更新 |
| **卸载干净** | 依赖残留 | `pipx uninstall` 完全清理 |

---

## 安装 pipx

```bash
# 安装 pipx
python3 -m pip install --user pipx

# 配置 PATH
python3 -m pipx ensurepath

# 重启终端或执行
source ~/.bashrc  # 或 ~/.zshrc
```

**验证：**
```bash
pipx --version
# 输出：1.8.0
```

---

## 常用命令

### 安装工具
```bash
pipx install poetry
pipx install black
pipx install flake8
```

### 查看已安装
```bash
pipx list
```

### 升级工具
```bash
pipx upgrade poetry
pipx upgrade-all  # 升级所有
```

### 卸载工具
```bash
pipx uninstall poetry
```

### 运行工具（不安装）
```bash
pipx run cowsay "Hello"
```

---

## pipx vs pip

| 场景 | 使用工具 |
|------|----------|
| 安装 CLI 工具（Poetry、Black） | pipx |
| 项目依赖（FastAPI、requests） | pip 或 Poetry |
| 系统级库 | 避免使用 pip install |

---

## 实际应用

**安装 Poetry（项目依赖管理工具）：**
```bash
pipx install poetry
poetry --version
```

**安装 Black（代码格式化工具）：**
```bash
pipx install black
black --version
```

---

## 常见问题

**Q: pipx 安装在哪里？**
A: `~/.local/pipx/venvs/`，每个工具一个独立虚拟环境

**Q: 可以全局用 pip 代替 pipx 吗？**
A: 可以但不推荐，容易造成依赖冲突

**Q: pipx 和项目的虚拟环境冲突吗？**
A: 不会，完全隔离

---

## 参考

- [pipx 官方文档](https://pypa.github.io/pipx/)
