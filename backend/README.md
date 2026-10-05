# 智慧医疗预约后端

项目基于 Java 21、Spring Boot 4.1、Spring Cloud、Spring Cloud Alibaba、
MyBatis-Plus、MySQL、Redis 与 RabbitMQ。当前已经从单体演进为四个独立进程：

- `healthy-gateway`：统一入口，校验 JWT + Redis 会话，注入可信用户 ID/角色并路由请求。
- `healthy-identity`：账号、患者档案、注册登录、JWT 签发与 Redis 会话，独占 `healthy_identity`。
- `healthy-doctor`：科室和医生主数据，独占 `healthy_doctor`。
- `healthy-server`：booking-service，负责排班号源、预约、候补、Outbox 和 RabbitMQ，独占 `healthy`。

`healthy-common` 只放跨服务响应、错误码和安全协议；`healthy-pojo` 只保留
booking-service 使用的模型，不再承载 Identity 或 Doctor 的实体。

## 请求链路

前端 API 地址不需要修改，始终访问 Gateway 的 `/api/**`：

```text
Vue / Nginx
    -> healthy-gateway:8080
       -> /api/auth/**                         -> healthy-identity:8083
       -> /api/admin/doctors/**                -> healthy-doctor:8082
       -> /api/admin/departments/**            -> healthy-doctor:8082
       -> /api/user/departments                -> healthy-doctor:8082
       -> 其余 /api/**                         -> healthy-server:8081

healthy-server
    -> Feign + Nacos -> healthy-identity（患者目录）
    -> Feign + Nacos -> healthy-doctor（医生目录）
```

Gateway 对登录、注册放行；其他公开路由先验证 JWT 签名及 Redis 会话，再覆盖客户端
伪造的身份请求头。下游服务不再重复解析 JWT。

## 本地启动

本地开发继续使用电脑上已经安装的 MySQL、Redis 和 RabbitMQ，不用 Docker 启动这三项。
只有 Nacos 使用项目根目录的 `compose.microservices.yaml`。

1. 确认 MySQL `3306`、Redis `6379`、RabbitMQ `5672` 已启动。
2. 准备三个数据库：`healthy`、`healthy_doctor`、`healthy_identity`。
3. 在项目根目录启动 Nacos：

   ```bash
   docker compose -f compose.microservices.yaml up -d nacos
   ```

4. 配置各服务数据库密码以及统一的 `JWT_SECRET`、Redis database。
5. 在本目录构建：

   ```bash
   mvn clean package
   ```

6. 依次启动：

   ```bash
   java -jar healthy-identity/target/healthy-identity-0.0.1-SNAPSHOT.jar
   java -jar healthy-doctor/target/healthy-doctor-0.0.1-SNAPSHOT.jar
   java -jar healthy-server/target/healthy-server-0.0.1-SNAPSHOT.jar
   java -jar healthy-gateway/target/healthy-gateway-0.0.1-SNAPSHOT.jar
   ```

端口分别为 Gateway `8080`、Booking `8081`、Doctor `8082`、Identity `8083`。
Nacos 控制台为 `http://localhost:8849/nacos/`。

## 从共享数据库升级

数据库迁移必须遵循“先建目标表、再复制校验、最后删源表”：

- Doctor 拆分参见 [doctor-service-extraction.md](docs/doctor-service-extraction.md)。
- Identity 拆分参见 [identity-service-extraction.md](docs/identity-service-extraction.md)。

`healthy-server` 的 V12 会删除旧医生/科室表，V13 会删除旧账号/患者表。禁止在复制脚本
的 mismatch 和 orphan 校验全部为 0 之前执行相应删除迁移。

## 配置变量

- Booking：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
- Doctor：`DOCTOR_DB_HOST`、`DOCTOR_DB_PORT`、`DOCTOR_DB_NAME`、`DOCTOR_DB_USERNAME`、`DOCTOR_DB_PASSWORD`
- Identity：`IDENTITY_DB_HOST`、`IDENTITY_DB_PORT`、`IDENTITY_DB_NAME`、`IDENTITY_DB_USERNAME`、`IDENTITY_DB_PASSWORD`
- 公共基础设施：`REDIS_*`、`RABBITMQ_*`、`NACOS_*`
- 安全：`JWT_SECRET`。Identity 与 Gateway 必须使用完全相同的值。

密码与 JWT 密钥不要提交到仓库。开发机可使用被 Git 忽略的 `application-local.yml`
或 IDE 环境变量。

## Docker Compose 部署

根目录 `compose.yaml` 已包含 Gateway、三个业务服务、Nacos、MySQL、Redis、RabbitMQ
和前端。复制 `.env.example` 为 `.env` 并替换所有密码后运行：

```bash
docker compose up -d --build
```

MySQL 初始化脚本会为三个服务创建独立数据库和最小作用域账号。已有 MySQL volume 不会
重新执行初始化脚本，旧环境必须先按上述拆分文档完成数据迁移。

## 验证

```bash
mvn clean test
```

完整测试会验证应用上下文、Gateway 路由、Feign 适配器、权限拦截器、预约并发、
候补、Outbox 与数据库边界。
