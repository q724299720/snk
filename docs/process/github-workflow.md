# GitHub 仓库与提交流程

## 文档职责

本文件记录：

- GitHub 仓库创建步骤
- 当前项目的远程连接信息
- 日常提交与推送流程
- 隐私信息处理规则

在任何需要提交、推送、改远程地址、改分支策略的操作前，必须先阅读本文件。

## 隐私与安全规则

- 不在仓库中保存 GitHub 密码、PAT、SSH 私钥、邮箱验证码等敏感信息
- 远程连接信息只记录无敏感字段，如仓库 URL、仓库名、默认分支、认证方式
- 所有本地敏感文件必须被 `.gitignore` 排除
- 若误提交敏感信息，必须立即撤销、改写历史并更换对应凭据

## 当前连接信息

以下字段允许记录：

| 字段 | 当前值 |
| --- | --- |
| 本地仓库路径 | `E:\programdata\Seafile\document\Seafile\document\AI\snk` |
| GitHub 仓库所有者 | `q724299720` |
| GitHub 仓库名称 | `snk` |
| 远程仓库 URL | `https://github.com/q724299720/snk.git` |
| 默认分支 | `main` |
| 认证方式 | `HTTPS（当前已配置，不记录密码）` |
| 本地 Git 仓库状态 | `已初始化，已首次提交并已推送到 origin/main` |
| Git 提交身份 | `已配置：q724299720 / 724299720@qq.com` |

禁止记录的内容：

- GitHub 登录密码
- Personal Access Token 明文
- SSH 私钥内容
- 任何一次性验证码

## 推荐创建方式

优先使用 SSH 连接 GitHub，原因：

- 不需要在文档中保留任何密码
- 推送体验更稳定
- 更适合长期开发

## GitHub 新仓库创建步骤

### 方案 A：网页创建

1. 打开 GitHub，新建仓库
2. 仓库名填写：`snk`
3. 建议选择：`Private`
4. 不要勾选自动生成 `README`、`.gitignore`、License
5. 创建完成后，复制仓库 URL

### 方案 B：后续补 CLI

当前机器未安装 `gh` CLI，因此当前流程以网页创建为准。若后续安装 `gh`，可再补充自动化创建流程。

## 本地连接远程仓库步骤

在仓库创建完成后执行：

```powershell
git remote add origin git@github.com:<owner>/snk.git
git remote -v
```

如果你选择 HTTPS：

```powershell
git remote add origin https://github.com/<owner>/snk.git
git remote -v
```

注意：

- 只记录仓库 URL，不记录 HTTPS 密码或 Token
- 若后续从 HTTPS 切换到 SSH，需要同步更新本文件中的远程 URL 和认证方式

当前项目已配置：

```powershell
git remote add origin https://github.com/q724299720/snk.git
```

## 首次提交前的本机配置

当前机器还没有配置 Git 提交身份，首次提交前需要先执行：

```powershell
git config --global user.name "你的 GitHub 显示名"
git config --global user.email "你的 GitHub 邮箱"
```

检查是否配置成功：

```powershell
git config --get user.name
git config --get user.email
```

## 首次推送步骤

```powershell
git branch -M main
git add .
git commit -m "chore: initialize project docs and workflow"
git push -u origin main
```

## 日常提交流程

每次修改完成后，必须执行：

```powershell
git status
git add .
git commit -m "<type>: <summary>"
git push
```

建议的提交前检查：

1. `agents.md` 和相关子文档是否已同步
2. 文档“变更记录”是否已追加
3. 是否误包含 `.env`、密钥、证书、缓存文件
4. 是否完成本次增量测试

## 建议提交信息规范

- `feat: ...` 新功能
- `fix: ...` 缺陷修复
- `docs: ...` 文档调整
- `refactor: ...` 重构
- `chore: ...` 基础设施或杂项
- `test: ...` 测试相关

## 后续执行约束

- 任何项目修改完成后，不允许只保留本地未提交状态
- 若当次工作无法推送远程，必须明确记录阻塞原因
- 在远程连接信息补全后，默认要求每次修改结束即推送到 GitHub

## 变更记录维护规则

- 每次修改本文件时，必须在下方追加一条记录

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-06-13 | Codex | 新建 GitHub 仓库创建、连接记录与提交流程文档 | 落实“后续每次修改提交到远程 GitHub”要求，并避免记录敏感信息 |
| 2026-06-13 | Codex | 补充本机 Git 身份配置与当前连接状态 | 当前环境未配置提交身份，需先完成后才能首次提交 |
| 2026-06-13 | Codex | 回填 GitHub 仓库所有者、远程 URL 与当前连接状态 | 用户已提供远程仓库地址，需要纳入可追溯连接记录 |
| 2026-06-13 | Codex | 更新 Git 提交身份状态 | 用户已提供显示名和邮箱，已完成本机 Git 身份配置 |
| 2026-06-13 | Codex | 更新本地 Git 仓库状态为已首次推送 | 首次提交与推送已完成，需要使连接记录与真实状态一致 |
