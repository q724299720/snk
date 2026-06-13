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
- 无条形码但明显属于包装食品的条目，仍使用 `packaged_product`
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

字段说明：

- `FoodRecord` 是一次具体发生的记录事件，不是长期累计评价卡片
- 同一用户可针对同一 `FoodItem` 产生多条 `FoodRecord`
- 后续统计、推荐、偏好画像应从多条 `FoodRecord` 聚合，而不是覆盖单条记录
- 服务端正式持久化的 `FoodRecord` 必须绑定 `food_item_id`
- 客户端本地草稿阶段可暂时不绑定，待识别完成或用户手动创建条目后再提交
- `is_public` 默认值应为 `false`
- 公开不应作为记录创建的默认行为，而应作为用户主动执行的后续动作
- `rating` 表示本次记录事件的体验评分，不表示用户对该 `FoodItem` 的长期总评分
- 用户长期偏好、推荐分或平均分应从多条 `FoodRecord` 聚合得出
- `location` 在 MVP 阶段仅作为自由文本字段使用，不绑定独立地点实体
- `price` 在 MVP 阶段表示本次事件中的实际支付或主观感知价格，默认按人民币数值理解
- `price` 不作为 `FoodItem` 固定属性维护，后续价格趋势应从多条 `FoodRecord` 聚合

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
- `FoodItem.barcode` 对 `packaged_product` 建立唯一索引或等价唯一约束
- `FoodItem.audit_status` 建立筛选索引
- `FoodRecord.user_id + record_time` 建立组合索引
- 向量字段与图片 embedding 字段按 `pgvector` 能力设计索引

## 唯一性规则

- `packaged_product`：同一 `barcode` 视为同一个 `FoodItem`
- `dish`：先按通用菜名建统一条目，不按店铺拆分
- 店铺、价格、个体体验差异优先记录在 `FoodRecord`，而不是拆分新的 `FoodItem`
- 同一条形码对应的新旧包装、封面变化、文案变化，优先通过 `FoodImage`、`alias`、`search_keywords` 吸收，不新建多个 `FoodItem`

## 数据治理约束

- `pending` 条目默认对创建者可见、对审核后台可见、对其他普通用户不可见
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
| 2026-06-13 | Codex | 补充 `FoodItem` 唯一性规则 | 已确认包装食品按条形码唯一、菜品按通用菜名统一建模 |
| 2026-06-13 | Codex | 明确 `FoodRecord` 为一次事件记录 | 已确认记录对象不是长期卡片，而是一次具体发生的行为 |
| 2026-06-13 | Codex | 明确 `FoodRecord` 绑定规则 | 已确认正式记录必须绑定 `FoodItem`，但本地草稿允许临时未绑定 |
| 2026-06-13 | Codex | 明确 `FoodRecord` 默认私有 | 已确认记录创建默认仅自己可见，公开应为主动动作 |
| 2026-06-13 | Codex | 明确 `FoodRecord.rating` 为事件评分 | 已确认评分语义针对本次体验，而不是长期总分 |
| 2026-06-13 | Codex | 明确 `location` 为自由文本字段 | 已确认 MVP 阶段不引入结构化地点实体 |
| 2026-06-13 | Codex | 明确 `price` 为事件价格 | 已确认价格语义属于单次记录事件，而不是目录属性 |
| 2026-06-13 | Codex | 明确 `pending FoodItem` 的可见性规则 | 已确认待审核条目仅创建者和审核后台可见 |
| 2026-06-13 | Codex | 明确同条形码包装演化不新建条目 | 已确认包装变化应被同一目录条目吸收，而不是拆分多个商品 |
| 2026-06-13 | Codex | 明确无条码包装食品仍归属 `packaged_product` | 已确认条形码不是包装食品类型判断的唯一前提 |
