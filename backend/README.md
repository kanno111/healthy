# 智约医疗后端

基于 Java 21、Spring Boot、MyBatis、MySQL 与 Redis 的预约挂号服务。

这是一个 Maven 多模块工程：

- `healthy-common`：错误码、统一响应和公共异常。
- `healthy-pojo`：DTO、实体（Entity）和 VO。
- `healthy-server`：可启动的 Spring Boot 服务，包含控制器、服务、Mapper 和配置。

## 本地启动前准备

1. 安装 JDK 21 与 Maven 3.9+。
2. 创建数据库：`CREATE DATABASE healthy DEFAULT CHARACTER SET utf8mb4;`
3. 启动本地 MySQL（3306）和 Redis（6379），或通过环境变量覆盖连接配置。
4. 首次运行时，在本目录安装模块依赖：`mvn -pl healthy-server -am install -DskipTests`。
5. 启动服务：`mvn -f healthy-server/pom.xml spring-boot:run`。

服务默认监听 `http://localhost:8080`；健康检查为 `GET /api/health`。

## 配置环境变量

- `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
- `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`

密码不提交到仓库；本地可在 IDE 运行配置中设置上述变量。

## 当前目录职责

- `healthy-server/controller`：HTTP 接口层
- `healthy-server/service`：业务编排与事务边界
- `healthy-server/mapper`：MyBatis 数据访问接口与 XML
- `healthy-pojo`：实体、DTO 和 VO
- `healthy-common`：统一响应、异常与错误码

下一步先建立预约领域的表结构和迁移脚本，再实现认证、号源和预约接口。
