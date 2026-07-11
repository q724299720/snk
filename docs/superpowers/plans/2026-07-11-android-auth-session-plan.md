# SNK Android 登录、会话与草稿隔离实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除游客模式，完成账号注册登录、长期会话自动刷新、审核轮询、密码修改/退出、旧数据认领和多账号草稿隔离。

**Architecture:** 使用独立 public Retrofit 调认证接口，authenticated Retrofit 通过 Bearer Interceptor 与串行 Refresh Authenticator 访问业务 API。Refresh Token 存 Android Keystore 加密存储；Room 草稿按登录 userId 分区，WorkManager 只同步当前账号。

**Tech Stack:** Kotlin、Jetpack Compose、Navigation Compose、Retrofit、OkHttp、Coroutines/Mutex、Room、WorkManager、Android Keystore、JUnit、MockWebServer、Compose UI Test。

## Task 1: 认证 DTO、API 与安全凭据存储

**Files:**
- Modify: `android-app/app/build.gradle.kts`
- Create: `android-app/app/src/main/java/com/snk/app/data/auth/AuthApi.kt`
- Create: `android-app/app/src/main/java/com/snk/app/data/auth/AuthModels.kt`
- Create: `android-app/app/src/main/java/com/snk/app/data/auth/AuthRepository.kt`
- Create: `android-app/app/src/main/java/com/snk/app/data/auth/SecureTokenStore.kt`
- Create: `android-app/app/src/test/java/com/snk/app/data/auth/AuthRepositoryTest.kt`
- Create: `android-app/app/src/androidTest/java/com/snk/app/data/auth/SecureTokenStoreTest.kt`

- [ ] 先写 MockWebServer 失败测试，固定 `register/login/refresh/logout/me/password/change/legacy-claim` JSON 字段与错误码映射。
- [ ] `AuthApi.register` 不发送 deviceId；`login/refresh` 必须发送稳定 deviceId；注册响应保存 approvalTicket，但不能把它当 Bearer Token。
- [ ] 使用 Android Keystore AES-GCM 加密 Refresh Token；普通 DataStore 只保存 userId、username、role、deviceId、会话状态和密文版本，不保存 Token 原文。
- [ ] Access Token 仅保存在内存；任何 `toString`、异常和网络日志都必须脱敏 Authorization、password、refreshToken、approvalTicket。
- [ ] 运行 `./gradlew testDebugUnitTest --tests '*AuthRepositoryTest'` 和连接设备上的 `connectedDebugAndroidTest` 指定安全存储测试。
- [ ] 提交并推送：`feat(android): add auth api and secure token storage`

### Task 1 实施状态（2026-07-11）

已完成全部认证端点 DTO 与 Retrofit 契约、稳定 `deviceId`、认证错误码映射、仅内存 Access Token，以及 Android Keystore AES-GCM 加密的 Refresh Token/审批票据存储。普通 DataStore 只保存账号元数据、设备 ID、会话状态和密文版本；任何认证模型的 `toString()` 均不输出密码或凭据原文。Task 2 可在此基础上接入双客户端、Bearer Interceptor 和并发安全刷新。

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-07-11 | Codex | 标记 Android 认证计划 Task 1 完成 | 为自动刷新和登录界面提供可验证、不可明文落盘的认证数据层 |

## Task 2: 自动 Bearer 与并发安全刷新

**Files:**
- Create: `android-app/app/src/main/java/com/snk/app/data/auth/AuthenticatedSessionManager.kt`
- Create: `android-app/app/src/main/java/com/snk/app/data/auth/BearerTokenInterceptor.kt`
- Create: `android-app/app/src/main/java/com/snk/app/data/auth/RefreshTokenAuthenticator.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/data/AppContainer.kt`
- Create: `android-app/app/src/test/java/com/snk/app/data/auth/RefreshTokenAuthenticatorTest.kt`

- [ ] 先写失败测试：多个并发 401 只调用 refresh 一次；原请求只重放一次；刷新失败清理会话；60 秒宽限返回的 replacement 被原子覆盖到安全存储。
- [ ] `AppContainer` 建立不带 Authenticator 的 public client 和带 Bearer/Authenticator 的 business client，防止 refresh 自递归。
- [ ] `AuthenticatedSessionManager` 用 `Mutex` 双重检查 Access Token 版本；刷新成功先持久化新 Refresh Token，再发布新 Access Token。
- [ ] Authenticator 对同一 request 的 `responseCount >= 2` 返回 null；PENDING/REJECTED/DISABLED/MUST_CHANGE_PASSWORD 转换为明确会话状态。
- [ ] 网络日志在 debug 构建也调用 `redactHeader("Authorization")`，并禁用认证请求 body 日志。
- [ ] 运行新增测试和 `./gradlew testDebugUnitTest`。
- [ ] 提交并推送：`feat(android): add persistent authenticated sessions`

### Task 2 实施状态（2026-07-11）

已完成 public/business 双 OkHttp/Retrofit 客户端、自动 Bearer、基于 `Mutex` 双重检查的并发 Refresh 和单次请求重放。刷新成功严格先持久化替代 Refresh Token，再发布内存 Access Token；刷新失败清除持久会话并映射为 `PENDING / REJECTED / DISABLED / MUST_CHANGE_PASSWORD / SIGNED_OUT`。认证客户端不安装 Authenticator，避免 refresh 自递归；日志固定为 BASIC 并显式脱敏 Authorization。

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-07-11 | Codex | 标记 Android 认证计划 Task 2 完成 | 建立长期默认登录所需的并发安全自动刷新与业务请求鉴权基础 |

## Task 3: 登录、注册与审核等待界面

**Files:**
- Create: `android-app/app/src/main/java/com/snk/app/ui/auth/LoginScreen.kt`
- Create: `android-app/app/src/main/java/com/snk/app/ui/auth/RegisterScreen.kt`
- Create: `android-app/app/src/main/java/com/snk/app/ui/auth/PendingApprovalScreen.kt`
- Create: `android-app/app/src/main/java/com/snk/app/ui/auth/AuthViewModel.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/SnkApp.kt`
- Create: `android-app/app/src/androidTest/java/com/snk/app/ui/auth/AuthFlowTest.kt`

- [ ] 先写 Compose 失败测试：未登录只见登录/注册；PENDING 每 10 秒轮询且 onResume 立即查；ACTIVE 引导登录；REJECTED 停止轮询并显示明确原因；密码不回显。
- [ ] 用顶层 `AuthUiState` 取代 `AnonymousAuth` 的 `SessionUiState`：Restoring、SignedOut、Pending、Rejected、MustChangePassword、Authenticated、Disabled。
- [ ] 启动时没有 Refresh Token 直接登录；有 Refresh Token 先静默 refresh，成功后进入首页，失败后清凭据并进入登录。
- [ ] Pending 页只在 STARTED/RESUMED 生命周期轮询，进入后台立即取消 coroutine；用户手动“重新检查”与自动轮询共用同一防并发方法。
- [ ] 登录错误不区分“用户名不存在”和“密码错误”；TalkBack 正确读出输入错误、审核状态和主要操作。
- [ ] 运行 Compose 测试、`testDebugUnitTest`、`assembleDebug`，ADB 覆盖注册→待审核→后台审核→登录。
- [ ] 提交并推送：`feat(android): require login and approval`

## Task 4: 我的页账号、修改密码与主动退出

**Files:**
- Modify: `android-app/app/src/main/java/com/snk/app/ui/ProfileScreen.kt`
- Create: `android-app/app/src/main/java/com/snk/app/ui/auth/ChangePasswordScreen.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/SnkApp.kt`
- Create: `android-app/app/src/androidTest/java/com/snk/app/ui/auth/AccountSettingsTest.kt`

- [ ] 先写失败测试：我的页显示 username/role/同步状态；修改密码校验旧密码和确认密码；成功后清会话并回登录；退出撤销当前 Refresh Token 并清本地凭据。
- [ ] 移除游客模式、installationId 和 user_id 的普通可见展示；installationId 仅保留在隐藏诊断区与一次性认领流程。
- [ ] 修改密码提交成功后无论 logout 网络请求是否成功，都本地清 Token；网络失败提示“本机已退出，服务器会话将在下次鉴权时失效”并记录不含凭据的诊断事件。
- [ ] 主动退出不删除其他账号草稿；返回登录页并清除导航 back stack，系统返回键不能回到业务页。
- [ ] 运行新增测试、全量 Android 测试与 APK 构建。
- [ ] 提交并推送：`feat(android): add account settings and logout`

## Task 5: Room v5 多账号草稿隔离

**Files:**
- Modify: `android-app/app/src/main/java/com/snk/app/data/local/FoodRecordDraftEntity.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/data/local/FoodRecordDraftDao.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/data/local/SnkDatabase.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/data/draft/DraftRecordRepository.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/sync/DraftSyncWorker.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/sync/DraftSyncScheduler.kt`
- Create: `android-app/app/src/androidTest/java/com/snk/app/data/local/DraftOwnerMigrationTest.kt`
- Create: `android-app/app/src/test/java/com/snk/app/sync/DraftSyncWorkerTest.kt`

- [ ] 先写失败测试：v4→v5 保留草稿并写入 owner；A 登录只查询/上传 A 草稿；B 登录不显示、不删除、不上传 A 草稿；无当前登录用户时 Worker retry/暂停而不提交。
- [ ] 增加非空 `draft_owner_user_id` 与 `(draft_owner_user_id, updated_at)` 索引；新草稿创建时从 `AuthenticatedSessionManager.requireUserId()` 固化 owner。
- [ ] 旧草稿迁移先写入保留的旧匿名 userId 标识，不直接归属当前正式账号；认领成功后单独事务更新为目标 userId。
- [ ] DAO 的可见列表、待上传列表、计数、去重和删除都必须带 ownerUserId；禁止保留无 owner 的全表同步查询。
- [ ] Worker 启动与每条提交前重新检查当前 userId；401 停止该次执行并等待重新登录，不把草稿改为不可恢复 FAILED。
- [ ] 运行迁移测试、Worker 测试、全量测试和 `assembleDebug`。
- [ ] 提交并推送：`feat(android): isolate drafts by account owner`

## Task 6: 一次性历史数据认领

**Files:**
- Create: `android-app/app/src/main/java/com/snk/app/data/auth/LegacyClaimCoordinator.kt`
- Create: `android-app/app/src/main/java/com/snk/app/ui/auth/LegacyClaimDialog.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/data/auth/InstallationIdStore.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/SnkApp.kt`
- Create: `android-app/app/src/test/java/com/snk/app/data/auth/LegacyClaimCoordinatorTest.kt`

- [ ] 先写失败测试：只对升级安装且存在旧 installationId 展示一次；用户可暂不认领；成功后本地草稿 owner 原子迁移；409 已认领后不重复弹窗；网络失败可重试。
- [ ] 登录成功后由 coordinator 检查 `legacyClaimResolved` 和旧身份存在性；弹窗清楚说明“本机历史记录将归入当前账号，完成后不可更改”。
- [ ] 服务端认领成功后，在 Room 事务中把旧匿名 owner 的草稿更新到当前 userId，再写 `legacyClaimResolved=true`；若本地迁移失败则保留可恢复标记并下次补做，不重复调用服务端认领。
- [ ] 用户选择“暂不”只关闭当前提示，不销毁 installationId；在我的页提供“认领本机历史数据”入口直到成功、已认领或明确放弃。
- [ ] 不显示其他账号或匿名用户的记录数量、昵称等探测信息。
- [ ] 运行新增测试、全量测试、APK 与 ADB 升级安装认领测试。
- [ ] 提交并推送：`feat(android): claim legacy anonymous history`

## Task 7: Android 鉴权端到端回归

**Files:**
- Modify: `docs/architecture/implementation-plan.md`
- Create: `android-app/app/src/androidTest/java/com/snk/app/AuthEndToEndTest.kt`

- [ ] 运行 `./gradlew clean testDebugUnitTest assembleDebug` 和 `connectedDebugAndroidTest`。
- [ ] ADB 测试冷启动、登录保持、杀进程恢复、Access Token 过期刷新、弱网刷新丢响应、主动退出、A/B 草稿隔离、待审核 onResume/10 秒轮询。
- [ ] 用 `adb logcat` 搜索 `Bearer|refreshToken|password|approvalTicket`，确认没有凭据原文。
- [ ] 正式域名测试注册、待审核、OWNER 审核、登录、上传、创建、修改密码和退出。
- [ ] 记录 APK 路径、版本、设备型号、Android 版本与测试结果；阻塞项写入实施计划变更记录。
- [ ] 提交并推送：`test(android): cover authenticated app lifecycle`

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-07-11 | Codex | 新建 Android 登录、会话与草稿隔离实施计划 | 把必须登录、长期会话和多账号数据隔离拆成可验证任务 |
