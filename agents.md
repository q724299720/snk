# SNK 项目全局索引

## 文档职责

`agents.md` 是项目的全局核心看板，只记录以下内容：

- 项目级硬约束
- 顶层架构结论
- 当前实施顺序
- 文档索引
- 文档变更审计记录

除上述内容外，业务细节、技术细节、接口细节、数据库细节、识别方案细节均必须拆分到 `docs/` 下的独立文档维护。

## 强制工作流

在任何代码、配置、脚本、数据库或部署相关操作前，必须先执行以下步骤：

1. 先阅读本文件 `agents.md`
2. 根据当前任务定位对应子文档
3. 确认当前开发阶段是否符合 [docs/architecture/implementation-plan.md](docs/architecture/implementation-plan.md) 的顺序约束
4. 按模块逐步实现，不允许跨阶段跳做
5. 每完成一个增量后立即执行对应测试或验证
6. 如果修改了任何文档，必须同步更新该文档内的“变更记录”
7. 如果修改了项目文件，必须提交到本地 Git，并推送到远程 GitHub
8. 每一步完成后，必须向用户回报：`commit id`、修改内容、用户需要进行的操作、验证 / 测试方式

如果任务与现有文档冲突，以 `agents.md` 的全局约束和 `docs/architecture/implementation-plan.md` 的阶段顺序为准。

## 当前全局结论

- 产品定位：安卓端美食 / 零食分享与记录 App
- 识别主路径：`条形码 > 本地 OCR 文本搜索 > 服务端 OCR 兜底 > 图像识别`
- 后端 MVP 原则：`All-in-PostgreSQL`
- 数据策略：先解决冷启动，再扩智能识别
- 客户端体验底线：支持离线草稿与弱网补传
- UGC 原则：未审核条目仅创建者可见，审核后再全局可见

## 当前部署约束

- 目标部署环境：腾讯轻量云 `2 核 4G / 40GB`，已安装宝塔面板
- 架构决策必须优先考虑低运维复杂度、低常驻进程数量和可在单机上稳定运行
- MVP 阶段避免引入 Elasticsearch、Redis、独立向量库、消息队列等额外常驻基础设施，除非后续容量数据明确证明必要

## 当前实施顺序

必须严格按以下顺序推进，并在每一步完成后做增量测试：

1. `Phase 0`：冷启动数据准备
2. `Phase 1`：后端基础与数据库迁移
3. `Phase 2`：安卓端基础记录闭环
4. `Phase 3`：识别链路接入
5. `Phase 4`：后台治理与审核
6. `Phase 5`：增强能力与优化

详细拆步、交付物和测试要求见：
[docs/architecture/implementation-plan.md](docs/architecture/implementation-plan.md)

当前从 `Phase 0` 开始逐步推进，不允许跨阶段跳做。

## 文档索引

| 路径 | 作用 |
| --- | --- |
| `docs/product/prd.md` | 产品定位、用户、MVP、扩展范围、冷启动策略 |
| `docs/product/food-taxonomy.md` | 首批食物分类、标签体系与冷启动数据范围定义 |
| `docs/product/external-data-strategy.md` | 外部开放数据源选择、接入边界、批量导入与合规约束 |
| `docs/architecture/system-design.md` | 系统架构、技术选型、模块结构、安全与运维 |
| `docs/architecture/implementation-plan.md` | 开发阶段拆解、模块化实施顺序、增量测试要求 |
| `docs/api/api-contract.md` | App / 后台接口边界、识别链路接口职责 |
| `docs/database/schema.md` | 数据模型、字段约束、索引与迁移策略 |
| `docs/database/seed-data-spec.md` | 冷启动种子数据字段规范、最低规模和导入校验规则 |
| `docs/database/import-script-plan.md` | 冷启动种子数据导入脚本方案、目录约定与脚本骨架说明 |
| `docs/recognition/recognition-plan.md` | 条形码、OCR、图像识别的策略与兜底流程 |
| `docs/process/document-governance.md` | 文档维护规则、审计记录规则、协作约束 |
| `docs/process/github-workflow.md` | GitHub 仓库创建、远程连接记录与日常提交流程 |

## 当前开发前必查文档

按任务类型读取：

- 产品或范围调整：`docs/product/prd.md`
- 冷启动数据准备：`docs/product/food-taxonomy.md`、`docs/database/seed-data-spec.md`
- 外部数据源接入：`docs/product/external-data-strategy.md`
- 写导入脚本前：`docs/database/import-script-plan.md`
- 编码前的阶段确认：`docs/architecture/implementation-plan.md`
- Android / 后端架构实现：`docs/architecture/system-design.md`
- 写接口前：`docs/api/api-contract.md`
- 改数据库前：`docs/database/schema.md`
- 改识别链路前：`docs/recognition/recognition-plan.md`
- 改任何文档前：`docs/process/document-governance.md`
- 推送代码前：`docs/process/github-workflow.md`

## 变更记录维护规则

- 每次修改本文件，必须在下方“变更记录”追加一条记录
- 每次修改子文档，也必须在对应子文档的“变更记录”追加记录
- 记录至少包含：日期、修改人、变更范围、原因

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 重构 `agents.md` 为全局索引、执行规范和文档看板 | 按要求将细节拆分至独立文档，并建立可追溯工作流 |
| 2026-06-13 | Codex | 增加 GitHub 提交约束与 GitHub 工作流文档索引 | 将后续每次修改提交远程仓库的要求纳入全局规则 |
| 2026-06-13 | Codex | 增加逐步推进后的固定回报项，并补充冷启动数据文档索引 | 后续每一步都需要回报 commit id、改动、操作和测试方式 |
| 2026-06-13 | Codex | 增加外部数据源策略文档索引与开发前必查入口 | Phase 0 已推进到外部开放数据源接入策略，需要纳入全局索引 |
| 2026-06-13 | Codex | 增加导入脚本方案文档索引与开发前必查入口 | Phase 0 已推进到种子数据导入脚本方案，需要纳入全局索引 |
| 2026-06-13 | Codex | 增加部署环境约束 | 已确认目标服务器为腾讯轻量云 2 核 4G 40GB + 宝塔面板，后续架构需按单机低运维前提收敛 |
