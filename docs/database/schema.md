# 数据库设计

## 文档职责

本文件记录核心数据模型、关键字段语义、数据库迁移与索引策略。

## 迁移策略

- 数据库结构必须通过 Flyway 或 Liquibase 管理
- 不允许依赖 `hibernate.hbm2ddl.auto=update`
- 迁移脚本建议统一放在 `db/migration` 或框架约定目录
- 涉及字段、索引、扩展变更时，必须同步更新本文件

## PostgreSQL 基线

- 主数据库：PostgreSQL
- 模糊搜索扩展：`pg_trgm`
- 向量检索扩展：`pgvector`

## 核心实体

### User

- `id`
- `nickname`
- `avatar`
- `phone / email`
- `auth_provider`
- `created_at`
- `status`

### FoodItem

- `id`
- `name`
- `item_type`
- `alias`
- `category`
- `brand`
- `barcode`
- `source`：`system / user_generated / external_api`
- `audit_status`：`pending / approved / rejected`
- `search_keywords`
- `report_count`
- `description`
- `cover_image_url`
- `tags`
- `nutrition_info`
- `created_at`
- `updated_at`

字段说明：

- `item_type`：统一目录实体的语义类型，建议首批支持 `packaged_product / dish / drink / fruit`
- `search_keywords`：用于沉淀别名、OCR 扫描文本、包装关键词，提高模糊搜索召回率
- `report_count`：用于累计用户报错 / 纠错次数，辅助后台识别低质量 UGC 条目

### FoodImage

- `id`
- `food_item_id`
- `image_url`
- `thumbnail_url`
- `image_type`
- `width`
- `height`
- `embedding_status`
- `created_at`

字段说明：

- `thumbnail_url`：列表页和历史记录页优先加载缩略图，避免直接请求原图

### FoodRecord

- `id`
- `user_id`
- `food_item_id`
- `source_type`：`text_search / image_search / manual`
- `sync_status`：`draft / syncing / synced / failed`
- `is_public`
- `rating`
- `comment`
- `price`
- `location`
- `record_time`
- `created_at`

### FoodRecordImage

- `id`
- `record_id`
- `image_url`
- `thumbnail_url`
- `created_at`

### RecognitionTask

- `id`
- `user_id`
- `input_image_url`
- `status`
- `top_candidates`
- `selected_food_item_id`
- `confidence`
- `created_at`
- `finished_at`

### Tag

- `id`
- `name`
- `tag_type`
- `created_at`

## 索引与约束建议

- `FoodItem.name`、`alias`、`search_keywords` 建立模糊搜索相关索引
- `FoodItem.barcode` 建立唯一索引或高选择性索引
- `FoodItem.audit_status` 建立筛选索引
- `FoodRecord.user_id + record_time` 建立组合索引
- 向量字段与图片 embedding 字段按 `pgvector` 能力设计索引

## 数据治理约束

- `pending` 条目默认仅创建者可见
- 审核通过后方可进入全局搜索结果
- 报错 / 纠错会影响 `report_count`，供后台排查
- 缩略图与原图都属于长期可追溯资源，删除策略需谨慎

## 变更记录维护规则

- 每次修改本文件时，必须在下方追加一条记录

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 从 `agents.md` 拆出数据模型、迁移策略与索引建议 | 将数据库设计独立管理，便于后续迁移迭代 |
| 2026-06-13 | Codex | 为 `FoodItem` 增加 `item_type` 语义约束 | 已确认目录实体统一建模，但需要类型字段区分包装食品与菜品 |
