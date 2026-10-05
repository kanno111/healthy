# 智约医疗后端

基于 Java 21、Spring Boot 4.1、Spring Cloud、Spring Cloud Alibaba、MyBatis-Plus、MySQL、Redis 与 RabbitMQ 的预约挂号系统。

这是一个 Maven 多模块工程：

- `healthy-common`：错误码、统一响应和公共异常。
- `healthy-pojo`：identity/booking 尚未拆出的实体、DTO 和 VO；不再承载 doctor-service 模型。
- `healthy-server`：identity 与 booking 业务服务，通过 Feign 读取医生主数据。
- `healthy-gateway`：统一入口，使用 Nacos 服务发现、Spring Cloud Gateway 路由和 Sentinel 限流能力。
- `healthy-doctor`：doctor-service；拥有医生、科室的数据库和 Java 模型，直接提供管理端医生/科室 API、患者端科室列表及 booking 所需的内部查询接口。

`healthy-server` 的 `DoctorDirectory` 固定通过 Feign 和 Nacos 调用 `healthy-doctor`，不再包含医生或科室本地 Mapper。管理端 `/api/admin/doctors/**`、`/api/admin/departments/**` 与患者端 `/api/user/departments` 均由 Gateway 直接路由至 `healthy-doctor`，前端 API 地址不变。

## 本地启动前准备

1. 安装 JDK 21 与 Maven 3.9+。
2. 创建两个独立数据库：
   - `CREATE DATABASE healthy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
   - `CREATE DATABASE healthy_doctor DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
3. 启动本机已有的 MySQL（3306）、Redis（6379）和 RabbitMQ（5672）。这三个基础设施不使用 Docker。
4. 在项目根目录执行 `docker compose -f compose.microservices.yaml up -d nacos`，仅用 Docker 启动新引入的 Nacos（8848，控制台映射到 8849）。
5. 在本目录执行 `mvn clean package`。
6. 首次启动先运行 `healthy-doctor`，让它在 `healthy_doctor` 中执行自己的 Flyway；再启动另外两个服务：
   - `java -jar healthy-doctor/target/healthy-doctor-0.0.1-SNAPSHOT.jar`
   - `java -jar healthy-server/target/healthy-server-0.0.1-SNAPSHOT.jar`
   - `java -jar healthy-gateway/target/healthy-gateway-0.0.1-SNAPSHOT.jar`

Gateway 默认监听 `http://localhost:8080`，identity/booking 服务监听 `http://localhost:8081`，doctor-service 监听 `http://localhost:8082`。前端及 Nginx 继续访问 `GET http://localhost:8080/api/health`；可通过 `GET http://localhost:8080/doctor-service/actuator/health` 验证 Gateway 对 doctor-service 的服务发现。

`healthy-server` 的 Flyway 只维护 `healthy`，`healthy-doctor` 的 Flyway 只维护 `healthy_doctor`。从旧的共享数据库升级时，按 [doctor-service 拆分清单](docs/doctor-service-extraction.md) 的数据库迁移章节执行，不能在数据复制完成前让 `healthy-server` 执行 V12。

## 配置环境变量

- `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
- `DOCTOR_DB_HOST`、`DOCTOR_DB_PORT`、`DOCTOR_DB_NAME`、`DOCTOR_DB_USERNAME`、`DOCTOR_DB_PASSWORD`
- `REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE`、`REDIS_PASSWORD`
- `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`
- `NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`
- `SENTINEL_DASHBOARD`
- `JWT_SECRET`（Gateway 必须与签发 Token 的 `healthy-server` 保持一致）

密码不提交到仓库；本地可在 IDE 运行配置中设置上述变量。

其中 `DB_*` 属于 identity/booking 数据库，`DOCTOR_DB_*` 属于 doctor-service 数据库。即使两个数据库暂时位于同一个 MySQL 实例，也应使用不同数据库名；生产环境建议再配置两个权限隔离的数据库账号。

## 当前目录职责

- `healthy-server/controller`：identity、booking 与患者聚合接口层
- `healthy-server/service`：预约业务编排与事务边界
- `healthy-server/domain`：跨领域端口、Feign 适配器与批量视图装配
- `healthy-server/mapper`：identity、booking 数据访问接口与 XML
- `healthy-gateway`：统一路由、服务发现、限流接入和 Trace ID 透传
- `healthy-doctor`：医生与科室主数据服务；独占 `healthy_doctor` 数据库、Flyway、实体、DTO 和 VO，医生/科室管理 API 和患者科室查询已直接对接 Gateway
- `healthy-pojo`：identity/booking 暂时共用的实体、DTO 和 VO；后续随服务拆分继续收窄
- `healthy-common`：统一响应、异常与错误码

详细的边界、调用关系与后续抽取顺序见 [微服务演进说明](docs/microservice-evolution.md)。
