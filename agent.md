# Agent 注意事项

## 项目信息
- **项目名称**：在线聊天系统
- **技术栈**：Spring Boot 3.x + HTML/CSS/JS + MySQL 8.0
- **架构**：MVC 分层（Controller → Service → Mapper）

## Git 规范
- **分支命名**：`feat|fix|refactor/<功能描述>`（如 `feature/user-auth`、`feature/chat-private`）
- **提交格式**：`feat: xxx` / `fix: xxx` / `docs: xxx`
- **合并流程**：feature → main（需 Code Review）

## 数据库迁移
- **迁移目录**：`db/migration/`
- **命名规则**：`{表名}.sql`（如 `user.sql`、`friend_group.sql`）
- **迁移脚本**：`db/migrate.sh`（按文件名排序依次执行）
- **各成员负责自己的表**，提交到 `db/migration/` 目录

## 目录结构
```
backend/          — Spring Boot 后端
frontend/         — 前端静态资源
docs/             — 文档
db/               — 数据库迁移脚本
  ├── init.sql    — 建库
  ├── migrate.sh  — 迁移脚本
  └── migration/  — 各表 SQL
```

## 注意事项
- 每张表的 SQL 文件必须加 `USE chat_system;` 和 `IF NOT EXISTS`
- 前端放在 `backend/src/main/resources/static/`，同域部署
- 统一响应格式：`{ code, message, data }`
- 认证方式：JWT Token（Header: `Authorization: Bearer <token>`）

## 相关文档
- **PRD**：`docs/PRD-在线聊天系统.md`
- **分工方案**：`docs/分工方案.md`
- **技术方案**：`docs/技术方案.md`
- **实施计划**：`docs/superpowers/plans/`

## 推荐开发流程

```
┌─────────────────────────────────────────────────────────────┐
│                    推荐开发流程                               │
└─────────────────────────────────────────────────────────────┘

1️⃣  需求阶段 → brainstorming
   ├─ 理解需求
   ├─ 提出 2-3 种方案
   ├─ 分段呈现设计，逐段确认
   └─ 输出设计文档 → docs/superpowers/specs/

2️⃣  计划阶段 → writing-plans
   ├─ 拆解为可执行的 Task
   ├─ 每个 Task 包含具体代码和步骤
   └─ 输出实施计划 → docs/superpowers/plans/

3️⃣  执行阶段 → subagent-driven-development（推荐）
   ├─ 每个 Task 分派独立子代理
   ├─ 执行完一个 Task → review → 下一个
   └─ 或用 executing-plans 批量执行

4️⃣  验证阶段 → verification-before-completion
   ├─ 检查是否符合原始需求
   ├─ 测试关键功能
   └─ 确认无遗漏

5️⃣  收尾阶段 → finishing-a-development-branch
   ├─ 代码整理
   ├─ 提交 PR
   └─ 合并到 main
```
