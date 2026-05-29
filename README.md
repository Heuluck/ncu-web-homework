# NCU 在线聊天系统

一个支持私聊、群聊、好友管理、聊天记录查询的实时在线聊天系统，适用于 Web 开发课程设计。

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | HTML + CSS + JavaScript |
| 后端 | Spring Boot 3.x + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 实时通信 | STOMP over WebSocket |
| 认证 | JWT Token |
| 部署 | Docker Compose |

## 项目结构

详见 [AGENTS.md](./AGENTS.md)

> **原型参考：** `preview.html` 是完整的 UI 原型页面，新开发页面可以参考其风格和组件用法。

## 部署方式

### 方式一：Docker 部署（推荐）

需要安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)。

**一键启动：**
```bash
# macOS / Linux
./deploy.sh

# Windows (PowerShell)
.\deploy.ps1
```

或手动执行：
```bash
docker compose up -d --build
```

**访问地址：** http://localhost:8080

**常用命令：**
```bash
docker compose logs -f        # 查看日志
docker compose down            # 停止服务
docker compose restart         # 重启服务
docker compose ps              # 查看状态
```

**配置说明：**

Docker 部署使用根目录 `application.yml`，通过 volume 挂载到容器内。如需修改配置（如数据库密码、JWT 密钥等），直接编辑根目录 `application.yml` 后重启容器。

> ⚠️ 首次启动需删除旧的 MySQL volume 才能重新执行建表：
> ```bash
> docker compose down -v && docker compose up -d --build
> ```

### 方式二：本地开发

需要安装 JDK 17+、Maven、MySQL 8.0。

**1. 配置数据库**

编辑 `backend/src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chat_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password    # 改成你的 MySQL 密码
```

**2. 执行数据库迁移**
```bash
# macOS / Linux
./db/migrate.sh

# Windows (PowerShell)
.\db\migrate.ps1
```

> 需要本地安装 `mysql` 客户端。如果没有，也可以手动登录 MySQL 执行 `db/migration/` 下的 SQL 文件。

**3. 一键启动**
```bash
# macOS / Linux
./start.sh

# Windows PowerShell
.\start.ps1
```

**4. 访问**

http://localhost:8080

## 数据库迁移

每个数据表对应 `db/migration/` 下的一个 SQL 文件，文件名为表名（如 `user.sql`）。

添加新表只需在 `db/migration/` 下新建 `{表名}.sql`，每个文件开头加 `USE chat_system;` 和 `CREATE TABLE IF NOT EXISTS`。

迁移脚本按文件名排序依次执行所有 SQL：
```bash
./db/migrate.sh          # macOS / Linux
.\db\migrate.ps1         # Windows PowerShell
```

## 开发进度

| 模块 | 负责人 | 状态 | 说明 |
|------|--------|------|------|
| A. 用户与认证 | A | ✅ 完成 | 注册/登录/登出/个人资料/文件上传/部署 |
| B. 私聊与实时通信 | B | 🔲 待开发 | WebSocket/私聊消息/已读状态/离线消息 |
| C. 群聊 | C | 🔲 待开发 | 群 CRUD/群成员管理/群消息/群公告 |
| D. 好友与分组 | D | 🔲 待开发 | 好友申请/分组管理/黑名单/好友搜索 |
| E. 消息中心与文件 | E | 🔲 待开发 | 聊天记录查询/导出/图片文件消息/表情 |
| F. 语音与管理后台 | F | 🔲 待开发 | 语音消息/管理员功能/敏感词过滤 |

**基础设施：** ✅ Docker 部署 / 数据库迁移 / 前端设计系统 / JWT 认证
