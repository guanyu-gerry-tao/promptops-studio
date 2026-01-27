# 学习笔记 01：初始化 Spring Boot 项目

**日期：** 2026-01-27
**目标：** 学会如何从零创建一个 Spring Boot 项目

---

## 背景知识

### 什么是 Spring Boot？

**Spring Boot** 是一个 Java 框架，用来快速搭建 Web 应用和 REST API。

**通俗解释：**
- 就像 Node.js 的 Express，Python 的 FastAPI
- 帮你处理 HTTP 请求、数据库连接、配置管理等常见任务
- "Boot" 的意思是"快速启动"，不需要繁琐的配置

### 核心概念

| 概念 | 解释 | 类比 |
|------|------|------|
| **Spring Boot** | Java Web 框架 | Express (Node.js) |
| **Gradle** | 构建工具 | npm (Node.js), pip (Python) |
| **Maven** | 另一个构建工具 | 和 Gradle 类似，但用 XML 配置 |
| **JPA** | 数据库操作框架 | ORM，像 Sequelize, SQLAlchemy |

---

## 创建项目的方法

### 方法 A：使用 Spring Initializr（网页）

**网址：** https://start.spring.io/

**步骤：**
1. 打开网页
2. 选择：
   - Project: **Gradle - Groovy**
   - Language: **Java**
   - Spring Boot: **3.x** 或最新版
   - Java: **21**
3. 填写项目信息：
   - Group: `com.promptops`
   - Artifact: `platform-api`
   - Name: `platform-api`
4. 添加依赖：
   - Spring Web
   - Spring Data JPA
   - MySQL Driver
   - Lombok
5. 点击 **Generate** 下载 ZIP
6. 解压到项目目录

---

### 方法 B：使用 IntelliJ IDEA（推荐）

**步骤：**
1. 打开 IntelliJ IDEA
2. File → New → Project
3. 选择 **Spring Initializr**
4. 配置同上
5. Next → 选择依赖 → Finish

---

## 项目结构解读

生成的项目结构：

```
platform-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/promptops/platformapi/
│   │   │       └── PlatformApiApplication.java  ← 主入口
│   │   └── resources/
│   │       └── application.yml                  ← 配置文件
│   └── test/                                    ← 测试代码
├── build.gradle                                 ← 依赖配置
└── gradlew                                      ← Gradle 启动脚本
```

### 关键文件说明

#### 1. `PlatformApiApplication.java` - 主入口

```java
@SpringBootApplication  // ← 这是魔法注解
public class PlatformApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
```

**解释：**
- `@SpringBootApplication` - 告诉 Spring 这是启动类
- `main` 方法 - Java 程序的入口点
- `SpringApplication.run()` - 启动 Spring Boot 应用

#### 2. `build.gradle` - 依赖管理

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'
}
```

**解释：**
- `spring-boot-starter-web` - 包含 Tomcat 服务器、Spring MVC
- `spring-boot-starter-data-jpa` - 数据库操作（Hibernate）
- `mysql-connector-j` - MySQL 驱动

#### 3. `application.yml` - 配置文件

```yaml
server:
  port: 8080  # 服务器端口
```

---

## 启动项目

### 方法 1：在 IntelliJ 中运行

1. 找到 `PlatformApiApplication.java`
2. 右键 → Run 'PlatformApiApplication'
3. 看到控制台输出：
   ```
   Started PlatformApiApplication in 2.5 seconds
   ```

### 方法 2：命令行运行

```bash
cd platform-api
./gradlew bootRun
```

**命令解释：**
- `./gradlew` - 运行 Gradle Wrapper（自带的 Gradle）
- `bootRun` - Spring Boot 的运行任务

---

## 验证是否成功

### 方法 1：浏览器访问

打开浏览器，访问：
```
http://localhost:8080
```

**预期结果：**
- 看到 404 或 Whitelabel Error Page → **成功**（应用运行了，只是没有页面）
- 无法访问 → **失败**（应用没启动）

### 方法 2：命令行测试

```bash
curl http://localhost:8080
```

---

## 常见问题

### Q1: 启动失败，提示端口被占用

**错误：**
```
Port 8080 was already in use
```

**原因：** 8080 端口已被其他程序占用

**解决方法 A：** 更改端口
```yaml
# application.yml
server:
  port: 8081
```

**解决方法 B：** 找到并关闭占用端口的程序
```bash
# macOS/Linux
lsof -i :8080

# 关闭进程
kill -9 <PID>
```

---

### Q2: 找不到 Java 或 Gradle

**错误：**
```
JAVA_HOME not set
```

**解决方法：**
1. 安装 JDK 21
2. 设置环境变量（IntelliJ 会自动帮你设置）

---

### Q3: 依赖下载很慢

**原因：** Maven 中央仓库在国外

**解决方法：** 使用国内镜像（阿里云）

在 `build.gradle` 中添加：
```gradle
repositories {
    maven { url 'https://maven.aliyun.com/repository/public' }
    mavenCentral()
}
```

---

## 下一步

项目创建好后，接下来要做：
1. 连接数据库（见笔记 02）
2. 创建第一个 API 端点（Controller）
3. 测试 API

---

## 术语表

| 术语 | 英文 | 解释 |
|------|------|------|
| 注解 | Annotation | 以 `@` 开头的标签，给代码添加元数据 |
| 依赖 | Dependency | 项目需要的外部库 |
| 构建 | Build | 编译代码、下载依赖、打包的过程 |
| 端口 | Port | 网络通信的"门牌号"，HTTP 默认 80，我们用 8080 |
| 启动器 | Starter | Spring Boot 提供的依赖包，包含多个相关库 |

---

## 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Initializr](https://start.spring.io/)
- [Gradle 官方文档](https://gradle.org/)
