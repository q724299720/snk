# 种子数据导入脚本方案

## 文档职责

本文件定义 `Phase 0 / Step 5` 的导入脚本输入输出、目录约定、执行流程、校验规则和仓库内脚本骨架。

## 目标

在后端工程尚未初始化前，先把冷启动数据导入流程固定为可脚本化、可复用、可增量验证的离线工具链。

MVP 阶段导入脚本需要支持：

- 从人工整理的条码清单拉取 Open Food Facts 数据
- 将外部数据归一化到本项目的 `FoodItem` 种子字段
- 输出可再次人工校对的 CSV
- 在真正入库前先完成字段级校验

## 仓库内当前产物

- 输入模板：`data/seed/openfoodfacts-barcodes-template.csv`
- 条码导入脚本骨架：`tools/seed/import-openfoodfacts-barcodes.ps1`
- 已有种子样本：`data/seed/food-items-sample.csv`

## 目录约定

```text
data/
  seed/
    food-items-sample.csv
    openfoodfacts-barcodes-template.csv
tools/
  seed/
    import-openfoodfacts-barcodes.ps1
```

约束：

- `data/seed/` 只放输入样本、输出预览和人工校对后的中间文件
- `tools/seed/` 只放导入、转换、校验脚本
- 不把临时下载的大体积外部原始数据直接提交到仓库

## 输入文件规范

`openfoodfacts-barcodes-template.csv` 当前列定义：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `barcode` | 是 | 待拉取的条形码 |
| `brand_hint` | 否 | 人工已知品牌提示，便于后续核对 |
| `category_hint` | 否 | 预期一级分类，默认可回落为 `零食` |
| `subcategory_hint` | 否 | 预期二级分类 |
| `alias_hint` | 否 | 常见简称提示 |
| `tag_hint` | 否 | 口味 / 场景标签提示，多个值使用 `|` |
| `enabled` | 是 | 是否参与本次导入，`true / false` |

## 输出文件规范

脚本输出统一对齐本项目种子数据列：

```text
name,item_type,category,subcategory,brand,barcode,alias,search_keywords,tags,source,audit_status
```

输出约束：

- `item_type` 统一写入 `packaged_product`
- `source` 统一写入 `external_api`
- `audit_status` 统一写入 `approved`
- `search_keywords` 由脚本根据名称、品牌、分类、别名自动生成

## 导入流程

### 模式 A：Dry Run

用途：

- 校验输入 CSV 是否可解析
- 校验条码格式和启用状态
- 不发起网络请求，只输出本次将处理的条码数量

### 模式 B：Live Import

用途：

- 对启用条码逐条调用 Open Food Facts
- 归一化为本项目种子 CSV
- 输出预览文件，供人工复核后再进入后端导入流程

### 标准步骤

1. 读取输入 CSV
2. 过滤 `enabled=false` 行
3. 校验条码只包含数字
4. 调用 Open Food Facts 产品读取接口
5. 映射到本项目字段
6. 生成 `search_keywords`
7. 跳过缺失关键字段的数据
8. 输出统一 CSV

## 最小校验规则

- 输入必须包含脚本要求的表头
- `enabled=true` 的条码必须为纯数字
- 外部结果至少需要可解析 `name` 与 `barcode`
- `category_hint` 为空时，可回落到 `零食`
- 输出前必须去除重复条码

## 当前脚本骨架职责

`tools/seed/import-openfoodfacts-barcodes.ps1` 当前负责：

- 读取条码模板
- 校验输入表头和条码格式
- 支持 `-DryRun`
- 支持按条码调用 Open Food Facts
- 生成符合本项目字段的输出 CSV

当前不负责：

- 直接写入 PostgreSQL
- 处理 Open Food Facts 全量数据集
- 处理图片下载和缩略图生成
- 处理复杂分类映射词典

## 推荐执行方式

### 1. Dry Run

```powershell
powershell -ExecutionPolicy Bypass -File tools/seed/import-openfoodfacts-barcodes.ps1 -DryRun
```

### 2. Live Import

```powershell
powershell -ExecutionPolicy Bypass -File tools/seed/import-openfoodfacts-barcodes.ps1 `
  -InputCsv data/seed/openfoodfacts-barcodes-template.csv `
  -OutputCsv data/seed/openfoodfacts-import-preview.csv `
  -UserAgent "snk-phase0-import/0.1 (contact: replace-me)"
```

## 与后续 Phase 1 的衔接

- 当前脚本输出的是可审阅 CSV，不直接碰数据库
- Phase 1 后端初始化完成后，可在服务端仓库中新增真正的数据库导入器
- 到那时，这份 CSV 仍可作为 Flyway / 批量初始化 / 管理后台导入的稳定输入格式

## Phase 0 Step 5 验收标准

- 仓库内已有可执行的导入脚本骨架
- 已有输入模板，便于继续扩充真实条码
- 已明确脚本输入、输出、校验和目录约定
- 已明确当前脚本不直接写库，只负责预处理和标准化

## 变更记录维护规则

- 每次修改本文件时，必须在下方追加一条记录

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 新建种子数据导入脚本方案文档 | 作为 Phase 0 Step 5 交付物，固定导入脚本输入输出、目录约定和执行流程 |
