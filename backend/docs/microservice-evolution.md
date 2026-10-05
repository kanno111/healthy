# 微服务演进说明

## 当前落地范围

本次完成的是阶段二至阶段五：先验证微服务组件兼容性，再整理领域边界、清理跨领域 JOIN，最后加入 Nacos 与 Gateway。当前仍是一个可运行的业务单体，不会为了数量提前复制三个难以维护的服务进程。

版本基线：

- Java 21
- Spring Boot 4.1.1
- Spring Cloud 2025.1.3
- Spring Cloud Alibaba 2025.1.0.0
- Nacos Client 3.1.1

Spring Cloud OpenFeign、Nacos Discovery/Config 与 Sentinel 已接入 `healthy-server`，Gateway、LoadBalancer、Nacos 与 Sentinel 已接入 `healthy-gateway`。

## 目标边界

### identity-service

负责账号、患者资料、登录认证和会话。拥有 `sys_user`、`patient` 数据。

### doctor-service

负责科室、医生基础资料和医生账号绑定。拥有 `department`、`doctor` 数据。

### booking-service

负责排班号源、预约、候补、库存、Outbox、RabbitMQ 消费与最终补偿。拥有 `doctor_schedule_slot`、`appointment`、`appointment_waitlist`、`message_outbox` 数据。

排班归 booking，而不是 doctor-service。因为扣减号源、创建预约、候补流转都要求和排班保持强一致；这样拆分后核心写链路仍可使用本地事务，不必为了展示微服务而引入 Seata。

## 当前代码如何映射到未来服务

`PatientDirectory` 是 booking 访问 identity 数据的端口，当前由 `LocalPatientDirectory` 通过本地 Mapper 实现。未来抽出 identity-service 时，只需增加基于 OpenFeign 的远程适配器，并替换 Spring Bean。

`DoctorDirectory` 是 booking 访问医生和科室数据的端口，当前由 `LocalDoctorDirectory` 实现。未来抽出 doctor-service 时，只需增加 Feign 适配器并替换 Spring Bean。

`BookingViewAssembler` 根据 booking 查询返回的 `patientId`、`doctorId` 做批量查询并装配名称，避免逐条远程调用造成 N+1。预约和候补 Mapper 不再 JOIN identity/doctor-service 表；`MapperDomainBoundaryTest` 会防止跨域 JOIN 被重新引入。

三套前端与三个微服务不是一一对应关系。患者端、医生端和管理端都只访问 Gateway，Gateway 再按 URL 路由到领域服务。例如管理端的一张候补列表可以同时涉及 booking、identity 和 doctor-service，由 booking 的查询编排层批量聚合。

## 当前请求链路

```text
Vue / Nginx
    -> healthy-gateway:8080
       - /api/**
       - Trace ID
       - Sentinel
       - Nacos LoadBalancer
    -> healthy-server:8081
       - identity / doctor / booking 本地适配器
       - MySQL / Redis / RabbitMQ
```

`healthy-server` 和 `healthy-gateway` 都注册到 Nacos。Gateway 的 `/api/**` 路由使用 `lb://healthy-server`，因此前端开发代理与现有 Nginx 仍然访问 8080，无需修改接口地址。

本地开发采用混合方式：已经安装的 MySQL、Redis、RabbitMQ 继续运行在宿主机；新引入的 Nacos 使用项目根目录的 `compose.microservices.yaml` 单独启动。该 Compose 不定义前三项服务，因此不会误启动另一套数据库或消息队列。

## 下一次真正抽服务的顺序

1. 先抽 doctor-service（模块名与 Nacos 服务名为 `healthy-doctor`）。它以查询和基础资料 CRUD 为主，对预约写事务影响最小。
2. 将当前 `LocalDoctorDirectory` 替换为 Feign 适配器，保持批量接口，不允许循环远程调用。
3. 再抽 identity-service；认证可先留在原服务，稳定后再把登录与 Redis 会话迁走。
4. 最后把剩余项目重命名为 booking-service。Outbox、RabbitMQ 和候补补偿整体保留在 booking 内部。
5. 服务各自拥有数据库 schema 和 Flyway 迁移。跨服务只保存 ID，不建立数据库外键，也不跨库 JOIN。

这条路线不要求分布式事务：强一致的预约聚合留在 booking 本地事务内；跨服务资料查询接受短暂不一致，必要时使用缓存或事件驱动读模型。Outbox 继续解决 booking 本地事务提交与 RabbitMQ 发布之间的可靠性问题。
