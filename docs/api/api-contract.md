# 接口边界说明

## 文档职责

本文件记录 App 端与后台的接口边界、职责分工和识别链路接口约束。正式 `OpenAPI` 文档后续可在此基础上生成。

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
- `GET /api/foods/{id}`
- `GET /api/foods/recommend`

### 图片与识别

- `POST /api/upload/image`
- `POST /api/recognition/barcode`
- `POST /api/recognition/ocr`
- `POST /api/recognition/tasks`
- `GET /api/recognition/tasks/{id}`

### 记录管理

- `POST /api/records`
- `GET /api/records/my`
- `GET /api/records/{id}`
- `PUT /api/records/{id}`
- `DELETE /api/records/{id}`

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

- 先维护本文件，再落正式 `OpenAPI`
- 涉及字段新增、状态枚举变化、鉴权变化时，必须同步更新数据库文档
- 识别链路接口调整时，必须同步更新 `docs/recognition/recognition-plan.md`

## 变更记录维护规则

- 每次修改本文件时，必须在下方追加一条记录

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 从 `agents.md` 拆出接口边界、OCR 分工和识别接口职责 | 防止本地 OCR 与服务端 OCR 边界不清 |
| 2026-06-13 | Codex | 明确接口需返回候选质量信号 | 已确认兜底入口展示由后端信号与前端场景共同决定 |
| 2026-06-13 | Codex | 增加匿名用户初始化接口与安装级复用约束 | 当前 Phase 1 已开始落地游客模式的最小服务端身份闭环 |
