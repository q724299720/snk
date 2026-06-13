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

- `item_type`：统一目录实体的语义类型，MVP 阶段仅保留 `packaged_product / dish / fruit`
- `fruit` 在 MVP 阶段保留为独立类型，不并入 `dish`
- 无条形码但明显属于包装食品的条目，仍使用 `packaged_product`
- 包装饮料归入 `packaged_product`，现制饮品归入 `dish`
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
- `deleted_at`

字段说明：

- `FoodRecord` 是一次具体发生的记录事件，不是长期累计评价卡片
- 同一用户可针对同一 `FoodItem` 产生多条 `FoodRecord`
- 同一天同一 `FoodItem` 也允许产生多条 `FoodRecord`，不按自然日去重
- 后续统计、推荐、偏好画像应从多条 `FoodRecord` 聚合，而不是覆盖单条记录
- 服务端正式持久化的 `FoodRecord` 必须绑定 `food_item_id`
- 客户端本地草稿阶段可暂时不绑定，待识别完成或用户手动创建条目后再提交
- `is_public` 默认值应为 `false`
- 公开不应作为记录创建的默认行为，而应作为用户主动执行的后续动作
- `rating` 表示本次记录事件的体验评分，不表示用户对该 `FoodItem` 的长期总评分
- `rating` 在 MVP 阶段采用 `1-5` 的整数星级
- 用户长期偏好、推荐分或平均分应从多条 `FoodRecord` 聚合得出
- `location` 在 MVP 阶段仅作为自由文本字段使用，不绑定独立地点实体
- `price` 在 MVP 阶段表示本次事件中的实际支付或主观感知价格，默认按人民币数值理解
- `price` 不作为 `FoodItem` 固定属性维护，后续价格趋势应从多条 `FoodRecord` 聚合
- 删除采用逻辑删除，`deleted_at` 用于标记记录已被用户删除

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

### ReviewConfigWord

- `id`
- `word`
- `word_type`
- `enabled`
- `source`
- `remark`
- `updated_by`
- `created_at`
- `updated_at`

字段说明：
- `ReviewConfigWord` 用于承载审核规则依赖的小型手工词典
- MVP 阶段优先用于“已知有效词”维护，支持后台热更新
- 词典变更在数据库提交后立即生效，后续审核任务按最新生效数据读取
- 词典后台修改必须具备可追溯审计能力，并采用独立的追加式变更日志保留历史
- 该表不承载自动拒绝阈值等规则参数，相关阈值继续由服务端代码配置维护
- `word_type` 在 MVP 阶段至少支持 `valid_food_word`
- `enabled` 用于热切换词条是否生效，避免直接物理删除
- `source` 可标记 `manual` 等来源，便于后续区分系统生成与人工维护

### ReviewConfigWordAuditLog

- `id`
- `review_config_word_id`
- `action_type`
- `before_value`
- `after_value`
- `operator_id`
- `operator_name`
- `created_at`

字段说明：
- `ReviewConfigWordAuditLog` 用于记录词典后台修改的追加式变更日志
- 每次新增、编辑、启用、停用都追加新日志，不回写历史日志
- `action_type` 在 MVP 阶段至少支持 `create / update / enable / disable`
- `before_value` 与 `after_value` 采用结构化 JSON 快照，用于追踪具体改动内容，支撑误判排查和后续字段扩展
- `operator_id` 与 `operator_name` 用于保留最小可追溯操作人信息
- `ReviewConfigWordAuditLog` 在 MVP 阶段长期保留，不设置自动清理任务

## 索引与约束建议

- `FoodItem.name`、`alias`、`search_keywords` 建立模糊搜索相关索引
- `FoodItem.barcode` 对 `packaged_product` 建立唯一索引或等价唯一约束
- `FoodItem.audit_status` 建立筛选索引
- `FoodRecord.user_id + record_time` 建立组合索引
- 向量字段与图片 embedding 字段按 `pgvector` 能力设计索引

展示与排序约束：

- 用户侧最近记录默认按 `record_time DESC` 排序
- `created_at` 主要用于审计、排查和数据追踪，不作为用户主列表的默认排序字段
- `record_time` 默认取创建当下时间，但允许用户手动修改

## 唯一性规则

- `packaged_product`：同一 `barcode` 视为同一个 `FoodItem`
- `dish`：先按通用菜名建统一条目，不按店铺拆分
- 店铺、价格、个体体验差异优先记录在 `FoodRecord`，而不是拆分新的 `FoodItem`
- 同一条形码对应的新旧包装、封面变化、文案变化，优先通过 `FoodImage`、`alias`、`search_keywords` 吸收，不新建多个 `FoodItem`

## 数据治理约束

- `pending` 条目默认对创建者可见、对审核后台可见、对其他普通用户不可见
- 创建者看到自己的 `pending` 条目时，UI 应明确显示“待审核”状态
- 对创建者本人执行搜索时，`approved` 条目默认排在前，`pending` 条目靠后展示
- 若 `FoodRecord` 绑定 `pending FoodItem`，则在用户记录列表中也应显示轻量待审核提示
- 审核通过后方可进入全局搜索结果
- 报错 / 纠错会影响 `report_count`，供后台排查
- 缩略图与原图都属于长期可追溯资源，删除策略需谨慎
- 记录逻辑删除时，已上传图片和缩略图不立即物理删除，后续由统一清理策略回收
- 后台应支持 `FoodItem` 合并，将历史 `FoodRecord` 迁移到保留条目
- 被合并条目应进入废弃 / merged 状态，不再被普通搜索命中

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
| 2026-06-13 | Codex | 明确评分采用 `1-5` 整数星级 | 已确认 MVP 阶段优先使用最简单稳定的评分体系 |
| 2026-06-13 | Codex | 明确同日同物允许多次记录 | 已确认事件记录不按自然日去重 |
| 2026-06-13 | Codex | 明确 `FoodRecord` 删除采用逻辑删除 | 已确认用户删除记录时系统仍需保留追溯能力 |
| 2026-06-13 | Codex | 明确 `location` 为自由文本字段 | 已确认 MVP 阶段不引入结构化地点实体 |
| 2026-06-13 | Codex | 明确 `price` 为事件价格 | 已确认价格语义属于单次记录事件，而不是目录属性 |
| 2026-06-13 | Codex | 明确 `pending FoodItem` 的可见性规则 | 已确认待审核条目仅创建者和审核后台可见 |
| 2026-06-13 | Codex | 明确同条形码包装演化不新建条目 | 已确认包装变化应被同一目录条目吸收，而不是拆分多个商品 |
| 2026-06-13 | Codex | 明确无条码包装食品仍归属 `packaged_product` | 已确认条形码不是包装食品类型判断的唯一前提 |
| 2026-06-13 | Codex | 收敛 `item_type` 并明确饮料归属规则 | 已确认 `drink` 不单列为类型，包装饮料与现制饮品按不同语义归类 |
| 2026-06-13 | Codex | 明确保留 `fruit` 为独立 `item_type` | 已确认水果在 MVP 阶段具有独立语义，不并入 `dish` |
| 2026-06-13 | Codex | 明确后台需要支持条目合并和记录迁移 | 已确认错误 pending 条目不能只删除，需修正历史记录归属 |
| 2026-06-13 | Codex | 明确最近记录按 `record_time` 排序 | 已确认用户侧记录时间感知优先于系统创建时间 |
| 2026-06-13 | Codex | 明确 `record_time` 默认值与可编辑性 | 已确认快速记录默认当前时间，但允许补记和手动修正 |
| 2026-06-13 | Codex | 明确逻辑删除记录时图片不立即物理删除 | 已确认媒体资源回收应与记录删除解耦，避免误删和追溯丢失 |
| 2026-06-13 | Codex | 明确 pending 条目对创建者需展示待审核标记 | 已确认用户可见不等于正式生效，需清晰区分状态 |
| 2026-06-13 | Codex | 明确创建者搜索时正式条目优先排序 | 已确认 pending 条目不应优先于稳定正式条目展示 |
| 2026-06-13 | Codex | 明确绑定 pending 条目的记录也需显示待审核提示 | 已确认记录列表也应保持状态感知一致性 |
| 2026-06-13 | Codex | 增加 `ReviewConfigWord` 配置表模型 | 已确认小型手工词典应落在数据库并支持后台热更新 |
| 2026-06-13 | Codex | 明确 `ReviewConfigWord` 更新后立刻生效 | 已确认审核任务无需等待缓存刷新即可读取最新词典 |
| 2026-06-13 | Codex | 明确 `ReviewConfigWord` 需要可追溯审计 | 已确认后台词典治理必须保留操作历史以追查误判来源 |
| 2026-06-13 | Codex | 增加 `ReviewConfigWordAuditLog` 追加式日志模型 | 已确认词典审计需要保留前后值与完整变更历史 |
| 2026-06-13 | Codex | 明确 `ReviewConfigWordAuditLog` 前后值采用 JSON 快照 | 已确认日志字段需要兼容后续词典字段扩展 |
| 2026-06-13 | Codex | 明确 `ReviewConfigWordAuditLog` 长期保留 | 已确认 MVP 阶段不对词典审计日志做自动清理 |
| 2026-06-13 | Codex | 明确 `ReviewConfigWord` 不承载审核阈值参数 | 已确认阈值应继续由代码配置维护，避免后台规则漂移 |
