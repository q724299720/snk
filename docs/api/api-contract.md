# 接口边界说明

## 文档职责

本文档记录 App 端与后台的接口边界、职责分工和识别链路接口约束。正式 `OpenAPI` 文档后续可在此基础上生成。

## App 端主要接口

### 认证

- `POST /api/auth/anonymous`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

游客模式初始化约束：

- `POST /api/auth/anonymous` 作为 MVP 首个实际落地的认证入口
- 客户端上传安装级 `installationId`
- 服务端对同一 `installationId` 复用同一匿名 `user_id`
- 卸载重装后若 `installationId` 变化，服务端视为新匿名用户

### 食物搜索

- `GET /api/foods/search?q=&userId=`
- `GET /api/foods/{foodItemId}/related`
- `POST /api/foods/manual`
- `GET /api/foods/{id}`

文本搜索当前约束：

- `GET /api/foods/search?q=` 默认返回已审核通过的基础食物条目
- `userId` 为可选参数；传入当前游客 / 用户 id 时，搜索结果可额外包含该用户自己创建且仍为 `pending` 的条目
- 未传 `userId` 或 `userId` 非法时，不返回任何 `pending` 条目
- 空白 `q` 直接返回 `400`
- `q` 去除首尾空格后最长 `128` 字符，超过长度直接返回 `400`，避免高并发下超长模糊搜索放大数据库压力
- 服务端会先按原始 `q` 搜索；当原始词组未命中时，会继续尝试去空格后的紧凑查询，并按食物条目 ID 去重
- 当前响应包含 `items` 与 `qualitySignal`
- `items[*]` 当前最小字段包含：`id`、`name`、`itemType`、`category`、`subcategory`、`brand`、`barcode`、`coverImageUrl`、`averageRating`、`auditStatus`
- `qualitySignal` 当前最小取值：`strong / weak`

相似推荐当前约束：

- `GET /api/foods/{foodItemId}/related` 用于返回当前条目的相似食物推荐
- `{foodItemId}` 必须为正整数，`0` 或负数直接返回 `400`
- 当前推荐首版按同品牌、同二级分类、同一级分类和同名称词召回
- 当前响应形态与搜索结果一致，返回 `items` 与 `qualitySignal`
- `qualitySignal` 当前可取 `related / weak`

手动创建条目当前约束：

- `POST /api/foods/manual` 当前最小请求字段包含：`userId`、`name`、`itemType`、`category`
- `subcategory`、`brand`、`barcode` 当前为可选字段
- `itemType` 当前仅允许：`packaged_product / dish / fruit`
- `barcode` 当前仅建议用于 `packaged_product`
- 服务端成功后直接返回新建的 `FoodItem` 响应体
- 当前新建条目固定写入：`source = user_generated`、`auditStatus = pending`
- 当前响应体字段包含：`id`、`name`、`itemType`、`category`、`subcategory`、`brand`、`barcode`、`coverImageUrl`、`auditStatus`
- 当前客户端在创建成功后直接进入“记录创建”页，不要求用户重新搜索
- MVP 当前不提供扫码入口，`barcode` 仅作为包装食品的可选资料字段保留

### 图片与识别

- `POST /api/upload/image`
- `POST /api/recognition/ocr`

图片上传约束：

- `POST /api/upload/image` 使用 `multipart/form-data`
- 首版字段采用 `file`
- 服务端当前返回 `objectKey`、`resourceUrl`、`thumbnailObjectKey`、`thumbnailUrl`、`contentType`、`size`
- `thumbnailUrl` 用于历史记录、列表页和候选卡片优先加载缩略图，避免首屏直接请求原图
- `resourceUrl` 在未配置公网前缀时可返回相对路径；配置 `SNK_STORAGE_PUBLIC_BASE_URL` 后应返回可被真机直接访问的绝对地址，当前部署目标为 `https://snk.qiuxinmin.cn`
- 首版仅接受图片 `MIME` 类型，不接受通用文件上传
- 开发环境当前采用本地文件系统存储，对外暴露 `resourceUrl` 静态访问路径；后续可替换为 MinIO / S3

### 记录管理

- `POST /api/records`
- `GET /api/records?userId=&limit=`
- `GET /api/records/public?limit=`
- `GET /api/records/{id}`
- `PUT /api/records/{id}`
- `DELETE /api/records/{id}`
- `POST /api/records/{id}/like`
- `GET /api/records/{id}/comments`
- `POST /api/records/{id}/comments`

Record image contract:

- `POST /api/upload/image` uploads the raw image first and returns `resourceUrl` plus optional `thumbnailUrl`.
- `POST /api/records` may include optional `images: [{ imageUrl, thumbnailUrl }]`.
- `imageUrl` must come from a successful upload response; `thumbnailUrl` should be reused when available.
- `POST /api/records` success response returns the same `images` array.
- `GET /api/records?userId=&limit=` returns each record with `images`; mobile recent-record cards must prefer `images[0].thumbnailUrl`, then `images[0].imageUrl`, then `foodCoverImageUrl`.
- Offline draft image retry is not expanded in this increment; if a record is submitted while offline, existing draft retry remains text-record only until a later draft-media migration.

Public record feed contract:

- `GET /api/records/public?limit=` returns only records where `isPublic = true` and `deletedAt IS NULL`.
- `limit` is optional, defaults to `10`, must be positive, and is capped by the service layer.
- The response shape matches `GET /api/records?userId=&limit=`, including `images`.
- Mobile public-feed cards must prefer `images[0].thumbnailUrl`, then `images[0].imageUrl`, then `foodCoverImageUrl`.
- `POST /api/records` keeps `isPublic` opt-in. Clients must not default new records to public without explicit user action.

记录创建当前约束：

- `POST /api/records` 当前最小请求字段包含：`userId`、`foodItemId`、`sourceType`、`isPublic`、`rating`
- `comment`、`recordTime` 当前为可选字段
- `comment` 最长 `500` 字符；Android 客户端应在本地拦截超长备注，服务端也必须返回 `400`
- 当前 `rating` 允许范围为 `1-5`
- 当前客户端只应发送：`text_search / manual`
- 服务端创建记录时仅接受 `text_search / manual`，其他 `sourceType` 直接返回 `400`
- 历史服务端约束中仍保留 `image_search`，仅用于兼容旧数据，不作为当前 MVP 入口
- 当前成功响应返回：`id`、`userId`、`foodItemId`、`sourceType`、`isPublic`、`rating`、`comment`、`likeCount`、`recordTime`、`createdAt`

记录点赞当前约定：

- `GET /api/records` 当前用于按用户读取最近记录；`userId` 和 `limit` 必须为正整数，`0` 或负数直接返回 `400`
- `POST /api/records/{id}/like` 用于对记录增加一次聚合点赞
- `{id}` 必须为正整数，`0` 或负数直接返回 `400`
- 当前实现不做用户级去重，只累计 `likeCount`
- 当前成功响应返回与记录创建一致的完整记录视图，并包含更新后的 `likeCount`

记录评论当前约定：

- `GET /api/records/{id}/comments?limit=` 用于读取公开记录最新评论
- `POST /api/records/{id}/comments` 用于提交公开记录评论
- `{id}` 和 `limit` 必须为正整数，`0` 或负数直接返回 `400`
- 评论请求字段包含：`userId`、`content`
- `userId` 必须为正整数；`content` 不能为空，最长 `500` 字符
- 评论接口仅允许访问 `isPublic = true` 且未删除的记录；私有记录返回 `403`，删除或不存在记录返回 `404`
- 当前评论响应字段包含：`id`、`recordId`、`userId`、`content`、`createdAt`
- App 首页公开分享卡片优先展示最近评论，并允许当前游客用户提交评论

### 标签与分类

- `GET /api/tags`
- `GET /api/categories`

## 管理后台接口范围

当前轻量后台入口：

- `GET /admin/index.html`

后台 API 访问保护：

- 默认未配置 token 时，`/api/admin/**` 保持放行，便于本地开发和现有部署平滑升级
- 配置环境变量 `SNK_ADMIN_API_TOKEN` 后，`/api/admin/**` 请求必须携带 `X-SNK-ADMIN-TOKEN: <token>`
- 可通过 `SNK_ADMIN_TOKEN_HEADER` 覆盖默认 header 名称

- 食物条目管理
- 图片样本管理
- 用户记录审核
- 用户报错 / 纠错处理
- 标签体系管理
- 统计报表
- `GET /api/admin/food-items/pending`
- `GET /api/admin/food-items?auditStatus=&q=&limit=`
- `GET /api/admin/food-items/{foodItemId}`
- `GET /api/admin/food-items/{foodItemId}/reports`
- `GET /api/admin/food-items/reported?minReportCount=`
- `POST /api/admin/food-items/{foodItemId}/approve`
- `POST /api/admin/food-items/{foodItemId}/reject`
- `POST /api/admin/food-items/{foodItemId}/clear-reports`
- `POST /api/admin/food-items/{foodItemId}/merge`
- `POST /api/foods/{foodItemId}/report`
- `GET /api/admin/stats`
- `POST /api/admin/moderation/auto-audit/run`

后台食物条目管理当前约定：

- 列表支持按 `auditStatus`、`q` 和 `limit` 过滤，默认按创建时间倒序
- `auditStatus` 为可选参数；传入时仅允许 `pending / approved / rejected`，其他值直接返回 `400`
- `limit` 必须为正整数，`0` 或负数直接返回 `400`；服务端最多返回 `100` 条
- 返回字段与后台条目治理一致，详情接口复用同一响应模型
- 详情接口返回单条完整管理视图，便于后台核查与处理
- `clear-reports` 用于将已处理条目的 `reportCount` 清零，作为报错处理闭环的结束动作
- `reports` 用于查看单个条目的报错 / 纠错明细，便于后台核查具体反馈原因
- `merge` 用于将重复或错误条目的历史 `FoodRecord` 迁移到保留条目，并将重复条目标记为 `rejected`
- `POST /api/admin/food-items/{foodItemId}/merge` 当前请求字段为 `targetFoodItemId`
- `targetFoodItemId` 必须为正整数，缺失、为 `0` 或负数时应直接返回 `400`
- 合并响应字段包含：`duplicateItem`、`targetItem`、`migratedRecordCount`
- MVP 阶段不新增 `merged` 审核状态，合并后的重复条目使用 `rejected` 阻止继续进入全局搜索
- 合并目标条目必须为 `approved`，避免历史记录被迁移到仍不可全局搜索的 `pending / rejected` 条目

后台统计报表当前约定：

- `GET /api/admin/stats` 用于返回后台治理的汇总概览
- 当前最小响应字段包含：`totalFoodItems`、`pendingFoodItems`、`approvedFoodItems`、`rejectedFoodItems`、`reportedFoodItems`、`totalRecognitionTasks`、`pendingRecognitionTasks`、`processingRecognitionTasks`、`completedRecognitionTasks`、`failedRecognitionTasks`、`enabledReviewWords`、`disabledReviewWords`
- 识别任务统计字段仅兼容历史服务端预留能力，当前 Android MVP 不再创建图片识别任务
- `totalRecognitionTasks` 统计所有历史识别任务状态，至少包含 `pending / processing / completed / failed`
- 统计口径只做后台管理概览，不承诺分页或趋势图

后台自动审核当前约定：

- `POST /api/admin/moderation/auto-audit/run` 用于后台人工立即触发一次自动审核扫描
- 自动审核仍只处理超过 `SNK_MODERATION_PENDING_AGE_HOURS` 的 `pending` 条目，默认阈值为 24 小时
- 当前响应字段包含：`scannedCount`、`rejectedCount`、`keptPendingCount`、`cutoffAt`、`rejectedFoodItemIds`
- 自动审核只执行保守拒绝，不自动大范围通过条目
- 接口受同一后台 Admin Token 机制保护

后台条目治理当前约定：

- `pending` 列表返回当前待审核食物条目，按创建时间倒序
- `reported` 列表返回报错次数达到阈值的食物条目，默认阈值为 `1`
- `reported` 的 `minReportCount` 必须为正整数，`0` 或负数直接返回 `400`
- 后台返回字段至少包含：`id`、`name`、`itemType`、`category`、`subcategory`、`brand`、`barcode`、`source`、`auditStatus`、`reportCount`、`createdByUserId`、`createdAt`、`updatedAt`
- `approve` 会将条目标记为 `approved`，并允许继续进入全局搜索
- `reject` 会将条目标记为 `rejected`，并阻止进入全局搜索
- `GET /api/admin/food-items/{foodItemId}/reports` 当前最小响应字段包含：`id`、`foodItemId`、`reporterUserId`、`reason`、`createdAt`
- `GET /api/admin/food-items/{foodItemId}/reports` 在目标条目不存在时返回 `404`，避免后台将错误 ID 误判为无报错
- 上述带 `{foodItemId}` 的后台食物条目操作接口要求 `foodItemId` 为正整数，`0` 或负数直接返回 `400`

审核词典后台接口：

- `GET /api/admin/review-config-words?enabled=&wordType=`
- `POST /api/admin/review-config-words`
- `PUT /api/admin/review-config-words/{wordId}`
- `POST /api/admin/review-config-words/{wordId}/enable`
- `POST /api/admin/review-config-words/{wordId}/disable`
- `GET /api/admin/review-config-words/{wordId}/audit-logs`
- 上述带 `{wordId}` 的接口要求 `wordId` 为正整数，`0` 或负数直接返回 `400`

审核词典当前约定：

- 列表支持按 `enabled` 和 `wordType` 过滤，默认按 `updatedAt` 倒序
- `wordType` 在列表过滤时为可选参数；传入时仅允许 `valid_food_word / blocked_word`，其他值直接返回 `400`
- `wordType` 在新增 / 编辑词条时为必填参数，且仅允许 `valid_food_word / blocked_word`，其他值直接返回 `400`
- 返回字段至少包含：`id`、`word`、`wordType`、`enabled`、`source`、`remark`、`updatedBy`、`createdAt`、`updatedAt`
- 新增 / 编辑 / 启停都会追加审计日志
- 审计日志返回 `beforeValue` 与 `afterValue` 的结构化 JSON 快照
- `GET /api/admin/review-config-words/{wordId}/audit-logs` 在目标词条不存在时返回 `404`
- 词典后台修改后立即生效，后续审核任务直接读取最新提交数据

历史识别任务监控接口：

- `GET /api/admin/recognition-tasks?status=&userId=&limit=`
- `GET /api/admin/recognition-tasks/{taskId}`

历史识别任务监控当前约定：

- 列表支持按 `status` 和 `userId` 过滤，`limit` 默认 `20`
- `status` 为可选参数；传入时仅允许 `pending / processing / completed / failed`，其他值直接返回 `400`
- `userId` 为可选参数；传入时必须为正整数，`0` 或负数直接返回 `400`
- `limit` 必须为正整数，`0` 或负数直接返回 `400`
- 列表按创建时间倒序返回，最多返回 `100` 条
- 返回字段至少包含：`id`、`userId`、`inputImageUrl`、`status`、`topCandidates`、`selectedFoodItemId`、`confidence`、`createdAt`、`finishedAt`、`statusReason`
- `GET /api/admin/recognition-tasks/{taskId}` 要求 `taskId` 为正整数，`0` 或负数直接返回 `400`
- 当前 Android MVP 不再调用图片识别任务创建接口
- 该接口仅用于查看历史预留任务数据，后续若服务端正式下线图片识别任务，应同步移除此段

用户报错接口当前约定：

- `POST /api/foods/{foodItemId}/report` 用于提交用户对条目的报错或纠错信号
- 当前最小请求字段为 `userId`，`reason` 可选
- `{foodItemId}` 和 `userId` 必须为正整数，`0` 或负数直接返回 `400`
- 服务端成功后会将目标条目的 `report_count` 加 `1`
- 服务端成功后会写入一条 `food_item_reports` 明细记录，保留提交用户、原因与时间
- 当前最小响应字段为 `foodItemId`、`reportCount`、`auditStatus`

## OCR 与识别接口边界

### 本地优先原则

- 本地 ML Kit OCR 成功时，客户端优先直接调用 `GET /api/foods/search?q=` 做文本召回
- 当前客户端没有条码识别入口，不进入条码查询链路

### 服务端 OCR 兜底原则

`POST /api/recognition/ocr` 仅在以下情况下使用：

- 本地 OCR 失败
- 本地 OCR 结果乱码严重
- 本地 OCR 结果置信度过低
- 需要更强云端 OCR 能力做补充识别
- 当前接口使用 `multipart/form-data`
- 当前首版字段：
- `file`：必填图片文件
- `clientRecognizedText`：可选，本地 OCR 已提取出的文本，供服务端 OCR 或兜底策略复用
- 当前响应字段：
- `recognizedText`、`attemptedQueries`、`matchedQuery`、`qualitySignal`、`items`
- 当前若服务端 OCR provider 未配置，返回 `503`
- 当前后端已预留 provider 抽象，MVP 首版默认支持 `disabled / stub` 两种模式，后续再接真实云 OCR

### 非 MVP 识别边界

- 当前 Android MVP 不做条码识别
- 当前 Android MVP 不做以图搜图或图片识别任务
- 当前 Android MVP 的图片入口只用于 OCR 文本提取，提取结果统一回填主搜索框
- 服务端历史预留的条码查询和图片识别任务接口不再作为 App 端当前主链路接口

## 候选质量信号

识别和搜索相关接口在 MVP 阶段应支持返回一个简单的候选质量信号，供前端判断是否需要展示“手动创建”等兜底入口。

最小要求：

- 可区分“结果稳定”与“结果不可靠”
- 前端不必只依赖结果数量做判断
- 不要求首版就提供复杂打分体系，只需支持基本低置信提示

## 接口演进规则

- 先维护本文档，再落正式 `OpenAPI`
- 涉及字段新增、状态枚举变化、鉴权变化时，必须同步更新数据库文档
- 识别链路接口调整时，必须同步更新 `docs/recognition/recognition-plan.md`

## 变更记录维护规则

- 每次修改本文档时，必须在下方追加一条记录

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 从 `agents.md` 拆出接口边界、OCR 分工和识别接口职责 | 防止本地 OCR 与服务端 OCR 边界不清 |
| 2026-06-13 | Codex | 明确接口需返回候选质量信号 | 已确认兜底入口展示由后端信号与前端场景共同决定 |
| 2026-06-13 | Codex | 增加匿名用户初始化接口与安装级复用约束 | 当前 Phase 1 已开始落地游客模式的最小服务端身份闭环 |
| 2026-06-13 | Codex | 回填图片上传接口的 multipart 与返回约束 | 当前 Phase 1 已开始落地上传接口与本地对象存储适配 |
| 2026-06-13 | Codex | 回填文本搜索接口的最小响应字段、质量信号与空查询约束 | 当前 Phase 2 已开始落地服务端文本搜索接口并被安卓搜索页真实消费 |
| 2026-06-13 | Codex | 回填记录创建接口的最小请求字段与成功响应约束 | 当前 Phase 2 已开始落地搜索命中后的远程记录创建闭环 |
| 2026-06-13 | Codex | 补充图片上传 `resourceUrl` 的相对 / 绝对地址约束 | 真机联调与宝塔反向代理部署需要稳定的外部可访问资源地址策略 |
| 2026-06-14 | Codex | 补充当前生产域名 | 已确认图片资源与 App 真机访问统一使用 `https://snk.qiuxinmin.cn` |
| 2026-06-21 | Codex | 收口 App 端识别接口边界 | 当前 Android MVP 已去掉条码与图片识别任务入口，识别链路固定为名称搜索、OCR 文本辅助与手动创建 |
| 2026-06-14 | Codex | 补充手动创建待审核条目接口与 `auditStatus` 响应字段 | Phase 3 已落地搜索失败后的手动创建条目闭环，接口文档需与实现对齐 |
| 2026-06-14 | Codex | 补充手动创建条目的可选 `barcode` 入参与扫码未命中的前端流转 | Phase 3 需覆盖包装食品扫码未命中的 UGC 沉淀场景 |
| 2026-06-14 | Codex | 补充服务端 OCR 接口的 multipart 入参、响应字段与 provider 降级语义 | Phase 3 已落地本地 OCR 失败后的服务端 OCR 回退链路 |
| 2026-06-14 | Codex | 补充图片识别任务接口的上传前置约束、响应字段与客户端轮询语义 | Phase 3 已落地图片上传后创建识别任务并回退到候选确认页的闭环 |
| 2026-06-14 | Codex | 补充图片上传响应的缩略图字段 | 需要让历史记录与列表页优先使用缩略图资源，减少首屏原图加载压力 |
| 2026-06-14 | Codex | 补充后台 pending / reported 条目列表接口 | Phase 4 已开始落地后台治理的最小可见能力 |
| 2026-06-14 | Codex | 补充用户报错接口与 `report_count` 累加语义 | Phase 4 已开始落地报错 / 纠错处理流程的最小后端信号 |
| 2026-06-14 | Codex | 补充后台审核通过 / 驳回接口 | Phase 4 需要具备可执行的审核动作，形成列表到处理的完整闭环 |
| 2026-06-14 | Codex | 补充审核词典后台 CRUD / 启停 / 审计日志接口 | Phase 4 已落地配置词典的编辑、立即生效和历史追踪闭环 |
| 2026-06-14 | Codex | 补充识别任务监控接口 | Phase 4 已落地后台查看识别失败与异常任务的最小闭环 |
| 2026-06-14 | Codex | 补充食物条目管理列表 / 详情接口 | Phase 4 已落地后台食物条目基础管理闭环 |
| 2026-06-14 | Codex | 补充食物条目报错清零接口 | Phase 4 已补齐报错 / 纠错处理流程的最小处理动作 |
| 2026-06-14 | Codex | 补充后台统计报表接口 | Phase 4 已落地后台治理汇总概览闭环 |
| 2026-06-14 | Codex | 补充记录点赞接口与 `likeCount` 响应字段 | Phase 5 已落地记录互动的最小聚合点赞闭环 |
| 2026-06-14 | Codex | 补充相似食物推荐接口 | Phase 5 已落地记录页可消费的最小推荐闭环 |
| 2026-06-16 | Codex | 补充图片识别任务可选 `hintQuery` 入参与服务端优先召回规则 | 让 OCR 已提取到的文本提示继续参与图片识别兜底链路，减少上下文丢失 |
| 2026-06-21 | Codex | 补充轻量后台入口与 Admin Token 访问约束 | Phase 4 已新增 `/admin/index.html` 静态后台，并通过可选 token 保护后台 API |
| 2026-06-21 | Codex | 补充后台食物条目报错明细接口 | Phase 4 需要后台可追踪用户报错原因，报错治理不能只依赖聚合计数 |
| 2026-06-21 | Codex | 补充搜索接口的创建者 pending 可见性参数 | Phase 4 要求待审核条目仅创建者可见，普通全局搜索仍只暴露已审核条目 |
| 2026-06-21 | Codex | 修正后台识别任务总数统计口径 | 历史识别任务监控仍保留，统计总数需要包含 pending 任务以避免后台概览低估遗留任务 |
| 2026-06-21 | Codex | 补充后台统计的待处理识别任务字段 | 后台页需要直接看到 pending 历史识别任务数量，便于定位总数来源 |
| 2026-06-21 | Codex | 补充后台自动审核手动触发接口 | Phase 4 需要后台页面可立即触发一次超时待审核条目的保守自动审核扫描 |
| 2026-06-21 | Codex | 补充后台条目合并接口 | Phase 4 报错 / 纠错治理需要支持将重复条目的历史记录迁移到保留条目 |
| 2026-06-21 | Codex | 补充条目合并目标状态约束 | 合并会迁移历史记录，目标必须是已审核通过条目，避免记录归属到不可全局搜索条目 |
| 2026-06-21 | Codex | 补充合并目标 ID 入参校验约束 | 后台合并请求必须在控制器层拒绝缺失或非正数目标 ID，避免无效请求进入治理服务 |
| 2026-06-21 | Codex | 补充报错明细未知条目返回 404 约束 | 后台查询不存在条目的报错明细不应返回空列表，避免误导人工治理判断 |
| 2026-06-21 | Codex | 补充审核词典审计日志未知词条返回 404 约束 | 后台查询不存在词条的审计日志不应返回空列表，避免误导治理判断 |
| 2026-06-21 | Codex | 补充审核词典 `wordId` 正数校验约束 | 后台词典操作接口应在控制器层拒绝 `0` 或负数 ID，避免无效请求进入服务层 |
| 2026-06-21 | Codex | 补充后台食物条目 `foodItemId` 正数校验约束 | 后台食物条目操作接口应在控制器层拒绝 `0` 或负数 ID，避免无效请求进入服务层 |
| 2026-06-21 | Codex | 补充历史识别任务 `taskId` 正数校验约束 | 后台历史识别任务详情接口应在控制器层拒绝 `0` 或负数 ID，避免无效请求进入服务层 |
| 2026-06-21 | Codex | 补充后台列表参数正数校验约束 | 后台食物条目、报错条目和历史识别任务列表应拒绝非正数分页 / 阈值参数，避免无效请求进入服务层 |
| 2026-06-21 | Codex | 补充用户报错接口 ID 正数校验约束 | 用户报错 / 纠错入口应拒绝非正数 `foodItemId` 和 `userId`，避免无效请求进入服务层 |
| 2026-06-21 | Codex | 补充相似推荐 `foodItemId` 正数校验约束 | 相似推荐入口应拒绝非正数食物 ID，避免无效请求进入推荐服务 |
| 2026-06-21 | Codex | 补充历史识别任务列表 `userId` 正数校验约束 | 后台历史识别任务列表按用户过滤时应拒绝非正数用户 ID，避免无效请求进入查询服务 |
| 2026-06-21 | Codex | 补充历史识别任务列表 `status` 枚举校验约束 | 后台历史识别任务列表按状态过滤时应拒绝未知状态，避免后台误判为空结果 |
| 2026-06-21 | Codex | 补充后台食物条目列表 `auditStatus` 枚举校验约束 | 后台食物条目列表按审核状态过滤时应拒绝未知状态，避免后台误判为空结果 |
| 2026-06-21 | Codex | 补充审核词典列表 `wordType` 枚举校验约束 | 后台审核词典列表按类型过滤时应拒绝未知类型，避免后台误判为空结果 |
| 2026-06-21 | Codex | 扩展审核词典新增 / 编辑 `wordType` 枚举校验约束 | 后台新增或编辑审核词条时应拒绝未知类型，避免写入不可治理词条 |
| 2026-06-21 | Codex | 补充记录创建 `sourceType` 写入校验约束 | 记录创建接口应拒绝非当前入口来源类型，避免旧链路或未知来源继续写入新记录 |
| 2026-06-21 | Codex | 补充记录列表与点赞 ID 正数校验约束 | 记录列表和点赞入口应拒绝非正数 `userId`、`limit` 与 `recordId`，避免无效请求进入服务层 |
| 2026-06-21 | Codex | 补充公开记录流接口约束 | Phase 5 需要最小公开分享列表，只暴露用户主动公开且未删除的记录 |
| 2026-06-21 | Codex | 补充公开记录评论接口约束 | Phase 5 评论能力需要固定公开记录评论的读取、提交、字段和权限边界 |
| 2026-06-21 | Codex | 补充名称搜索查询变体兜底约束 | Phase 5 搜索优化需要让带空格词组在原始搜索未命中时继续尝试紧凑查询 |
| 2026-06-21 | Codex | 补充搜索查询长度上限约束 | Phase 5 搜索优化需要限制超长模糊查询进入数据库，降低高并发下的资源放大风险 |
| 2026-06-21 | Codex | 补充记录备注长度上限 | Phase 5 稳定性优化需要限制记录创建备注体积，避免超长文本放大客户端请求、服务端校验和后续分享成本 |
# Audit Trail Addendum

| Date | Author | Scope | Reason |
| --- | --- | --- | --- |
| 2026-06-21 | Codex | Record image request/response contract | Support uploaded record photos being attached to records and shown in recent-record cards |
| 2026-06-21 | Codex | Public record feed request/response contract | Define `GET /api/records/public` and keep record publication explicit opt-in |
| 2026-06-21 | Codex | Public record comments request/response contract | Define latest-comment listing and comment creation for public records only |
| 2026-06-21 | Codex | Search query fallback contract | Define compacted-query retry for spaced food names |
| 2026-06-21 | Codex | Record detail and edit API contract | Add owner-only detail/update endpoints for the mobile record edit flow |

## Phase 2 Addendum: Record Detail And Edit

- `GET /api/records/{recordId}?userId=` returns the full record view for the owner, including existing `images`.
- `PUT /api/records/{recordId}` updates only owner-editable fields: `rating`, `comment`, and `isPublic`.
- The update request body is `{ userId, rating, comment, isPublic }`.
- `rating` must stay in `1..5`; `comment` must stay at or below `500` characters.
- The server must reject editing another user's record with `403` and missing/deleted records with `404`.
- Record images are read-only in this increment; image add/replace remains a later media-edit task.
