# SNK 产品简化、图片与记录编辑实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 前后端取消产品分类展示与输入，新产品自动审核并全局可搜索，记录默认公开，搜索/发现展示图片，记录名称和图片可进入编辑页。

**Architecture:** 数据库保留分类列用于兼容，但迁移统一为 `none/NULL`，服务端不再把分类作为创建、筛选或展示必需信息。图片使用“记录首图→产品主图→占位图”的一致回退顺序，记录编辑继续复用现有 PUT 接口。

**Tech Stack:** PostgreSQL/Flyway、Spring Boot/JPA、Kotlin、Jetpack Compose、Coil、Navigation Compose、JUnit、Compose UI Test。

## Task 1: 统一无分类、自动审核与产品上下架

**Files:**
- Create: `server/src/main/resources/db/migration/V15__simplify_food_items.sql`
- Modify: `server/src/main/java/com/snk/server/infrastructure/persistence/food/FoodItemEntity.java`
- Modify: `server/src/main/java/com/snk/server/infrastructure/persistence/food/FoodItemRepository.java`
- Modify: `server/src/main/java/com/snk/server/domain/record/FoodRecordService.java`
- Modify: `server/src/main/java/com/snk/server/domain/food/ManualFoodItemService.java`
- Modify: `server/src/main/java/com/snk/server/domain/food/FoodModerationService.java`
- Modify: `server/src/main/java/com/snk/server/api/controller/AdminFoodItemController.java`
- Create: `server/src/test/java/com/snk/server/domain/food/FoodItemSimplificationTests.java`

- [ ] 先写失败集成测试：历史 category 全部迁移为 none/subcategory null；快速和手动新建均 `approved + is_searchable=true`；隐藏后搜索不到但历史记录仍可读；恢复后重新可搜。
- [ ] V15 增加非空 `is_searchable boolean default true`，批量更新 category/subcategory，并为 `audit_status='approved' AND is_searchable` 搜索条件建立合适索引。
- [ ] 所有产品创建/编辑路径强制 `category="none"`、`subcategory=null`、`auditStatus="approved"`；客户端传入旧分类值也忽略或在旧 API 兼容层归一化。
- [ ] 增加 `POST /api/admin/products/{id}/hide` 与 `/restore`；隐藏只更新 `is_searchable`，不得修改历史记录、图片、评论或 audit_status。
- [ ] 删除后台待分类筛选、计数和分类编辑字段；合并目标继续要求 approved 且允许隐藏目标由管理员先恢复。
- [ ] 运行新增测试、`./gradlew test` 和本地 curl 搜索隐藏/恢复回归。
- [ ] 提交并推送：`feat: simplify products and add search visibility`

## Task 2: API 移除分类展示并默认公开

### Task 1 实施状态（2026-07-12）

已新增 V15：历史产品归一为 `category=none/subcategory=NULL`，并增加 `is_searchable`。手动创建、快速记录创建和后台编辑均自动写入无分类、`approved` 与可搜索；后台提供隐藏/恢复操作，隐藏不修改审核状态或历史记录关联。全局搜索和精确匹配均要求 `approved && is_searchable`；后台页面已移除待分类和分类编辑控件。

### Task 2 实施状态（2026-07-12）

App 搜索、历史记录和手动创建 API 已移除分类字段；手动创建请求不再接收分类，服务端统一写入 `none/null`。普通和快速记录省略 `isPublic` 时统一默认公开，显式 `false` 仍保留私密语义。接口契约已同步更新，并为搜索、历史记录和快速创建补充无分类/默认公开测试。

### Task 3 实施状态（2026-07-12）

Android 搜索、记录历史和手动创建 DTO 已移除分类字段；手动创建、搜索候选、创建记录、编辑记录、首页记录和草稿卡片均不展示分类。Room 草稿兼容列仅供历史迁移与补传使用，新草稿写入 `none/null`。新增 Compose 仪器测试，确保手动创建页无分类输入。

### Task 4 实施状态（2026-07-12）

新增统一 `ProductImage` 组件，搜索与发现页均使用一致的裁剪、加载/失败回退和无障碍描述。发现页按记录缩略图、记录原图、产品主图的顺序取图；无图或加载失败时显示不重复朗读的“暂无图片”占位。已覆盖优先级单元测试、Compose 测试和真机仪器测试。

**Files:**
- Modify: `server/src/main/java/com/snk/server/api/dto/FoodSearchItemResponse.java`
- Modify: `server/src/main/java/com/snk/server/api/dto/FoodRecordHistoryResponse.java`
- Modify: `server/src/main/java/com/snk/server/api/dto/CreateManualFoodItemRequest.java`
- Modify: `server/src/main/java/com/snk/server/api/dto/CreateQuickFoodRecordRequest.java`
- Modify: `server/src/main/java/com/snk/server/api/dto/CreateFoodRecordRequest.java`
- Modify: `server/src/main/java/com/snk/server/api/controller/FoodSearchController.java`
- Modify: `server/src/main/java/com/snk/server/api/controller/FoodRecordController.java`
- Modify: `server/src/test/java/com/snk/server/api/controller/FoodSearchControllerTests.java`
- Modify: `server/src/test/java/com/snk/server/api/controller/FoodRecordControllerTests.java`

- [ ] 先修改契约测试，断言新响应不含 category/subcategory，新建请求省略 `isPublic` 时结果为 true，显式 false 仍保持私密；运行指定测试确认失败。
- [ ] 请求 DTO 将 `Boolean isPublic` 归一化为 `isPublic == null || isPublic`；Android 新版仍显式发送 true，服务端默认用于旧/其他客户端保护。
- [ ] 从面向 App 的搜索、历史和产品创建 DTO 移除分类字段；数据库/domain 内部兼容字段不必立即删除。
- [ ] 搜索结果始终返回 `coverImageUrl`；公开记录历史始终返回 record images 和 product cover，空值使用 null/空数组而不是伪造 URL。
- [ ] 更新 `docs/api/api-contract.md` 与变更记录，给旧字段标明移除版本。
- [ ] 运行指定测试、`./gradlew test`。
- [ ] 提交并推送：`refactor: remove categories from app api contracts`

## Task 3: Android 移除全部分类 UI

**Files:**
- Modify: `android-app/app/src/main/java/com/snk/app/data/food/FoodSearchApi.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/data/record/FoodRecordApi.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/FoodSearchResultsCard.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/RecordEditScreen.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/GalleryScreen.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/QuickRecordScreen.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/ManualFoodCreateScreen.kt`
- Create: `android-app/app/src/androidTest/java/com/snk/app/ui/NoCategoryUiTest.kt`

- [ ] 先写 Compose 失败测试，遍历首页搜索、极简记录、手动创建、记录、编辑、发现和后台外 App 页面，断言不存在“分类/子分类/待分类/uncategorized”。
- [ ] 删除 Android DTO 的 category/subcategory 字段并修正映射；若滚动发布需兼容旧服务端，使用可忽略未知 JSON 字段而不保留 UI 字段。
- [ ] 搜索卡片辅助信息只显示品牌和评分；记录/编辑页只显示产品名、品牌、评分、备注、公开状态和图片。
- [ ] 删除分类输入、筛选、验证、TalkBack 描述和空状态文案，不用空白占位保留原布局。
- [ ] 运行新增 Compose 测试、`testDebugUnitTest`、`assembleDebug`。
- [ ] 提交并推送：`refactor(android): remove category user interface`

## Task 4: 搜索与发现图片显示

**Files:**
- Modify: `android-app/app/src/main/java/com/snk/app/ui/FoodSearchResultsCard.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/DiscoverScreen.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/data/record/FoodRecordApi.kt`
- Create: `android-app/app/src/androidTest/java/com/snk/app/ui/ProductImageDisplayTest.kt`

- [ ] 先写 Compose 失败测试：搜索有主图显示图片、无主图显示统一占位；发现优先 record thumbnail/image，其次 product cover，最后占位；加载失败显示占位和可读描述。
- [ ] 抽取或复用统一 `ProductImage` Composable，尺寸/裁剪统一为 `ContentScale.Crop`，加载中不导致卡片跳动。
- [ ] 搜索图片 contentDescription 为“{产品名}图片”；发现图片为“{产品名}记录图片”；装饰占位不重复朗读。
- [ ] Coil 请求使用 HTTPS URL；服务端相对路径通过 `BuildConfig.API_BASE_URL` 解析，禁止字符串直接拼接双斜杠。
- [ ] 保持“记一笔”为搜索结果唯一主按钮，图片加载失败不阻塞点击。
- [ ] 运行新增测试、全量测试、APK；ADB 在有图/无图/断网三种状态截图检查。
- [ ] 提交并推送：`feat(android): show product images in search and discover`

## Task 5: 记录名称/图片进入编辑并即时保存

**Files:**
- Modify: `android-app/app/src/main/java/com/snk/app/ui/GalleryScreen.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/SnkApp.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/ui/RecordEditScreen.kt`
- Modify: `android-app/app/src/main/java/com/snk/app/data/record/FoodRecordRepository.kt`
- Modify: `android-app/app/src/test/java/com/snk/app/ui/RecordEditScreenTest.kt`
- Create: `android-app/app/src/androidTest/java/com/snk/app/ui/RecordNavigationTest.kt`

- [ ] 先写失败测试：点击记录名称或图片均导航到 `record/{recordId}/edit`；状态标签和菜单不误触发导航；保存后回记录列表并刷新；首页仍可点击（覆盖既往回归 bug）。
- [ ] Gallery 卡片只给名称和图片明确 clickable/语义 role；使用稳定 recordId 路由，不把完整记录 JSON 塞入 navigation argument。
- [ ] 编辑页进入时调用 `GET /api/records/{id}` 获取最新版本；404 显示“记录不存在”并返回，403 显示无权限且不得展示缓存详情。
- [ ] 修改评分、备注、公开状态和图片后点击保存立即 PUT；保存中禁用重复点击，失败保留输入并允许重试，成功清理已上传的替换图片临时文件。
- [ ] 新建页 `isPublic` 初值改为 true；用户仍可主动关闭公开。不得新增每日或延时批量提交逻辑。
- [ ] 运行新增测试、全量测试、APK；ADB 验证记录→编辑→保存→首页、发现→首页导航。
- [ ] 提交并推送：`feat(android): edit records from gallery cards`

## Task 6: 后台页面简化与产品隐藏恢复

**Files:**
- Modify: `server/src/main/resources/static/admin/index.html`
- Modify: `server/src/test/java/com/snk/server/api/controller/AdminFoodItemControllerTests.java`

- [ ] 先写/更新控制器测试，覆盖隐藏、恢复、搜索排除和历史保留；运行测试确认页面/API 改动前失败。
- [ ] 后台移除分类输入、分类列、待分类快捷筛选与计数；增加“可搜索/已隐藏”状态、隐藏和恢复操作。
- [ ] 产品编辑只包含名称、类型、品牌、别名、关键词和主图；提交时服务端统一写 category none。
- [ ] 删除危险操作前显示产品名和影响说明；隐藏明确提示“不会删除历史记录”，恢复后提示“重新进入全局搜索”。
- [ ] 使用浏览器回归筛选、编辑、隐藏、恢复、合并以及账号管理共存布局。
- [ ] 运行 `./gradlew test` 和正式域名后台只读/测试账号验证。
- [ ] 提交并推送：`feat(admin): simplify products and manage visibility`

## Task 7: 产品体验总回归与发布

**Files:**
- Modify: `docs/architecture/implementation-plan.md`
- Modify: `docs/product/prd.md`

- [ ] 服务端执行 `./gradlew clean test`；Android 执行 `./gradlew clean testDebugUnitTest assembleDebug connectedDebugAndroidTest`。
- [ ] 在真机验证：首页有图搜索、无结果创建、默认公开、发现图片、记录名称/图片编辑、保存失败重试、保存后返回首页。
- [ ] 在正式 API 验证新产品立即可搜、隐藏后不可搜、恢复后可搜、历史记录图片/评论/点赞不变。
- [ ] 使用 `rg -n "uncategorized|待分类|category|subcategory" android-app/app/src/main server/src/main/resources/static/admin` 审查残留；只允许数据库兼容代码与非展示字段。
- [ ] 更新实施计划状态和文档变更记录，记录 APK、服务端包、数据库迁移版本与回滚点。
- [ ] 提交并推送：`test: complete authenticated product experience regression`

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-07-11 | Codex | 新建产品简化、图片与记录编辑实施计划 | 把无分类、自动审核、默认公开和媒体体验拆成可验证任务 |
| 2026-07-12 | Codex | 完成 Task 1 无分类、自动审核与搜索可见性 | 落实所有产品不分类、新产品自动审核和后台隐藏恢复 |
| 2026-07-12 | Codex | 完成 Task 2 API 去分类字段与默认公开 | 将 App API 契约收敛为无分类，记录创建默认公开 |
| 2026-07-12 | Codex | 完成 Task 3 Android 去分类模型与界面 | 移除客户端分类输入、展示与 DTO 字段，保留草稿迁移兼容 |
| 2026-07-12 | Codex | 完成 Task 4 搜索与发现图片展示 | 统一产品图片回退、发现页图片展示与无障碍描述 |
