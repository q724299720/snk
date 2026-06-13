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
- 数据库定时备份
- 数据库迁移脚本纳入发布流程
- 图片对象存储生命周期管理
- 上传后异步生成缩略图
- 逻辑删除记录后，已上传图片通过异步或定期回收策略清理，而不是立即物理删除
- API 日志与链路追踪
- 识别失败率与外部数据导入告警

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
