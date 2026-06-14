# 系统设计

## 文档职责

本文件记录系统架构、职责划分、技术选型、代码目录建议、安全与运维基线。

## 顶层架构

建议采用“安卓客户端 + 远程 API 服务 + PostgreSQL + 对象存储 + 图像识别服务 + 管理后台”的结构。

```text
[Android App]
   |
   | HTTPS / JSON
   v
[Backend API Gateway / App Server]
   |------ [PostgreSQL]
   |------ [Object Storage]
   |------ [Image Recognition Service]
   |
   v
[Admin Web Console]
```

## MVP 架构原则

- 使用 `All-in-PostgreSQL`
- 文本搜索先用 `pg_trgm`
- 向量检索先用 `pgvector`
- 不在 MVP 首发阶段引入 Elasticsearch / OpenSearch / 独立向量数据库
- MQ 与缓存能力作为后续扩容选项，不作为首发依赖

## 职责划分

### 安卓端负责

- UI 展示与交互
- 相机 / 相册接入
- 条形码扫描
- 本地 OCR 和基础图片校验
- 图片预处理
- 优先使用本地识别结果发起搜索
- 用户登录态管理
- 搜索、展示、记录提交
- 本地缓存与离线兜底

### 服务端负责

- 用户与权限管理
- 食物与零食资料管理
- 冷启动数据导入与外部数据源聚合
- 记录、评分、标签等业务数据
- 图片存储与访问控制
- 缩略图生成与图片派生资源管理
- 图像识别与相似图检索
- 服务端 OCR provider 抽象与配置化切换
- 审核、统计、推荐、运营配置

## 安卓端技术选型

- 语言：Kotlin
- UI：Jetpack Compose
- 架构：Clean Architecture + MVVM
- 导航：Navigation Compose
- 网络：Retrofit + OkHttp + Kotlinx Serialization
- 图片加载：Coil
- 本地数据库：Room
- 依赖注入：Hilt
- 异步：Kotlin Coroutines + Flow
- 相机：CameraX
- 后台任务：WorkManager
- 本地视觉能力：ML Kit（条形码扫描 / OCR）
- 文件上传：OkHttp Multipart

## 服务端技术选型

- 主服务语言：Java 21 + Spring Boot
- 构建工具：Gradle Wrapper
- API 风格：RESTful API
- ORM：Spring Data JPA / MyBatis Plus
- 数据库迁移：Flyway 或 Liquibase
- 鉴权：JWT + Refresh Token
- 主数据库：PostgreSQL
- 对象存储：MinIO / S3 兼容存储
- 向量检索：pgvector
- 任务调度：数据库任务表 / Spring 异步起步，后续可升级 MQ
- 管理后台：React + Ant Design 或 Vue 3 + Element Plus

## Android 项目结构建议

```text
android-app/
  app/
  core/
    common/
    ui/
    network/
    database/
    model/
    designsystem/
  feature/
    auth/
    home/
    search/
    recognition/
    record/
    profile/
  sync/
  build-logic/
```

### Android 模块职责

- `app`：应用入口、路由、初始化
- `core/common`：通用工具与基础封装
- `core/ui`：通用 UI 组件
- `core/network`：API 服务、拦截器、上传重试
- `core/database`：Room、草稿与同步状态存储
- `core/model`：DTO / VO / Domain Model
- `core/designsystem`：主题、颜色、间距、组件规范
- `feature/search`：文本搜索与结果页
- `feature/recognition`：拍照、扫码、OCR、候选确认
- `feature/record`：记录创建、评分、草稿、历史记录
- `sync`：WorkManager 任务、离线补传、上传重试

最近搜索约束：

- 最近搜索仅保存在本地
- MVP 阶段不上服务端，不做跨设备同步
- 可使用 Room 或轻量本地存储承载
- 用户点击最近搜索词条后，直接回填并立即执行搜索
- 提供清空最近搜索能力，清空动作只影响本地缓存

离线草稿图片管理约束：

- 用户拍照或选图后，草稿阶段应将图片复制到 App 自己的草稿目录
- Room 中保存草稿目录内的本地 URI / 路径，而不是只保存外部相册路径
- 上传成功或草稿废弃后，再清理对应草稿媒体文件

离线草稿重试约束：

- WorkManager 对草稿上传采用有限次自动重试
- 达到重试上限后，将草稿状态标记为 `failed`
- 用户可在 UI 中看到失败状态并主动触发重试
- UI 应展示有限几类可理解的失败原因：网络问题、图片问题、服务异常、未知错误
- 更详细的底层错误细节保留给日志和排查系统，不直接暴露给普通用户

审核自动化约束：

- 自动拒绝规则必须极保守
- 对“极短乱码”采用组合判断，而不是简单长度阈值一刀切
- 组合判断至少包括：长度短、不含已知有效词、且明显不像真实食物名
- 已知有效词来源采用“种子数据词表 + 小型手工词典”
- 小型手工词典由服务端数据库配置表维护，支持后台热更新，无需应用重新发版
- 后台修改词典后应立刻生效，后续审核任务直接读取最新已提交词典
- 后台对词典的增删改必须保留追加式变更日志，至少记录操作人、操作时间，以及以结构化 JSON 快照保存的变更前值和变更后值
- 对停用等状态变更，`after_value` 也保存变更后的完整对象快照，而不是只记录差异字段
- 已停用词条若重新启用，应恢复原记录继续使用，而不是新建一条新记录
- 词条 `word` 文本本身的修正属于同一条记录上的 `update`，不拆成“停用旧词 + 新建新词”
- `word_type` 变更在 MVP 阶段也属于同一条记录上的 `update`，不引入跨语义迁移分支
- `source` 字段变更在 MVP 阶段同样属于同一条记录上的 `update`
- 词典审计日志在 MVP 阶段长期保留，不做自动清理
- 自动拒绝阈值参数仍保留在服务端代码配置中，MVP 阶段不放入后台热更新范围

## 服务端项目结构建议

```text
server/
  api/
    controller/
    dto/
  domain/
    food/
    record/
    user/
    recognition/
  infrastructure/
    persistence/
    storage/
    search/
    security/
  recognition/
    provider/
    vector/
  admin/
    controller/
    service/
```

### 服务端模块职责

- `api`：对 App 暴露接口
- `domain`：核心业务规则
- `infrastructure`：数据库、对象存储、搜索实现
- `recognition`：图像识别与检索能力封装
- `admin`：后台接口与运营能力

MVP 可在 `infrastructure/search` 中直接封装 PostgreSQL 的 `pg_trgm` 和 `pgvector` 查询实现。

当前仓库 Phase 1 首个落地产物：

- `server/` 已初始化为单模块 Spring Boot + Gradle Wrapper 工程
- Flyway 迁移目录固定为 `server/src/main/resources/db/migration`
- 当前已落地 PostgreSQL 扩展初始化与审核词典基础表迁移脚本
- 当前测试通过“禁用数据源自动配置的最小启动测试”验证工程骨架可构建

当前仓库 Phase 1 第二个落地产物：

- 已补充 `users`、`food_items`、`food_images`、`food_records`、`food_record_images`、`recognition_tasks`、`tags` 的首版 Flyway 迁移
- 已增加基于 PostgreSQL Testcontainers 的迁移集成测试，用于验证核心表、约束和 `pg_trgm` 扩展可成功建库
- 当本机 Docker Engine 不可用时，该集成测试自动跳过，不阻塞常规单测

当前仓库 Phase 1 第三个落地产物：

- 已落地 `POST /api/auth/anonymous` 匿名初始化接口
- 已落地 `installationId -> 匿名 user_id` 的服务端复用逻辑
- 已增加控制器与服务层自动化测试，验证匿名用户新建与复用行为

当前仓库 Phase 1 第四个落地产物：

- 已落地 `POST /api/upload/image` 上传接口
- 已落地开发环境本地文件系统对象存储适配，并通过 `/uploads/**` 暴露静态访问路径
- 已支持通过 `SNK_STORAGE_PUBLIC_BASE_URL` 生成绝对 `resourceUrl`，当前默认公网域名为 `https://snk.qiuxinmin.cn`
- 已增加上传控制器与本地存储服务测试，验证图片写入与非法文件拒绝行为

## 安全与运维基线

### 安全

- 全站 HTTPS
- JWT 鉴权与 Refresh Token
- 图片上传鉴权和类型校验
- 上传大小限制
- 接口限流
- 敏感操作审计
- 管理后台 RBAC

### 运维

- Docker 化部署
- 分环境配置管理
- 反向代理部署时启用转发头识别，并支持通过外部基础地址配置生成真机可访问的资源 URL
- 数据库定时备份
- 数据库迁移脚本纳入发布流程
- 图片对象存储生命周期管理
- 上传后异步生成缩略图
- 逻辑删除记录后，已上传图片通过异步或定期回收策略清理，而不是立即物理删除
- API 日志与链路追踪
- 识别失败率与外部数据导入告警

## 当前仓库落地状态

当前仓库 Phase 2 第一个落地产物：

- 已初始化 `android-app/` Gradle Wrapper 与 Android Application 工程
- 已落地 Jetpack Compose 单模块基线与首屏应用壳
- 已加入首页、搜索、草稿、游客身份四个入口页，作为后续游客闭环、搜索、草稿补传的承载层
- 已将默认服务端 `API_BASE_URL` 指向 `https://snk.qiuxinmin.cn/`，供真机直接联调

当前仓库 Phase 2 第二个落地产物：

- 已接入 `Retrofit + OkHttp + Kotlinx Serialization` 基础网络层
- 已接入 `DataStore` 安装级身份存储，并生成稳定 `installationId`
- 已落地匿名初始化仓储，支持远程初始化成功与离线缓存回退两种路径
- 已在应用启动阶段接入游客身份初始化状态，并回显到搜索页与个人页
- 已补充匿名初始化仓储单测，覆盖远程成功与断网回退场景

当前仓库 Phase 2 第三个落地产物：

- 已落地服务端 `GET /api/foods/search?q=` 文本搜索接口
- 已通过样例种子数据让本地开发环境具备最小可演示搜索结果
- 已在安卓搜索页接入真实远程搜索请求、质量信号展示与结果列表渲染
- 已补充服务端搜索控制器/服务测试与安卓搜索仓储单测

当前仓库 Phase 2 第四个落地产物：

- 已落地服务端 `POST /api/records` 记录创建接口
- 已落地安卓端“搜索结果 -> 新建记录”提交页
- 已支持对命中食物条目填写评分与备注并提交到远程服务端
- 已补充服务端记录创建控制器/服务测试与安卓记录仓储单测

当前仓库 Phase 2 第五个落地产物：

- 已落地基于 Room 的本地记录草稿存储，支持提交失败时将记录转存为本地草稿
- 已落地 WorkManager 草稿补传任务，网络恢复后自动重试远程 `POST /api/records`
- 已在安卓端草稿页展示 `draft / syncing / synced / failed` 状态，并支持 `failed` 草稿手动重试
- 已补充安卓端提交协调器单测，并通过真机 `adb` 验证“断网保存草稿 -> 恢复网络自动补传”闭环

当前仓库 Phase 3 新增落地产物：

- 已落地 `POST /api/recognition/ocr` 服务端 OCR 兜底接口
- 已在服务端引入 `ServerOcrProvider` 抽象与 `snk.recognition.ocr.*` 配置项，当前默认 `disabled`，开发期可切到 `stub`
- 已在安卓端 OCR 页面接通“本地 OCR -> 服务端 OCR -> 手动创建”回退链路
- 已落地 `RecognitionTaskService`、`RecognitionTaskRepository` 与 `POST /api/recognition/tasks` / `GET /api/recognition/tasks/{id}` 图片识别任务接口
- 已在服务端引入 `ImageRecognitionTaskProvider` 抽象与 `snk.recognition.image.*` 配置项，当前默认 `disabled`，开发期可切到 `stub`
- 已在安卓端 OCR 页面接通“上传图片 -> 创建识别任务 -> 候选确认 -> 手动创建”回退链路，并复用统一候选确认页
- 已补充后台治理基础能力的最小落地：`GET /api/admin/food-items/pending`、`GET /api/admin/food-items/reported`、`POST /api/admin/food-items/{foodItemId}/approve`、`POST /api/admin/food-items/{foodItemId}/reject`
- 已补充后台食物条目管理能力的最小落地：`GET /api/admin/food-items`、`GET /api/admin/food-items/{foodItemId}`
- 已补充审核词典后台能力的最小落地：`GET /api/admin/review-config-words`、`POST /api/admin/review-config-words`、`PUT /api/admin/review-config-words/{wordId}`、`POST /api/admin/review-config-words/{wordId}/enable`、`POST /api/admin/review-config-words/{wordId}/disable`、`GET /api/admin/review-config-words/{wordId}/audit-logs`
- 已补充识别任务监控能力的最小落地：`GET /api/admin/recognition-tasks`、`GET /api/admin/recognition-tasks/{taskId}`
- 已补充 `pending` 超时自动审核服务：超过 24 小时的待审条目按保守规则扫描，仅对明显垃圾数据执行拒绝
- 已补充用户报错信号接口：`POST /api/foods/{foodItemId}/report`，用于累加 `report_count`

## 变更记录维护规则

- 每次修改本文件时，必须在下方追加一条记录

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 从 `agents.md` 拆出系统架构、技术选型、目录结构与运维基线 | 将核心看板与技术细节解耦 |
| 2026-06-13 | Codex | 明确离线草稿图片复制到 App 草稿目录 | 已确认草稿媒体不能只依赖原始相册路径 |
| 2026-06-13 | Codex | 明确离线草稿采用有限次自动重试 | 已确认失败后应转为人工重试，而不是无限后台重试 |
| 2026-06-13 | Codex | 明确草稿失败原因采用轻量分类展示 | 已确认 UI 需要可理解失败原因，而不是只有笼统失败提示 |
| 2026-06-13 | Codex | 明确逻辑删除记录后的媒体回收策略 | 已确认正式媒体资源不应在删除记录时立即物理清除 |
| 2026-06-13 | Codex | 明确最近搜索只做本地缓存 | 已确认最近搜索提升体验，但无需进入服务端同步范围 |
| 2026-06-13 | Codex | 明确点击最近搜索后直接执行搜索 | 已确认最近搜索应尽量减少额外交互步骤 |
| 2026-06-13 | Codex | 明确最近搜索支持本地清空 | 已确认本地历史缓存需要可控的用户清理入口 |
| 2026-06-13 | Codex | 明确极短乱码拒绝规则采用组合判断 | 已确认自动审核要避免误伤真实短名称食物 |
| 2026-06-13 | Codex | 明确已知有效词采用双来源 | 已确认自动拒绝逻辑需要稳定词表支撑 |
| 2026-06-13 | Codex | 明确小型手工词典由数据库配置表热更新维护 | 已确认该词典不应依赖发版更新，需支持后台动态调整 |
| 2026-06-13 | Codex | 明确词典更新后立刻生效 | 已确认后台修改词典后，后续审核任务应直接读取最新值 |
| 2026-06-13 | Codex | 明确词典后台修改必须保留审计记录 | 已确认后续需要追查误判来源，不能只有当前值没有历史 |
| 2026-06-13 | Codex | 明确词典审计采用追加式变更日志 | 已确认不能只保留当前行更新时间，需保留完整历史轨迹 |
| 2026-06-13 | Codex | 明确词典审计前后值采用 JSON 快照 | 已确认日志需要适应后续字段扩展，避免退化成单字段文本 |
| 2026-06-13 | Codex | 明确停用操作也保存完整 after 快照 | 已确认状态变更排查需要完整对象而不只是差异字段 |
| 2026-06-13 | Codex | 明确重新启用时恢复原记录 | 已确认词典实体与审计链需要连续，不应新建并行记录 |
| 2026-06-13 | Codex | 明确词条文本修正属于同记录 update | 已确认轻量修正不应被放大成停用加新建两条实体 |
| 2026-06-13 | Codex | 明确 `word_type` 变更也属于同记录 update | 已确认 MVP 阶段先不引入跨类型迁移治理分支 |
| 2026-06-13 | Codex | 明确 `source` 变更也属于同记录 update | 已确认词典轻量字段变更在 MVP 阶段统一走 update 语义 |
| 2026-06-13 | Codex | 明确词典审计日志长期保留 | 已确认此类日志量小但排查价值高，MVP 阶段不做自动清理 |
| 2026-06-13 | Codex | 明确自动拒绝阈值继续留在代码配置 | 已确认 MVP 阶段需限制后台热更新范围，避免审核规则过度可变 |
| 2026-06-13 | Codex | 回填服务端构建工具与 Phase 1 首个落地产物 | 当前仓库已初始化 `server/` 工程并落地 Flyway 迁移目录与审核词典基础迁移 |
| 2026-06-13 | Codex | 回填 Phase 1 第二个落地产物 | 当前仓库已新增核心业务表迁移与 PostgreSQL 容器集成测试 |
| 2026-06-13 | Codex | 补充 Docker 不可用时的集成测试降级说明 | 当前环境无法启动 Docker Engine，需要让测试策略与真实环境约束保持一致 |
| 2026-06-13 | Codex | 回填 Phase 1 第三个落地产物 | 当前仓库已开始落地游客模式的最小服务端身份闭环 |
| 2026-06-13 | Codex | 回填 Phase 1 第四个落地产物 | 当前仓库已开始落地上传接口与开发环境对象存储适配 |
| 2026-06-13 | Codex | 回填 Phase 2 第一个落地产物 | 当前仓库已开始落地安卓端基础工程与导航应用壳，作为后续游客闭环、搜索与草稿能力的基座 |
| 2026-06-13 | Codex | 回填 Phase 2 第二个落地产物 | 当前仓库已落地安卓端游客身份初始化、安装级身份持久化与离线缓存回退能力 |
| 2026-06-13 | Codex | 回填 Phase 2 第三个落地产物 | 当前仓库已落地服务端文本搜索接口、样例种子数据与安卓端真实搜索结果展示 |
| 2026-06-13 | Codex | 回填 Phase 2 第四个落地产物 | 当前仓库已落地搜索命中后的记录创建接口与安卓端评分提交页 |
| 2026-06-13 | Codex | 补充反向代理与绝对资源地址配置约束 | 真机联调与宝塔部署需要稳定的转发头处理和外部资源地址生成策略 |
| 2026-06-14 | Codex | 回填正式公网域名到服务端资源地址与安卓默认接口地址 | 已确认统一使用 `https://snk.qiuxinmin.cn` 做服务器部署与真机联调 |
| 2026-06-14 | Codex | 回填 Phase 2 第五个落地产物 | 当前仓库已落地 Room 草稿存储、WorkManager 自动补传与草稿页重试闭环 |
| 2026-06-14 | Codex | 回填 Phase 3 的服务端 OCR 兜底接口与客户端自动回退链路 | 当前仓库已具备本地 OCR 失败后的服务端 OCR 回退能力，并预留 provider 配置扩展点 |
| 2026-06-14 | Codex | 回填 Phase 3 的图片识别任务接口与客户端自动回退链路 | 当前仓库已具备上传图片后创建识别任务、统一导入候选确认页并最终回退到手动创建的闭环 |
| 2026-06-14 | Codex | 回填 Phase 4 的后台 pending / reported 条目列表能力 | 当前仓库已开始落地后台治理的最小可见能力，便于查看待审核与报错条目 |
| 2026-06-14 | Codex | 回填 Phase 4 的用户报错信号接口 | 当前仓库已开始落地 `report_count` 累加入口，作为后续纠错治理的基础数据源 |
| 2026-06-14 | Codex | 回填 Phase 4 的后台审核通过 / 驳回动作 | 当前仓库已补齐后台审核动作，形成从查看队列到处理条目的完整闭环 |
| 2026-06-14 | Codex | 回填 Phase 4 的 pending 超时自动审核服务 | 当前仓库已补上 24 小时后保守自动拒绝机制，避免待审条目无限悬挂 |
| 2026-06-14 | Codex | 回填审核词典后台 CRUD / 启停 / 审计日志接口 | 当前仓库已落地审核词典配置、启停和历史追踪闭环 |
| 2026-06-14 | Codex | 回填识别任务监控接口 | 当前仓库已落地后台查看识别失败与异常任务的最小闭环 |
| 2026-06-14 | Codex | 回填食物条目管理列表 / 详情接口 | 当前仓库已落地后台食物条目基础管理闭环 |
