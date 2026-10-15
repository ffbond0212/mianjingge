# 面经阁 · 面试题库复盘系统

一个面向秋招 Java 方向的**自用面试题库管理与复盘系统**：题库沉淀 → 面试记录 → 数据统计 → 遗忘曲线复习 → 投递跟踪，全流程闭环。

> 本项目同时是个人简历项目，采用前后端分离 + Docker 环境，代码与文档可直接作为学习参考。

## 功能总览

| 模块 | 说明 |
|---|---|
| 🔐 登录认证 | Spring Security + JWT 单账号认证，Token 7 天有效，未登录请求统一拦截 |
| 📚 题库管理 | 十大分类（Java 基础/并发/JVM/MySQL/Redis/Spring/网络/操作系统/分布式/行为面），预置 82 道高频题；标准答案 + 个人话术双字段，支持「保存为话术」 |
| 🎤 面试记录 | 按场次记录（公司/岗位/轮次/日期），逐题关联得分、是否答上、扣分点、现场回答快照 |
| 📄 Markdown 导入 | 结构化《面试问答记录》一键解析 → 预览修正 → 落库；分类自动映射、标题全等复用、参考答案回写 |
| 📊 复盘统计 | 被问次数 TOP10、分类均分雷达（十轴）、场次均分趋势；Redis 缓存 30 分钟 + 写时失效 |
| 🧠 复习计划 | 遗忘曲线排期（默认 1/3/7/15/30 天五档，可配置）：低分/未答上自动入队，打卡答上×1.5 延后进档、答差降档、五档毕业归档；Redis ZSet 按到期日排序，今日任务全部到期展示 |
| 📋 投递看板 | 8 列看板跟踪投递进度；固定状态机（已投递→笔试→一面→二面→三面→HR→Offer），任意非终态可转「已挂/已拒」，跳步/终态再流转被拒绝；状态变更自动记时间线，可关联面试场次 |

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | JDK 21 · Spring Boot 3.5 · MyBatis-Plus 3.5 · Spring Security 6 + JWT (jjwt 0.12) · Flyway · Knife4j · Lombok |
| 前端 | Vue 3 · TypeScript · Vite · Element Plus · Pinia · Vue Router · Axios · ECharts |
| 存储 | MySQL 8.4（Docker） · Redis 7.4（Docker） |
| 测试 | JUnit 5 · Mockito · AssertJ（31 个核心单元测试） |
| 部署 | Docker Compose · 一键启停脚本（Windows） |

## 架构

```
┌────────────┐   HTTP/JSON    ┌──────────────────────────────┐
│  Vue3 前端 │ ──────────────▶ │  Controller → Service →      │
│  (5173)    │  Result 统一响应 │  Mapper (MyBatis-Plus)       │
└────────────┘                │  ├─ JWT 认证过滤器            │
                              │  ├─ Flyway 数据库迁移         │
                              │  └─ Redis 统计缓存 / 复习 ZSet │
                              └──────────┬──────────┬─────────┘
                                         │          │
                                    MySQL 8.4    Redis 7.4
                                    (33061)      (63791)
```

```
.
├── backend/            # Spring Boot 后端（com.mianjingge）
│   ├── src/main/java   # controller / service / mapper / enums / util / config
│   ├── src/main/resources/db/migration/   # Flyway V1-V6
│   └── src/test        # 核心逻辑单元测试
├── frontend/           # Vue3 + TS 前端
├── scripts/            # start-dev.bat / stop-dev.bat 一键启停
├── docs/               # 实施纲要、简历话术
├── docker-compose.yml  # MySQL + Redis（数据卷持久化）
└── .env.example        # 环境变量模板（复制为 .env 使用，.env 不入库）
```

## 快速开始

前置要求：JDK 21、Node.js 18+、Docker Desktop、Maven（或 IntelliJ IDEA 自带）。

```bash
# 1. 环境变量（复制后填入真实值）
cp .env.example .env

# 2. 启动 MySQL + Redis
docker compose up -d

# 3. 后端（任选其一）
#    a) IDEA 打开 backend/ 运行 BackendApplication
#    b) 命令行：cd backend && mvn package && java -jar target/backend-0.1.0-SNAPSHOT.jar
#    （Windows 需先加载 .env：set -a && . ./.env && set +a，或直接用一键脚本）

# 4. 前端
cd frontend && npm install && npm run dev
```

**一键启动（Windows）**：双击 `scripts/start-dev.bat`（自动检查 .env → 起容器 → 起后端 8080 → 起前端 5173）；停止用 `scripts/stop-dev.bat`（数据保留）。

访问：

- 前端页面：http://localhost:5173
- 接口文档（Knife4j）：http://localhost:8080/doc.html
- 默认账号：`admin`，初始密码为 `.env` 中 `MJ_ADMIN_INIT_PASSWORD` 的值（首次启动自动创建）

运行测试：

```bash
cd backend && mvn test
```

## 环境变量（.env）

| 变量 | 说明 |
|---|---|
| MYSQL_ROOT_PASSWORD | MySQL root 密码（Docker） |
| MYSQL_PASSWORD | 应用账号 mianjingge 密码（Docker + 后端） |
| MJ_JWT_SECRET | JWT 签名密钥（HS256，建议 `openssl rand -hex 64` 生成） |
| MJ_ADMIN_INIT_PASSWORD | 预置管理员账号初始密码 |

## 数据隐私

个人面试数据（题目记录、得分、投递信息）仅存储在本机 Docker 数据卷中，`.env` 与数据库内容均不进入 Git，仓库中只有占位模板 `.env.example`。

## 开发规范

- 提交信息：`feat(Mx-y): 描述` / `fix:` / `docs:`
- 模块按实施纲要（docs/实施纲要.md）推进，完成即提交
- 个人数据不入库
