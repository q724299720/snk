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

- `GET /api/foods/search?q=`
- `GET /api/foods/barcode/{code}`
- `POST /api/foods/manual`
- `GET /api/foods/{id}`
- `GET /api/foods/recommend`

文本搜索当前约束：

- `GET /api/foods/search?q=` 当前返回已审核通过的基础食物条目
- 空白 `q` 直接返回 `400`
- 当前响应包含 `items` 与 `qualitySignal`
- `items[*]` 当前最小字段包含：`id`、`name`、`itemType`、`category`、`subcategory`、`brand`、`barcode`、`coverImageUrl`、`auditStatus`
- `qualitySignal` 当前最小取值：`strong / weak`

条码查询当前约束：

- `GET /api/foods/barcode/{code}` 当前返回单个精确命中条目
- 空白条码直接返回 `400`
- 未命中时返回 `404`
- 当前响应体字段与 `items[*]` 一致，包含 `auditStatus`

手动创建条目当前约束：

- `POST /api/foods/manual` 当前最小请求字段包含：`userId`、`name`、`itemType`、`category`
- `subcategory`、`brand`、`barcode` 当前为可选字段
- `itemType` 当前仅允许：`packaged_product / dish / fruit`
- `barcode` 当前仅建议用于 `packaged_product`
- 服务端成功后直接返回新建的 `FoodItem` 响应体
- 当前新建条目固定写入：`source = user_generated`、`auditStatus = pending`
- 当前响应体字段包含：`id`、`name`、`itemType`、`category`、`subcategory`、`brand`、`barcode`、`coverImageUrl`、`auditStatus`
- 当前客户端在创建成功后直接进入“记录创建”页，不要求用户重新搜索
- 当前客户端在扫码未命中时，会携带原始 `barcode` 进入手动创建页，便于后端沉淀包装食品条目

### 图片与识别

- `POST /api/upload/image`
- `POST /api/recognition/barcode`
- `POST /api/recognition/ocr`
- `POST /api/recognition/tasks`
- `GET /api/recognition/tasks/{id}`

图片上传约束：

- `POST /api/upload/image` 使用 `multipart/form-data`
- 首版字段采用 `file`
- 服务端当前返回 `objectKey`、`resourceUrl`、`contentType`、`size`
- `resourceUrl` 在未配置公网前缀时可返回相对路径；配置 `SNK_STORAGE_PUBLIC_BASE_URL` 后应返回可被真机直接访问的绝对地址，当前部署目标为 `https://snk.qiuxinmin.cn`
- 首版仅接受图片 `MIME` 类型，不接受通用文件上传
- 开发环境当前采用本地文件系统存储，对外暴露 `resourceUrl` 静态访问路径；后续可替换为 MinIO / S3

### 记录管理

- `POST /api/records`
- `GET /api/records/my`
- `GET /api/records/{id}`
- `PUT /api/records/{id}`
- `DELETE /api/records/{id}`

记录创建当前约束：

- `POST /api/records` 当前最小请求字段包含：`userId`、`foodItemId`、`sourceType`、`isPublic`、`rating`
- `comment`、`recordTime` 当前为可选字段
- 当前 `rating` 允许范围为 `1-5`
- 当前 `sourceType` 已落地：`text_search / image_search / manual`
- 当前成功响应返回：`id`、`userId`、`foodItemId`、`sourceType`、`isPublic`、`rating`、`comment`、`recordTime`、`createdAt`

### 标签与分类

- `GET /api/tags`
- `GET /api/categories`

## 管理后台接口范围

- 食物条目管理
- 图片样本管理
- 用户记录审核
- 用户报错 / 纠错处理
- 标签体系管理
- 识别任务监控
- 统计报表

## OCR 与识别接口边界

### 本地优先原则

- 本地 ML Kit OCR 成功时，客户端优先直接调用 `GET /api/foods/search?q=` 做文本召回
- 有条形码时，优先调用条形码链路，不进入 OCR 链路

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

### 图像识别兜底原则

`POST /api/recognition/tasks` 主要用于以下场景：

- 无条形码
- 本地 OCR 与服务端 OCR 都未召回有效候选
- 需要给自然食物图片返回候选食物列表

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
| 2026-06-14 | Codex | 补充手动创建待审核条目接口与 `auditStatus` 响应字段 | Phase 3 已落地搜索失败后的手动创建条目闭环，接口文档需与实现对齐 |
| 2026-06-14 | Codex | 补充手动创建条目的可选 `barcode` 入参与扫码未命中的前端流转 | Phase 3 需覆盖包装食品扫码未命中的 UGC 沉淀场景 |
| 2026-06-14 | Codex | 补充服务端 OCR 接口的 multipart 入参、响应字段与 provider 降级语义 | Phase 3 已落地本地 OCR 失败后的服务端 OCR 回退链路 |
