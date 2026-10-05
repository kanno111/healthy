# 智约医疗后端

基于 Java 21、Spring Boot 4.1、Spring Cloud、Spring Cloud Alibaba、MyBatis-Plus、MySQL、Redis 与 RabbitMQ 的预约挂号系统。

这是一个 Maven 多模块工程：

- `healthy-common`：错误码、统一响应和公共异常。
- `healthy-pojo`：DTO、实体（Entity）和 VO。
- `healthy-server`：当前业务服务，内部已按 identity、provider、booking 边界解耦。
- `healthy-gateway`：统一入口，使用 Nacos 服务发现、Spring Cloud Gateway 路由和 Sentinel 限流能力。

## 本地启动前准备

1. 安装 JDK 21 与 Maven 3.9+。
2. 创建数据库：`CREATE DATABASE healthy DEFAULT CHARACTER SET utf8mb4;`
3. 启动本机已有的 MySQL（3306）、Redis（6379）和 RabbitMQ（5672）。这三个基础设施不使用 Docker。
4. 在项目根目录执行 `docker compose -f compose.microservices.yaml up -d nacos`，仅用 Docker 启动新引入的 Nacos（8848，控制台映射到 8849）。
5. 在本目录执行 `mvn clean package`。
6. 在两个终端分别运行：
   - `java -jar healthy-server/target/healthy-server-0.0.1-SNAPSHOT.jar`
   - `java -jar healthy-gateway/target/healthy-gateway-0.0.1-SNAPSHOT.jar`

Gateway 默认监听 `http://localhost:8080`，业务服务默认监听 `http://localhost:8081`。前端及 Nginx 继续访问 `GET http://localhost:8080/api/health`，请求由 Gateway 通过 Nacos 转发。

## 配置环境变量

- `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
- `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`
- `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`
- `NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`
- `SENTINEL_DASHBOARD`

密码不提交到仓库；本地可在 IDE 运行配置中设置上述变量。

## 当前目录职责

- `healthy-server/controller`：HTTP 接口层
- `healthy-server/service`：业务编排与事务边界
- `healthy-server/domain`：跨领域端口、当前本地适配器与批量视图装配
- `healthy-server/mapper`：MyBatis 数据访问接口与 XML
- `healthy-gateway`：统一路由、服务发现、限流接入和 Trace ID 透传
- `healthy-pojo`：实体、DTO 和 VO
- `healthy-common`：统一响应、异常与错误码

详细的边界、调用关系与后续抽取顺序见 [微服务演进说明](docs/microservice-evolution.md)。
