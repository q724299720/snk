# SNK 账号鉴权与产品简化总实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不引入 Redis、消息队列或第三方登录的前提下，把 SNK 升级为必须登录、主账户审核、长期会话的正式账号制，并完成无分类、图片展示、记录编辑、默认公开和产品自动审核。

**Architecture:** 先扩展 PostgreSQL 与 Spring Security 鉴权边界，再接入 Android 会话与多账号草稿隔离，最后切换业务接口身份来源并完成产品体验改造。服务端继续单节点部署；Access Token 使用 15 分钟 JWT，Refresh Token 使用数据库轮换、哈希存储和 60 秒弱网宽限。

**Tech Stack:** Java 21、Spring Boot 3.5、Spring Security、Spring OAuth2 JOSE、Caffeine、PostgreSQL/Flyway、Kotlin、Jetpack Compose、Room、WorkManager、Retrofit/OkHttp、Android Keystore。

## Global Constraints

- [ ] 每个任务开始前重新阅读 `agents.md` 和本总计划，并检查依赖计划是否完成。
- [ ] 严格执行 TDD：先新增失败测试，再做最小实现，再运行模块回归。
- [ ] 每个任务独立提交并推送；提交信息使用计划中给出的建议文本。
- [ ] 每个增量都执行服务端测试、Android 单元/Compose 测试、APK 构建、ADB 真机测试、正式域名网页/API 检查；无法执行时记录具体阻塞。
- [ ] 不记录密码、Access Token、Refresh Token、Admin Token 或替代 Refresh Token 密文到日志。
- [ ] 不新增游客入口、每日 15:00 定时任务、Redis、邮件、短信、第三方登录或自助找回密码。

## Execution Order

1. [后端账号、鉴权与治理计划](2026-07-11-backend-account-authentication-plan.md)
2. [Android 登录、会话与草稿隔离计划](2026-07-11-android-auth-session-plan.md)
3. [产品简化、图片与记录编辑计划](2026-07-11-product-simplification-and-media-plan.md)

后端计划的 Task 1–5 必须先完成，但 Task 6 的“全量强制鉴权切换”要等 Android 计划 Task 1–5 已能登录和自动刷新后一起发布，避免旧 APK 瞬间不可用。产品计划依赖前两份计划完成可信用户身份接入。

## Release Gates

- [ ] 数据库备份完成，Flyway 在真实 PostgreSQL 上从 V1 完整迁移到最新版本。
- [ ] 已配置 `SNK_JWT_PRIVATE_KEY`、`SNK_JWT_PUBLIC_KEY`、`SNK_TOKEN_ENCRYPTION_KEY`、`SNK_OWNER_USERNAME`、`SNK_OWNER_PASSWORD` 和非空 `SNK_ADMIN_API_TOKEN`。
- [ ] 首个 OWNER 可登录，普通用户注册后只能进入待审核页，审核后可登录。
- [ ] Refresh Token 丢响应后在 60 秒内重试得到同一替代会话；超过宽限重放会撤销会话链。
- [ ] 旧匿名数据只能认领一次；A/B 账号草稿不会串号。
- [ ] 搜索、发现、记录、上传、评论、点赞、反馈接口均以 Token 用户为准，不信任请求中的 `userId`。
- [ ] 所有新产品为 `approved + is_searchable=true + category=none`，新记录默认公开。
- [ ] 正式域名、后台页和 APK 完成一轮端到端回归后才部署生产。

## Documentation Sync

实施每个子计划时同步更新：

- `docs/product/prd.md`
- `docs/architecture/system-design.md`
- `docs/architecture/implementation-plan.md`
- `docs/api/api-contract.md`
- `docs/database/schema.md`
- `docs/process/github-workflow.md`（仅部署或环境变量流程发生变化时）
- `agents.md`（仅顶层结论、顺序或索引发生变化时）

每份被修改文档都必须追加“变更记录”。

## Plan Completion Checklist

- [ ] 三份子计划的全部任务已勾选。
- [ ] 检查四份计划不存在未决占位文本、模糊路径或未定义接口。
- [ ] 核对 DTO 字段名、数据库列名、Android 序列化名在三份计划中一致。
- [ ] 执行最终验证命令并保存输出摘要到最后一个提交说明。

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-07-11 | Codex | 新建账号鉴权与产品简化总实施计划 | 将已确认设计拆分为有依赖顺序和发布门禁的执行入口 |
