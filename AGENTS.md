# Agent 注意事项

## 项目信息
- **项目名称**：在线聊天系统
- **技术栈**：Spring Boot 3.x + HTML/CSS/JS + MySQL 8.0
- **架构**：MVC 分层（Controller → Service → Mapper）

## 重要信息
前端代码严禁修改backend/target/classes下的文件！这些是编译输出，修改会被覆盖。前端资源请放在backend/src/main/resources/static/目录下。

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
   ├─ 计划末尾生成完整的端到端测试用例（API + 前端 E2E）
   └─ 输出实施计划 → docs/superpowers/plans/

3️⃣  执行阶段 → subagent-driven-development（推荐）
   ├─ 每个 Task 分派独立子代理
   ├─ 执行完一个 Task → review → 下一个
   └─ 或用 executing-plans 批量执行

4️⃣  验证阶段 → verification-before-completion
   ├─ 检查是否符合原始需求
   ├─ API 测试（curl 验证所有接口）
   ├─ 前端 E2E 测试（推荐 agent-browser）
   └─ 确认无遗漏

5️⃣  收尾阶段 → finishing-a-development-branch
   ├─ 代码整理
   ├─ 提交 PR
   └─ 合并到 main
```

## 前端开发规范

### 设计系统
- **设计 Token**：`css/variables.css`（颜色、字体、间距、圆角、阴影）
- **通用组件**：`css/components.css`（按钮、输入框、卡片、头像、消息气泡等）
- **页面样式**：`css/style.css`（布局、页面级样式）

### 如何保证 UI 风格统一

1. **使用 CSS 变量**
   - 颜色：使用 `var(--color-primary)` 而非硬编码 `#27272a`
   - 间距：使用 `var(--space-4)` 而非硬编码 `16px`
   - 字体：使用 `var(--font-sans)` 而非硬编码字体名

2. **使用通用组件类**
   - 按钮：`.btn`、`.btn-primary`、`.btn-secondary`、`.btn-ghost`
   - 输入框：`.input-control`、`.input-group`
   - 卡片：`.card`
   - 头像：`.avatar`、`.avatar-sm`、`.avatar-md`
   - 列表项：`.list-item`

3. **图标和头像**
   - 图标库：Lucide Icons（`<i data-lucide="icon-name"></i>`）
   - 头像：DiceBear API（`https://api.dicebear.com/7.x/avataaars/svg?seed=xxx`）

4. **新建页面模板**
   ```html
   <!DOCTYPE html>
   <html lang="zh-CN">
   <head>
     <meta charset="UTF-8">
     <meta name="viewport" content="width=device-width, initial-scale=1.0">
     <title>页面标题 - 在线聊天系统</title>
     <link rel="stylesheet" href="/css/variables.css">
     <link rel="stylesheet" href="/css/components.css">
     <link rel="stylesheet" href="/css/style.css">
     <script src="https://unpkg.com/lucide@latest"></script>
   </head>
   <body>
     <!-- 内容 -->
     <script>lucide.createIcons();</script>
   </body>
   </html>
   ```

5. **原型参考**
   - `preview.html` 是完整的 UI 原型，新开发页面可以参考其风格和组件用法
   - 所有新页面的 CSS 类名、布局结构、交互模式以 preview.html 为准

## 测试与验收规范

### 单测要求
- **每个 Service 实现类**必须有对应的单元测试
- **每个 Controller** 必须有集成测试（MockMvc）
- 测试覆盖核心业务逻辑：正常流程 + 异常流程
- 测试文件放在 `src/test/java/com/ncu/chat/` 对应包下
- 命名规范：`XxxServiceTest.java`、`XxxControllerTest.java`

### 自测流程
1. **启动应用**：确保 `mvn spring-boot:run` 能正常启动
2. **API 测试**：用 Postman 或 curl 测试所有接口
3. **前端 E2E 测试**：使用 agent-browser 自动化测试（推荐）
4. **异常测试**：测试错误输入、未登录、权限不足等场景

### Agent-Browser 端到端测试（推荐）

前端功能开发完成后，推荐用 agent-browser 做端到端验证。

**标准流程：**
1. 打开页面：`agent-browser open http://localhost:8080/xxx.html`
2. 查看结构：`agent-browser snapshot -i`（获取元素 ref）
3. 截图验证：`agent-browser screenshot /tmp/xx.png`
4. 交互操作：`agent-browser fill @e2 "内容"` / `agent-browser click @e4`
5. 验证结果：`agent-browser get url` 或重新 `snapshot`
6. 关闭：`agent-browser close`

**注意事项：**
- 每次页面变化后必须重新 `snapshot` 获取最新 ref
- 截图保存到 `/tmp/` 目录便于查看
- 环境：`export PATH="$HOME/.nvm/versions/node/v24.14.1/bin:$PATH"`

### Agent 使用约束
使用 Agent 开发时，**必须**在 prompt 中强调：
```
请为 xxx 编写单元测试，覆盖：
1. 正常流程
2. 异常流程（参数错误、权限不足等）
3. 边界情况
```

### 提交前检查
- [ ] 单测通过：`mvn test`
- [ ] 应用能正常启动
- [ ] API 接口手动测试通过
- [ ] 前端页面功能正常
