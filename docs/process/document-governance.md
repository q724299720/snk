# 文档治理规则

## 文档职责

本文件定义项目文档的阅读顺序、维护约束和审计记录规则。

## 阅读顺序

在开始任何开发任务前，必须按以下顺序阅读：

1. `agents.md`
2. `docs/architecture/implementation-plan.md`
3. 与当前任务相关的专题文档

如果任务类型涉及：

- 产品：读 `docs/product/prd.md`
- 架构：读 `docs/architecture/system-design.md`
- 接口：读 `docs/api/api-contract.md`
- 数据库：读 `docs/database/schema.md`
- 识别链路：读 `docs/recognition/recognition-plan.md`

## 文档拆分原则

- `agents.md` 只保留全局信息和索引
- 业务细节、技术细节、接口细节、数据库细节必须进入子文档
- 子文档之间允许引用，但不可替代 `agents.md` 的全局约束

## 修改原则

- 修改任何文档时，必须同步更新该文档的“变更记录”
- 若修改影响全局执行顺序、架构结论或文档索引，必须同步修改 `agents.md`
- 若修改影响开发阶段顺序，必须同步修改 `docs/architecture/implementation-plan.md`
- 若修改影响字段或状态，必须同步修改 `docs/database/schema.md` 与 `docs/api/api-contract.md`
- 若修改完成后产生项目文件变更，必须按 `docs/process/github-workflow.md` 执行提交与推送

## 审计记录格式

每条记录至少包含：

- 日期
- 修改人
- 变更范围
- 变更原因

推荐格式：

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 文档路径或章节 | 简述变更原因 |

## 最低合规要求

- 不允许只改代码不改相关文档
- 不允许改文档但不写审计记录
- 不允许跳过 `agents.md` 直接进入专题文档

## 变更记录维护规则

- 每次修改本文件时，必须在下方追加一条记录

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 新建文档治理规则与审计记录规范 | 满足项目文档可追溯和前置阅读要求 |
| 2026-06-13 | Codex | 增加 GitHub 提交与推送约束 | 将文档治理与远程仓库同步流程绑定 |
