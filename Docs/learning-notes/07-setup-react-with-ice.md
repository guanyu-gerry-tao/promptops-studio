# 学习笔记 07：使用 Ice.js 创建 React 前端

**日期：** 2026-01-27
**目标：** 用 Ice.js 搭建企业级 React 前端项目

---

## Ice.js 是什么？

**Ice.js** = 阿里开源的 React 应用框架（企业级，开箱即用）

**对比其他工具：**

| 工具 | 定位 | 特点 |
|------|------|------|
| **Create React App** | 官方脚手架 | 基础，已不再维护 |
| **Vite** | 构建工具 | 超快，但需自己配置 |
| **Ice.js** | 应用框架 | 完整解决方案，企业级 |

**Ice.js 提供：**
- ✅ React + TypeScript
- ✅ 路由系统（约定式）
- ✅ 状态管理（内置）
- ✅ UI 组件库（可选 Ant Design）
- ✅ 请求封装
- ✅ 权限控制
- ✅ 完整项目结构

---

## 创建项目

### 方式 A：在新目录创建

```bash
npm init ice my-app
```

### 方式 B：在已有目录创建（推荐）

```bash
cd frontend
npm init ice .
```

**交互式选择：**
1. **模板选择：** Antd Pro Scaffold（企业级后台）
2. **TypeScript：** Yes
3. **包管理器：** npm

---

## 模板选项

| 模板 | 适用场景 |
|------|---------|
| **Antd Pro Scaffold** | 企业级管理后台（推荐） |
| Web Lite Scaffold | 轻量级应用 |
| Fusion Pro Scaffold | Fusion 组件库 |
| Miniapp Scaffold | 小程序 |

---

## 安装依赖

```bash
cd frontend
npm install
```

---

## 启动开发服务器

```bash
npm start

# 或使用加速模式
npm run start -- --speedup
```

**访问：** `http://localhost:3000`

**输出：**
```
✔ Webpack compiled successfully
✔ Server listening at http://localhost:3000
```

---

## 项目结构

```
frontend/
├── src/
│   ├── app.ts              # 应用配置（权限、store、请求）
│   ├── document.tsx        # HTML 模板
│   ├── menuConfig.tsx      # 左侧菜单配置
│   ├── global.css          # 全局样式
│   │
│   ├── pages/              # 页面目录（约定式路由）
│   │   ├── layout.tsx          # 布局组件（菜单+Header）
│   │   ├── index.tsx           # 工作台页面（路由：/）
│   │   ├── form/
│   │   │   └── index.tsx       # 表单页面（路由：/form）
│   │   └── list/
│   │       └── index.tsx       # 列表页面（路由：/list）
│   │
│   ├── components/         # 通用组件
│   ├── services/           # API 服务
│   ├── models/             # 数据模型
│   └── assets/             # 静态资源
│
├── ice.config.mts          # Ice 配置
├── package.json
└── tsconfig.json
```

---

## 核心概念

### 1. 约定式路由

**规则：** 文件路径自动映射为路由

```
pages/index.tsx       → 路由：/
pages/form/index.tsx  → 路由：/form
pages/list/index.tsx  → 路由：/list
```

**无需手动配置 `<Route>`！**

---

### 2. 布局系统

**`pages/layout.tsx`** - 所有页面的外壳

```tsx
export default function Layout() {
  return (
    <ProLayout>  {/* Ant Design Pro 布局组件 */}
      <Outlet />  {/* 当前页面内容 */}
    </ProLayout>
  );
}
```

**包含：**
- 左侧菜单
- 顶部 Header
- 页面内容区

---

### 3. 菜单配置

**`menuConfig.tsx`** - 定义左侧菜单

```tsx
export const asideMenuConfig = [
  {
    name: '工作台',
    path: '/',
    icon: <DashboardOutlined />,
  },
  {
    name: '表单',
    path: '/form',
    icon: <FormOutlined />,
  },
];
```

**自动渲染为侧边栏菜单！**

---

### 4. 应用配置

**`app.ts`** - 全局配置

```ts
export default defineAppConfig(() => ({}));

// 权限配置
export const authConfig = defineAuthConfig(...);

// Store 配置
export const storeConfig = defineStoreConfig(...);

// 请求配置
export const request = defineRequestConfig(() => ({
  baseURL: '/api',
}));
```

---

## 页面布局系统（Ant Design Grid）

### 栅格系统（24列）

```tsx
import { Row, Col } from 'antd';

export default function Dashboard() {
  return (
    <Row gutter={[16, 16]}>  {/* 间距 16px */}

      {/* 占 6/24 = 25% */}
      <Col span={6}>
        <Card>卡片 1</Card>
      </Col>

      {/* 占 12/24 = 50% */}
      <Col span={12}>
        <Card>卡片 2</Card>
      </Col>

      {/* 占 6/24 = 25% */}
      <Col span={6}>
        <Card>卡片 3</Card>
      </Col>

      {/* 占 24/24 = 100% */}
      <Col span={24}>
        <Card>卡片 4</Card>
      </Col>
    </Row>
  );
}
```

**效果：**
```
┌──────┬────────────┬──────┐
│ 25%  │    50%     │ 25%  │
├──────┴────────────┴──────┤
│          100%            │
└──────────────────────────┘
```

---

### 响应式布局

```tsx
<Col xs={24} sm={12} md={6}>
  <Card>响应式卡片</Card>
</Col>
```

| 属性 | 屏幕宽度 | 列宽 | 效果 |
|------|---------|------|------|
| `xs` | < 576px | 24 | 手机：占满 100% |
| `sm` | ≥ 576px | 12 | 平板：占 50% |
| `md` | ≥ 768px | 6 | 桌面：占 25% |

---

## 常用命令

```bash
# 开发模式
npm start

# 构建生产版本
npm run build

# 代码检查
npm run eslint

# 自动修复代码格式
npm run eslint:fix

# 样式检查
npm run stylelint
```

---

## Ice.js vs Vite 对比

| 特性 | Vite | Ice.js |
|------|------|--------|
| **入口文件** | `main.tsx` 可见 | 框架隐藏 |
| **路由** | 手动配置 `<Route>` | 自动扫描 `pages/` |
| **UI 库** | 需自己安装 | 模板已集成 Ant Design |
| **布局** | 手动创建 | `pages/layout.tsx` 约定 |
| **菜单** | 手动写 JSX | `menuConfig.tsx` 配置 |
| **状态管理** | 需自己选择 | 内置 store |
| **权限控制** | 需自己实现 | plugin-auth 插件 |
| **学习成本** | 低（手动） | 中（约定） |
| **开发效率** | 中 | 高（开箱即用） |

---

## 关键文件说明

| 文件 | 作用 | 必须？ |
|------|------|-------|
| `app.ts` | 应用配置 | ✅ 是 |
| `document.tsx` | HTML 模板 | ✅ 是 |
| `pages/layout.tsx` | 页面布局 | ⚠️ 可选 |
| `menuConfig.tsx` | 菜单配置 | ⚠️ 可选 |
| `ice.config.mts` | Ice 框架配置 | ✅ 是 |

---

## 启动流程（框架约定）

```
1. ice.config.mts（配置）
   ↓
2. Ice.js 框架启动
   ↓
3. document.tsx（HTML 模板）
   ↓
4. app.ts（应用配置）
   ↓
5. 扫描 pages/（生成路由）
   ↓
6. pages/layout.tsx（布局）
   ├─ 使用 menuConfig.tsx
   └─ 渲染 <Outlet />（当前页面）
   ↓
7. pages/index.tsx（具体页面）
```

**关键：** 很多步骤是框架"约定"自动完成的，不像 Vite 那样手动可见。

---

## 术语解释

- **约定式路由** - 根据文件结构自动生成路由，无需手动配置
- **约定大于配置** - 遵循框架约定（文件名、目录结构），减少配置
- **ProLayout** - Ant Design Pro 提供的企业级布局组件
- **Outlet** - 路由占位符，显示当前路由对应的页面
- **栅格系统** - 把页面分成 24 列，用数字控制宽度

---

## 布局自定义

**修改布局模式：**
```tsx
<ProLayout
  layout="mix"   // 顶部+侧边混合（默认）
  layout="side"  // 只有侧边栏
  layout="top"   // 只有顶部菜单
>
```

**隐藏元素：**
```tsx
<ProLayout
  headerRender={false}  // 隐藏顶部
  footerRender={false}  // 隐藏底部
  menuRender={false}    // 隐藏菜单
>
```

**某些页面不用布局：**
```tsx
export default function Layout() {
  if (location.pathname === '/login') {
    return <Outlet />;  // 登录页无布局
  }

  return <ProLayout>...</ProLayout>;
}
```

---

## 常见问题

**Q: 找不到入口文件？**
A: Ice.js 的入口被框架隐藏了，不像 Vite 有 `main.tsx`。

**Q: 如何添加新页面？**
A: 在 `pages/` 下创建新文件夹和 `index.tsx`，路由自动生成。

**Q: 如何修改菜单？**
A: 编辑 `menuConfig.tsx`。

**Q: 布局是固定的吗？**
A: 不是，`pages/layout.tsx` 完全可以自定义或删除。

**Q: Ice.js 和 Vite 能一起用吗？**
A: 不能，Ice.js 是完整框架，已包含构建工具。

---

## 下一步

- 修改 `menuConfig.tsx` 创建自己的菜单
- 在 `pages/` 下创建新页面
- 自定义 `pages/layout.tsx` 布局
- 连接后端 API（配置 `app.ts` 中的 `request`）

---

## 参考

- [Ice.js 官方文档](https://v3.ice.work/)
- [Ant Design 组件库](https://ant.design/components/overview-cn/)
- [Ant Design Pro Layout](https://procomponents.ant.design/components/layout)
