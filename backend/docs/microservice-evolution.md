# 微服务演进说明

## 当前落地范围

项目已经从单体演进为 Gateway、identity/booking 服务和 doctor-service 三个进程。医生、科室代码及数据已切换到 doctor-service；identity 与 booking 暂时仍在 `healthy-server`，以避免一次拆分过多核心事务。

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

`DoctorDirectory` 是 booking 访问医生和科室数据的端口，当前由 `FeignDoctorDirectory` 实现，通过 Nacos 服务发现调用 doctor-service。`healthy-server` 已删除本地医生/科室 Controller、Service、Mapper 和本地回退实现，避免两个服务同时拥有主数据访问逻辑。

`BookingViewAssembler` 根据 booking 查询返回的 `patientId`、`doctorId` 做批量查询并装配名称，避免逐条远程调用造成 N+1。预约和候补 Mapper 不再 JOIN identity/doctor-service 表；`MapperDomainBoundaryTest` 会防止跨域 JOIN 被重新引入。

三套前端与三个微服务不是一一对应关系。患者端、医生端和管理端都只访问 Gateway，Gateway 再按 URL 路由到领域服务。例如管理端的一张候补列表可以同时涉及 booking、identity 和 doctor-service，由 booking 的查询编排层批量聚合。

## 当前请求链路

```text
Vue / Nginx
    -> healthy-gateway:8080
       - /api/admin/doctors/** -> healthy-doctor
       - /api/admin/departments/** -> healthy-doctor
       - /api/user/departments -> healthy-doctor
       - 其余 /api/** -> healthy-server
       - /doctor-service/actuator/health（临时内部验通路由）
       - Trace ID
       - Sentinel
       - Nacos LoadBalancer
    -> healthy-server:8081
       - identity / booking
       - DoctorDirectory 远程适配器
       - healthy 数据库 / Redis / RabbitMQ

    healthy-doctor:8082
       - 已注册 Nacos
       - 提供 booking 所需的科室、医生内部查询接口
       - 提供受 Gateway 身份上下文保护的医生/科室管理 API 与患者科室 API
       - 拥有医生、科室查询和写入的数据访问逻辑
       - 独占 healthy_doctor 数据库与 Flyway
```

`healthy-server`、`healthy-gateway` 和 `healthy-doctor` 都注册到 Nacos。Gateway 优先把医生管理、科室管理和患者科室查询路由到 `lb://healthy-doctor`，再用 `/api/**` 兜底路由到 `lb://healthy-server`。Gateway 校验 JWT 与 Redis 会话，并向 doctor-service 注入可信用户 ID 和角色。

本地开发采用混合方式：已经安装的 MySQL、Redis、RabbitMQ 继续运行在宿主机；新引入的 Nacos 使用项目根目录的 `compose.microservices.yaml` 单独启动。该 Compose 不定义前三项服务，因此不会误启动另一套数据库或消息队列。

## 下一次真正抽服务的顺序

1. doctor-service 已完成代码和数据库抽取，医生/科室公共 API 已由 Gateway 直接路由，并独占 `healthy_doctor`。
2. booking 已固定使用 Feign 医生目录，并保留批量接口，避免循环远程调用。
3. 下一步再抽 identity-service；认证可先留在原服务，稳定后再把登录与 Redis 会话迁走。
4. 最后把剩余项目重命名为 booking-service。Outbox、RabbitMQ 和候补补偿整体保留在 booking 内部。
5. 服务各自拥有数据库 schema 和 Flyway 迁移。（doctor-service 已完成；identity 与 booking 待后续拆分。）跨服务只保存 ID，不建立数据库外键，也不跨库 JOIN。

这条路线不要求分布式事务：强一致的预约聚合留在 booking 本地事务内；跨服务资料查询接受短暂不一致，必要时使用缓存或事件驱动读模型。Outbox 继续解决 booking 本地事务提交与 RabbitMQ 发布之间的可靠性问题。
