# SNK 后端账号、鉴权与治理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立用户名密码账号、主账户审核、JWT/Refresh Token、密码管理、旧匿名数据认领和后台账号治理，并让所有业务写接口从可信登录上下文取得用户身份。

**Architecture:** 认证领域独立于现有食物与记录领域。Spring Security 解析 JWT 并构造 `CurrentUser`；Refresh Token 数据库存储哈希，轮换时使用行锁，60 秒内保存替代 Token 的 AES-GCM 密文。后台账号接口同时要求 Admin Token 和 OWNER Bearer Token。

**Tech Stack:** Spring Boot 3.5、Spring Security、OAuth2 JOSE、BCrypt、Caffeine、JPA、PostgreSQL、Flyway、JUnit 5、Testcontainers。

## Task 1: 同步正式文档与认证依赖

**Files:**
- Modify: `docs/product/prd.md`
- Modify: `docs/architecture/system-design.md`
- Modify: `docs/architecture/implementation-plan.md`
- Modify: `docs/api/api-contract.md`
- Modify: `docs/database/schema.md`
- Modify: `server/build.gradle`
- Modify: `server/src/main/resources/application.yml`
- Test: `server/src/test/java/com/snk/server/ServerApplicationTests.java`

- [x] 在上述五份文档写入已确认设计中的账号状态、角色、Token 生命周期、匿名认领、单节点限流、无分类和默认公开，并逐份追加变更记录。
- [x] 在 `server/build.gradle` 增加 `spring-boot-starter-security`、`spring-security-oauth2-jose`、`caffeine` 和 `spring-security-test`。
- [x] 在 `application.yml` 只声明环境变量引用与安全默认值，不写入真实密钥：JWT 15 分钟、Refresh 宽限 60 秒、Bootstrap OWNER 和强制重置开关。
- [x] 先在 `ServerApplicationTests` 增加“缺失生产密钥时测试 profile 可启动”的测试；运行 `./gradlew test --tests '*ServerApplicationTests'`，确认新增配置绑定前失败。
- [x] 增加 `AuthProperties`、`OwnerBootstrapProperties` 和测试 profile 配置后重跑同一命令，确认通过。
- [x] 提交并推送：`docs: finalize account authentication contracts`

## Task 2: 数据库迁移与认证持久化模型

**Files:**
- Create: `server/src/main/resources/db/migration/V12__add_account_authentication.sql`
- Create: `server/src/main/resources/db/migration/V13__add_refresh_sessions_and_claims.sql`
- Modify: `server/src/main/java/com/snk/server/infrastructure/persistence/user/UserEntity.java`
- Modify: `server/src/main/java/com/snk/server/infrastructure/persistence/user/UserRepository.java`
- Create: `server/src/main/java/com/snk/server/infrastructure/persistence/auth/RefreshTokenEntity.java`
- Create: `server/src/main/java/com/snk/server/infrastructure/persistence/auth/RefreshTokenRepository.java`
- Create: `server/src/main/java/com/snk/server/infrastructure/persistence/auth/LegacyIdentityClaimEntity.java`
- Create: `server/src/main/java/com/snk/server/infrastructure/persistence/auth/LegacyIdentityClaimRepository.java`
- Create: `server/src/test/java/com/snk/server/infrastructure/persistence/auth/AccountMigrationTests.java`

- [ ] 先写 Testcontainers 测试：用户名大小写不敏感唯一、角色/状态约束、Refresh Token 哈希唯一、匿名身份只能认领一次、最后 OWNER 查询可加锁；运行 `./gradlew test --tests '*AccountMigrationTests'`，预期因迁移和实体不存在而失败。
- [ ] V12 为 `users` 增加 `username`、`password_hash`、`role`、`account_status`、`token_version`、`must_change_password`、`approved_by_user_id`、`approved_at`；保留旧匿名用户字段并允许正式账号字段对匿名旧行为空。
- [ ] V13 建立 `refresh_tokens` 与 `legacy_identity_claims`；Refresh Token 包含 `token_hash`、`family_id`、`device_id`、`rotated_at`、`replacement_ciphertext`、`replacement_expires_at`、`revoked_at`，并创建必要唯一索引与外键。
- [ ] 实体使用枚举 `AccountRole { OWNER, USER }`、`AccountStatus { PENDING, ACTIVE, REJECTED, DISABLED }`；仓库增加 `findByUsernameIgnoreCase`、锁定会话、统计 ACTIVE OWNER 和查找匿名 installationId 的方法。
- [ ] 运行 `./gradlew test --tests '*AccountMigrationTests'` 和 `./gradlew test`，确认真实 PostgreSQL 迁移与既有测试通过。
- [ ] 提交并推送：`feat: add account and refresh session schema`

## Task 3: 注册、审核状态、OWNER 初始化与密码管理

**Files:**
- Create: `server/src/main/java/com/snk/server/domain/auth/AccountRegistrationService.java`
- Create: `server/src/main/java/com/snk/server/domain/auth/BootstrapOwnerService.java`
- Create: `server/src/main/java/com/snk/server/domain/auth/PasswordService.java`
- Create: `server/src/main/java/com/snk/server/api/controller/AuthController.java`
- Create: `server/src/main/java/com/snk/server/api/dto/RegisterRequest.java`
- Create: `server/src/main/java/com/snk/server/api/dto/RegistrationResponse.java`
- Create: `server/src/main/java/com/snk/server/api/dto/PasswordChangeRequest.java`
- Create: `server/src/test/java/com/snk/server/domain/auth/AccountRegistrationServiceTests.java`
- Create: `server/src/test/java/com/snk/server/domain/auth/BootstrapOwnerServiceTests.java`

- [ ] 先写失败测试：注册不接收 `deviceId`、新账号为 `USER/PENDING`、重复用户名返回 409、审批查询票据不可用于业务 API、Bootstrap 只创建首 OWNER、`SNK_OWNER_FORCE_RESET=true` 才能覆盖已有 OWNER 密码。
- [ ] 实现 `POST /api/auth/register`，返回随机高熵且只存哈希的一次性 `approvalTicket`；实现 `GET /api/auth/registration-status?ticket=...`，只返回 PENDING/ACTIVE/REJECTED/DISABLED。
- [ ] BCrypt 强度固定为 12；用户名 trim 后用 `lower(username)` 唯一，密码长度 12–72，错误响应不泄漏账户是否存在（注册重复除外）。
- [ ] 实现 `POST /api/auth/password/change`：验证旧密码、更新哈希、清除 `must_change_password`、递增 `token_version`、撤销该用户全部 Refresh Token。
- [ ] 实现 Bootstrap OWNER 与强制重置审计日志；日志只记录 userId/username/action，不记录密码。
- [ ] 运行 `./gradlew test --tests '*AccountRegistrationServiceTests' --tests '*BootstrapOwnerServiceTests'` 和 `./gradlew test`。
- [ ] 提交并推送：`feat: add registration owner bootstrap and password change`

## Task 4: JWT、Refresh Token 轮换与单机限流

**Files:**
- Create: `server/src/main/java/com/snk/server/domain/auth/JwtTokenService.java`
- Create: `server/src/main/java/com/snk/server/domain/auth/RefreshTokenService.java`
- Create: `server/src/main/java/com/snk/server/domain/auth/AuthService.java`
- Create: `server/src/main/java/com/snk/server/infrastructure/security/TokenCipher.java`
- Create: `server/src/main/java/com/snk/server/infrastructure/security/AuthRateLimiter.java`
- Create: `server/src/main/java/com/snk/server/api/dto/LoginRequest.java`
- Create: `server/src/main/java/com/snk/server/api/dto/RefreshRequest.java`
- Create: `server/src/main/java/com/snk/server/api/dto/TokenPairResponse.java`
- Create: `server/src/test/java/com/snk/server/domain/auth/RefreshTokenServiceTests.java`
- Create: `server/src/test/java/com/snk/server/api/controller/AuthControllerTests.java`

- [ ] 先写失败测试：ACTIVE 可登录；PENDING/REJECTED/DISABLED 返回稳定错误码；JWT 包含 `sub/role/tokenVersion` 且 15 分钟过期；并发刷新只有一个新会话；60 秒内旧 Token 重试返回同一 replacement；超过 60 秒重放撤销 family。
- [ ] `POST /api/auth/login` 强制 `username/password/deviceId`；`POST /api/auth/refresh` 强制 `refreshToken/deviceId`；`POST /api/auth/logout` 撤销当前设备会话。
- [ ] Refresh 原文使用 `SecureRandom` 256 bit，数据库只存 SHA-256 哈希；替代原文只在宽限期内以 AES-256-GCM 密文保存，AAD 绑定 refresh row id、family id 和 device id。
- [ ] 使用 `SELECT ... FOR UPDATE` 串行化轮换；同 device 且在 60 秒内返回解密 replacement 和新 Access Token；其他重放或超时重放撤销整个 family。
- [ ] Caffeine 对登录按 IP+username、注册按 IP 限流；超过限制返回 429 和 `Retry-After`，明确在类注释与文档标记单节点约束。
- [ ] 运行 `./gradlew test --tests '*RefreshTokenServiceTests' --tests '*AuthControllerTests'`、`./gradlew test`。
- [ ] 提交并推送：`feat: add jwt login and rotating refresh sessions`

## Task 5: Spring Security、可信当前用户与旧匿名数据认领

**Files:**
- Create: `server/src/main/java/com/snk/server/infrastructure/security/SecurityConfiguration.java`
- Create: `server/src/main/java/com/snk/server/infrastructure/security/BearerTokenFilter.java`
- Create: `server/src/main/java/com/snk/server/infrastructure/security/CurrentUser.java`
- Create: `server/src/main/java/com/snk/server/domain/auth/LegacyIdentityClaimService.java`
- Create: `server/src/main/java/com/snk/server/api/controller/LegacyIdentityClaimController.java`
- Modify: `server/src/main/java/com/snk/server/api/controller/ApiExceptionHandler.java`
- Create: `server/src/test/java/com/snk/server/infrastructure/security/SecurityConfigurationTests.java`
- Create: `server/src/test/java/com/snk/server/domain/auth/LegacyIdentityClaimServiceTests.java`

- [ ] 先写失败测试：无 Token 访问受保护接口为 401、错误角色为 403、tokenVersion 旧 Token 为 401；认领迁移 records/comments/reports/createdBy；重复认领 409；任一步异常全部回滚。
- [ ] Security 白名单只包含健康检查、注册、注册状态、登录、刷新和静态后台页；`CurrentUser.requiredUserId()` 从 SecurityContext 返回 Long，业务层不再接受客户端伪造身份。
- [ ] `POST /api/auth/legacy-claim` 请求只含旧 128-bit `installationId`；在事务中锁定匿名用户和 claim 标识，迁移 `food_records.user_id`、评论、报告、产品 createdBy，并永久记录认领目标。
- [ ] 对不存在或已认领的 installationId 返回不泄漏其他账户数据的稳定错误；认领成功返回迁移数量，不返回匿名用户资料。
- [ ] 在 `ApiExceptionHandler` 统一输出 `code/message/requestId`，认证错误码至少覆盖 `AUTH_REQUIRED`、`TOKEN_EXPIRED`、`ACCOUNT_PENDING`、`ACCOUNT_REJECTED`、`ACCOUNT_DISABLED`、`MUST_CHANGE_PASSWORD`。
- [ ] 运行安全与认领测试，再运行 `./gradlew test`。
- [ ] 提交并推送：`feat: enforce bearer identity and legacy data claim`

## Task 6: 业务接口去除 userId 信任并保护上传

**Files:**
- Modify: `server/src/main/java/com/snk/server/api/controller/FoodRecordController.java`
- Modify: `server/src/main/java/com/snk/server/api/controller/FoodSearchController.java`
- Modify: `server/src/main/java/com/snk/server/api/controller/FoodFeedbackController.java`
- Modify: `server/src/main/java/com/snk/server/api/controller/RecognitionController.java`
- Modify: `server/src/main/java/com/snk/server/api/controller/UploadController.java`
- Modify: `server/src/main/java/com/snk/server/domain/record/FoodRecordService.java`
- Create: `server/src/main/resources/db/migration/V14__attribute_uploaded_objects.sql`
- Create: `server/src/test/java/com/snk/server/api/controller/AuthenticatedBusinessApiTests.java`

- [ ] 先写 MockMvc 失败测试：请求 body/query 中伪造 userId 不生效；A 不能读改删 B 私密记录；上传无 Token 为 401；上传记录 owner user id。
- [ ] 从创建/更新/删除/历史/评论/反馈/识别 DTO 删除 `userId`；控制器统一调用 `CurrentUser.requiredUserId()`，领域 command 仍显式携带可信 userId 便于测试。
- [ ] V14 建立 `uploaded_objects` 元数据表，至少保存 object key、owner user id、content type、size、createdAt；上传成功与元数据写入失败时删除孤儿对象或整体报错。
- [ ] 搜索接口不再接受可见性 userId；只有 `approved && is_searchable` 条目全局可见，旧匿名私有待审核逻辑保留仅用于历史兼容且不暴露给新 App。
- [ ] 运行 `./gradlew test --tests '*AuthenticatedBusinessApiTests'`、`./gradlew test`，并用 curl 对本地登录→上传→创建记录链路回归。
- [ ] 提交并推送：`refactor: derive business identity from bearer token`

## Task 7: OWNER 后台账号治理与双重保护

**Files:**
- Create: `server/src/main/java/com/snk/server/domain/auth/AccountAdministrationService.java`
- Create: `server/src/main/java/com/snk/server/api/controller/AdminAccountController.java`
- Create: `server/src/main/java/com/snk/server/api/dto/AdminAccountResponse.java`
- Modify: `server/src/main/java/com/snk/server/infrastructure/admin/AdminWebMvcConfigurer.java`
- Modify: `server/src/main/resources/static/admin/index.html`
- Create: `server/src/test/java/com/snk/server/domain/auth/AccountAdministrationServiceTests.java`
- Create: `server/src/test/java/com/snk/server/api/controller/AdminAccountControllerTests.java`

- [ ] 先写失败测试：缺 Admin Token 为 401/403、缺 OWNER Bearer 为 403；approve/reject/disable/enable/promote/demote/revoke-sessions/reset-password 正确；禁止最后 OWNER 禁用或降级；禁止当前 OWNER 自降级。
- [ ] 增加账号列表与操作接口；所有状态与角色修改使用数据库锁，并在同一事务中检查 ACTIVE OWNER 数量。
- [ ] 管理员重置密码时生成一次性高熵临时密码，仅在 HTTPS 响应返回一次；设置 `must_change_password=true`、递增 tokenVersion 并撤销会话。
- [ ] 后台静态页增加 OWNER 登录区和内存态 Access Token；页面刷新后要求重新登录，不把 Token 写入 localStorage/sessionStorage。
- [ ] 后台请求同时发送 `X-SNK-ADMIN-TOKEN` 和 `Authorization: Bearer ...`；Admin Token 为空时生产 profile 拒绝启动后台账号管理能力。
- [ ] 运行两组新增测试、`./gradlew test`，浏览器回归登录、审核、拒绝、重置、最后 OWNER 保护。
- [ ] 提交并推送：`feat: add owner account administration`

## Task 8: 后端总回归与发布准备

**Files:**
- Modify: `docs/architecture/system-design.md`
- Modify: `docs/process/github-workflow.md`
- Create: `server/src/test/java/com/snk/server/AuthEndToEndTests.java`

- [ ] 写端到端 Testcontainers 测试：Bootstrap OWNER→注册→轮询→审核→登录→刷新→改密码→重新登录→认领历史→上传→创建/编辑记录。
- [ ] 运行 `./gradlew clean test`，确认全部通过且日志不包含密码或 Token。
- [ ] 使用 `rg -n "userId" server/src/main/java/com/snk/server/api/dto server/src/main/java/com/snk/server/api/controller` 审查剩余外部身份参数，只允许后台筛选或响应字段。
- [ ] 更新生产环境变量、备份、迁移、回滚和密钥轮换说明，并追加文档变更记录。
- [ ] 仅在 Android 登录链路已完成后，把生产安全策略切换为强制鉴权；在 `https://snk.qiuxinmin.cn` 验证健康、登录、刷新、受保护 API 与后台双 Token。
- [ ] 提交并推送：`test: cover authenticated account lifecycle`

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-07-11 | Codex | 新建后端账号、鉴权与治理实施计划 | 把账号安全设计拆成可测试、可独立提交的服务端任务 |
| 2026-07-11 | Codex | 完成 Task 1 文档契约、认证依赖与配置绑定 | 正式启动必须登录与 OWNER 审核方案的服务端实施 |
