# SNK 账号鉴权与产品简化设计

## 1. 背景与目标

本轮在现有快速记录改版基础上继续收敛产品模型，并把安装级游客身份升级为正式账号体系。

目标如下：

- App 必须登录后才能使用，不再提供游客模式。
- 支持用户名、密码注册与登录，登录状态持续到用户主动退出或服务端撤销会话。
- 普通账号注册后必须由主账户审核通过才能登录。
- 后台可管理主账户、普通账户、账号状态和有效设备会话。
- 客户端和后台不再展示或要求产品分类。
- 用户新建产品后自动审核通过并立即全局可搜索。
- 新建记录默认公开，每次新增、修改和删除立即写入数据库，不设置每日定时提交。
- 首页搜索、发现和记录页正确展示图片；记录页点击名称或图片进入本次记录编辑页。

## 2. 明确决策

### 2.1 登录与账号

- 认证方式固定为用户名和密码。
- App 取消游客入口，未登录时只能访问注册、登录和等待审核页面。
- 普通账号注册状态固定为 `PENDING`，主账户审核后变为 `ACTIVE`。
- 账号禁用状态为 `DISABLED`，禁用后现有 Token 立即失效。
- 角色固定为 `OWNER` 和 `USER`。
- 第一个主账户由服务器环境变量初始化，仅在系统不存在主账户时创建。
- 系统不得禁用或降级最后一个主账户。
- 主账户不得自行降级，必须由另一个主账户操作。
- 不实现游客数据合并，因为游客模式在本轮被完全移除。
- 不实现手机号、邮箱、短信验证码、找回密码和第三方登录。

### 2.2 Token

- 使用短期 JWT Access Token 和长期、可撤销、可轮换的 Refresh Token。
- Access Token 用于 App API 的 Bearer 鉴权。
- Refresh Token 原文只返回客户端一次，服务端只保存哈希；不设置固定到期日，在主动退出、密码修改、账号禁用、后台撤销或安全轮换失败前保持登录。
- Android 使用系统加密存储保存 Refresh Token，不把 Token 写入普通 DataStore、日志或崩溃信息。
- Access Token 过期时客户端自动刷新并重放原请求一次。
- 用户主动退出时撤销当前设备 Refresh Token，并清理本地凭据。
- 修改密码、禁用账号和后台强制退出时递增 `token_version`，使旧 Access Token 立即失效，并撤销相关 Refresh Token。
- 并发刷新采用 Refresh Token 轮换和单次使用约束，只允许一条会话链继续有效。

### 2.3 分类与审核

- Android 和后台不显示分类、子分类及待分类状态。
- 数据库暂时保留 `food_items.category` 和 `subcategory` 以兼容历史结构。
- 迁移把历史 `category` 统一更新为 `none`、`subcategory` 更新为 `NULL`。
- 所有新建和编辑产品统一写入 `category=none`、`subcategory=NULL`。
- 新产品直接写入 `audit_status=approved`，立即进入全局搜索。
- 移除待分类筛选、数量指标和用户侧待审核标签。
- 后台继续保留产品编辑、驳回、恢复和合并能力。

### 2.4 记录和图片

- 新记录默认 `is_public=true`，用户仍可主动改为私密。
- 每次新增、编辑、删除、点赞、评论和图片变更立即请求服务端并落库。
- 不新增每日 15:00 心跳或定时数据提交任务。
- 图片上传接口要求登录，从 Token 识别上传者并记录归属。
- 搜索结果优先显示产品主图，无图显示统一占位。
- 发现页优先显示记录图片，其次产品主图，无图显示占位。
- 记录页点击产品名称或图片进入本次记录编辑页，可修改评分、备注、公开状态和记录图片。
- 新记录首张图片可以在产品无主图时补为产品主图。
- 离线草稿继续使用稳定 `clientRequestId`；登录失效时暂停补传，重新登录后继续补传。

## 3. 系统架构

### 3.1 服务端模块

新增独立认证边界：

- `AuthController`：注册、登录、刷新、退出、当前账号。
- `AuthService`：密码校验、Token 签发、轮换与撤销。
- `JwtTokenService`：Access Token 创建和验证。
- `RefreshTokenService`：Refresh Token 哈希、单次轮换和设备会话管理。
- `AccountAdministrationService`：审核、禁用、恢复、角色调整、强制退出。
- `BootstrapOwnerService`：首个主账户初始化。
- `BearerTokenFilter`：解析 Token 并向 Spring Security 上下文写入当前账号。
- `CurrentUser`：控制器获取可信用户身份的统一入口。

除公开健康检查、注册、登录和刷新外，App API 默认需要认证。后台原有 `SNK_ADMIN_API_TOKEN` 继续保护 `/api/admin/**` 的网络入口，账户管理接口还要求当前登录账号角色为 `OWNER`。后台静态页增加主账户登录区：Admin Token 用于通过网络入口保护，主账户 Access Token 用于业务授权；两者缺一不可。

### 3.2 Android 模块

新增：

- `AuthApi` 和 `AuthRepository`。
- `SecureTokenStore`。
- `AuthenticatedSessionManager`。
- OkHttp Bearer Token 拦截器和单次刷新 Authenticator。
- `LoginScreen`、`RegisterScreen`、`PendingApprovalScreen`。
- `AccountManagementScreen` 仍位于后台网页，不在普通 App 暴露。

`SnkApp` 启动时先恢复会话：

1. 没有 Refresh Token，进入登录页。
2. 有 Refresh Token，调用刷新接口。
3. 刷新成功，进入首页。
4. 账号待审核，进入等待审核页。
5. 账号禁用或 Refresh Token 无效，清理凭据并进入登录页。

## 4. 数据模型

### 4.1 users

在现有 `users` 表增加或调整：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `username` | `VARCHAR(64)` | 大小写不敏感唯一，正式账号非空 |
| `password_hash` | `VARCHAR(255)` | 正式账号非空，不保存明文 |
| `role` | `VARCHAR(16)` | `OWNER / USER` |
| `account_status` | `VARCHAR(16)` | `PENDING / ACTIVE / DISABLED` |
| `token_version` | `INTEGER` | 默认 `0` |
| `approved_by_user_id` | `BIGINT` | 可空，自关联 `users.id` |
| `approved_at` | `TIMESTAMPTZ` | 可空 |
| `last_login_at` | `TIMESTAMPTZ` | 可空 |

旧匿名用户数据不删除，用兼容状态保留历史关联，但不得再通过 App 创建新的匿名用户。匿名初始化接口在兼容窗口结束后移除。

用户名规范化采用去首尾空白并小写化，数据库使用唯一索引保证并发注册一致性。显示时保留用户提交的原始大小写用户名。

### 4.2 refresh_tokens

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `UUID` | 主键 |
| `user_id` | `BIGINT` | 所属账号 |
| `token_hash` | `VARCHAR(128)` | 唯一，保存 SHA-256/HMAC 哈希 |
| `device_id` | `VARCHAR(128)` | App 安装级设备标识 |
| `issued_at` | `TIMESTAMPTZ` | 签发时间 |
| `last_used_at` | `TIMESTAMPTZ` | 最近轮换时间 |
| `revoked_at` | `TIMESTAMPTZ` | 撤销时间 |
| `replaced_by_token_id` | `UUID` | 轮换后的 Token |

### 4.3 account_audit_logs

记录账号审核、禁用、恢复、角色调整、强制退出和密码修改。日志保存操作者、目标账号、动作、前后状态、时间和可选备注。

### 4.4 food_items 与上传资源

- 分类兼容字段保留但统一为 `none/NULL`。
- 新建产品直接 `approved`。
- 上传资源增加 `uploaded_by_user_id` 或等价归属字段，禁止未认证上传。

## 5. 接口设计

### 5.1 认证接口

#### POST `/api/auth/register`

请求：`username`、`password`、`deviceId`。

响应：账号 ID、用户名、`role=USER`、`accountStatus=PENDING`。注册不签发 Token。

#### POST `/api/auth/login`

请求：`username`、`password`、`deviceId`。

- `PENDING` 返回 `403 ACCOUNT_PENDING_APPROVAL`。
- `DISABLED` 返回 `403 ACCOUNT_DISABLED`。
- 成功返回 Access Token、Refresh Token 和账号摘要。

#### POST `/api/auth/refresh`

请求：Refresh Token、设备 ID。成功后轮换并返回一组新 Token；旧 Refresh Token 立即失效。

#### POST `/api/auth/logout`

要求 Bearer Token，撤销当前设备 Refresh Token。

#### GET `/api/auth/me`

返回当前账号 ID、用户名、角色和状态。

### 5.2 后台账户接口

- `GET /api/admin/accounts?status=&role=&q=`。
- `POST /api/admin/accounts/{id}/approve`。
- `POST /api/admin/accounts/{id}/disable`。
- `POST /api/admin/accounts/{id}/enable`。
- `POST /api/admin/accounts/{id}/promote-owner`。
- `POST /api/admin/accounts/{id}/demote-owner`。
- `POST /api/admin/accounts/{id}/revoke-sessions`。
- `GET /api/admin/accounts/{id}/audit-logs`。

### 5.3 现有接口调整

- App 写接口删除或忽略请求体中的 `userId`，统一从 Token 获取用户。
- 记录列表、评论、点赞、产品创建、图片上传和草稿补传均使用当前认证账号。
- `POST /api/records/quick` 默认 `isPublic=true`，新建产品自动 `approved`。
- 产品和记录响应不再要求客户端消费分类字段；兼容期可保留字段但固定为 `none`。

## 6. 页面与交互

### 6.1 登录与注册

- 登录页包含用户名、密码、登录、注册入口和密码可见性切换。
- 注册页提供用户名、密码、确认密码和密码规则提示。
- 注册成功进入等待审核页，不自动登录。
- 等待审核页允许重新检查状态和返回登录。
- 登录错误使用明确错误文案，不泄露“用户名存在但密码错误”等敏感差异。

### 6.2 我的

登录后展示用户名、账号角色、同步状态、隐私说明、问题反馈和退出登录。

诊断区继续隐藏展示用户 ID、设备 ID 和 Token 到期时间，但不得显示 Token 原文。

### 6.3 搜索、发现和记录

- 搜索卡展示产品主图、名称、品牌和评分，不展示分类。
- 发现卡展示大图、名称、评分、备注、点赞和评论，不展示分类。
- 记录页产品名称和图片具备统一点击语义，打开记录编辑页。
- 记录编辑成功后返回记录页并刷新首页最近记录和发现内容。

## 7. 错误处理

- `401`：Access Token 无效时仅自动刷新一次；再次失败清理登录态。
- `403 ACCOUNT_PENDING_APPROVAL`：显示等待审核页。
- `403 ACCOUNT_DISABLED`：停止同步并显示账号被禁用。
- 网络失败：登录/注册保留输入；记录写操作继续保存本地草稿。
- 图片上传失败：保留应用私有目录副本，允许重试或移除图片后保存。
- Refresh Token 重放：撤销整条会话链并要求重新登录。
- 最后一个主账户保护失败返回明确业务错误，不执行部分更新。

## 8. 安全约束

- 密码最少 8 位，服务端统一校验并使用 BCrypt（cost 12）哈希。
- 日志不得记录密码、Access Token、Refresh Token 或完整认证请求体。
- 登录和注册接口进行基于 IP 与用户名的轻量限流；不引入 Redis，使用单机内存限流并记录失败审计。
- JWT 签名密钥、首个主账户密码和 Admin Token 只通过服务器环境变量提供。
- 生产环境启动时若 JWT 密钥或首个主账户初始化配置不安全，启动失败并给出配置错误。
- 所有账号管理动作要求 Admin Token 和 `OWNER` 双重验证。
- Token 中包含用户 ID、角色、账号状态版本和 `token_version`；服务端对敏感写操作校验当前数据库状态。

## 9. 数据迁移与发布

1. 备份 PostgreSQL 和上传目录。
2. Flyway 新增账号、Refresh Token、账号审计和分类归一化迁移。
3. 配置 JWT 密钥、首个主账户用户名和初始密码。
4. 首次启动创建主账户，并确认后台可登录和审核账号。
5. 发布要求登录的 Android APK。
6. Android 新版上线后停止匿名账号创建接口。
7. 旧匿名记录保留，不自动归属到新账号。

发布期间服务端先兼容旧客户端只读请求，但新写请求必须逐步切换到 Token；最终关闭匿名写入。

## 10. 测试与验收

### 服务端

- 注册用户名大小写唯一和并发冲突。
- 密码只保存哈希。
- 待审核和禁用账号无法登录。
- 主账户审核后普通账号可登录。
- Access Token 过期刷新、Refresh Token 轮换、重放检测和退出撤销。
- 修改密码、禁用和强制退出使旧 Token 失效。
- 普通账号不能访问账户管理接口。
- 最后一个主账户不能禁用或降级。
- 所有写接口忽略伪造用户 ID，使用 Token 用户。
- 新产品自动通过并可被其他账号搜索。
- 分类统一为 `none/NULL`。
- 新记录默认公开。
- 使用真实 PostgreSQL/Flyway 验证迁移、唯一约束和事务回滚。

### Android

- 首次启动必须登录。
- 注册、等待审核、审核后登录完整闭环。
- 进程重启和设备重启保持登录。
- 主动退出清理本地 Token 并回到登录页。
- Token 刷新并发只发起一次刷新。
- 登录失效时草稿不丢失，重新登录后补传。
- 搜索结果、发现和记录页图片显示正确。
- 记录名称与图片均能进入编辑页。
- 默认公开状态可被用户改为私密。
- 全部页面不展示分类。
- 不存在每日 15:00 定时提交任务。
- 执行 Compose、APK、ADB 权限/弱网/重启和公网 API 回归。

## 11. 非目标

- 不实现手机号、邮箱、验证码或第三方登录。
- 不实现密码找回邮件。
- 不删除历史分类列，只做兼容归一化和前端隐藏。
- 不删除旧匿名历史数据。
- 不引入 Redis、独立认证服务或消息队列。
- 不新增每日定时提交、心跳或批量数据同步任务。

## 12. 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-07-11 | Codex | 新增账号审批、Token 鉴权、无分类产品、自动公开与图片交互设计 | 用户确认取消游客模式并要求主账户审核普通账号 |
