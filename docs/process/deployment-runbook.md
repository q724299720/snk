# 生产部署清单

## 上传物

只上传构建产物：`server/build/libs/server-0.0.1-SNAPSHOT.jar`。该 JAR 已包含服务端代码、后台静态页和 Flyway 数据库迁移；不要上传源码、`.env`、密钥或 Android 构建缓存。

## 宝塔环境变量

在 Java 项目环境变量中配置非空的 `SNK_DB_HOST`、`SNK_DB_NAME`、`SNK_DB_USERNAME`、`SNK_DB_PASSWORD`、`SNK_JWT_PRIVATE_KEY`、`SNK_JWT_PUBLIC_KEY`、`SNK_TOKEN_ENCRYPTION_KEY`、`SNK_OWNER_USERNAME`、`SNK_OWNER_PASSWORD`、`SNK_ADMIN_API_TOKEN` 和 `SNK_STORAGE_ROOT`。生产保持 `SNK_AUTH_ENFORCE_SECURITY=true`；`SNK_OWNER_FORCE_RESET` 默认 `false`，仅应急时短暂开启。

## 发布顺序

1. 备份 PostgreSQL 与上传目录。
2. 上传新 JAR，保留上一版 JAR 作为回滚文件。
3. 在宝塔重启 Java 项目；Flyway 会自动执行未应用迁移。
4. 检查 `/actuator/health` 返回 200；用 OWNER 登录后台，确认账户接口同时需要 Admin Token 与 Bearer Token。
5. 若迁移或健康检查失败，停止新进程、恢复上一版 JAR 与数据库备份；不得在未确认备份前回滚数据库。

## 变更记录

| 日期 | 修改人 | 变更范围 | 原因 |
| --- | --- | --- | --- |
| 2026-07-12 | Codex | 新建认证版本生产部署清单 | 明确唯一上传物、密钥边界、备份和回滚流程 |
