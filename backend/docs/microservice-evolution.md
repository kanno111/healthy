# 微服务演进说明

## 当前阶段

第一阶段的服务边界已经落地：

| 进程 | 领域职责 | 数据库 | 主要依赖 |
|---|---|---|---|
| `healthy-gateway` | 统一入口、JWT + Redis 会话校验、路由 | 无 | Redis、Nacos |
| `healthy-identity` | 账号、患者、注册登录、Token 生命周期 | `healthy_identity` | Redis、Nacos |
| `healthy-doctor` | 科室、医生主数据 | `healthy_doctor` | Nacos |
| `healthy-server`（Booking） | 排班号源、预约、候补、Outbox | `healthy` | Redis、RabbitMQ、Nacos |

三个业务服务各自拥有数据库、Flyway、实体和 Mapper。跨服务只传逻辑 ID，不建立跨库外键，
不做跨库 JOIN。

## 为什么这样拆

服务边界按业务能力而不是按患者端、医生端、管理端划分。三套前端角色都访问 Gateway，
一个页面可能同时使用多个领域能力。例如管理端候补列表由 Booking 查询订单，再批量调用
Identity 和 Doctor 装配患者及医生展示字段。

排班仍归 Booking，因为扣减号源、创建预约和候补流转需要保持在同一个强一致本地事务中。
如果把排班库存拆到 Doctor，核心写链路会被迫引入分布式事务，复杂度大于收益。

## 调用关系

```text
Frontend
   -> Gateway
      -> Identity（/api/auth/**）
      -> Doctor（医生/科室直接 API）
      -> Booking（预约、候补、排班、聚合查询）

Booking -> PatientDirectory -> Feign -> Identity
Booking -> DoctorDirectory  -> Feign -> Doctor
```

`PatientDirectory` 与 `DoctorDirectory` 是 Booking 内部的领域端口。Feign 类只是基础设施适配器，
所以 Booking 的业务 Service 不感知 Nacos、HTTP 或对方数据库结构。

批量患者/医生接口限制单批最多 100 个 ID，预约列表装配时不会逐行发远程请求。

## 认证模型

Identity 校验密码并签发 JWT，同时写入 Redis session；Gateway 对后续请求校验 JWT 和 session，
清除客户端伪造身份头后写入可信的 `X-Auth-User-Id`、`X-Auth-Role`。Booking 与 Doctor 读取
这些请求头并进行角色授权，不再重复持有 JWT 密钥或访问会话。

内部 Feign API 不经 Gateway 暴露，当前依赖部署网络隔离。下一阶段可增加服务间凭证或 mTLS，
但这不影响现有领域边界。

## 数据一致性

- Identity 注册：账号和患者档案在同一数据库事务中提交。
- Booking 挂号：Redis Lua 预扣、MySQL 条件更新、唯一索引及库存校准保持不变。
- Booking 候补：数据库 FIFO、状态机、RabbitMQ TTL/DLX、消费者重试及定时兜底保持不变。
- Outbox 只解决 Booking 本地事务与 RabbitMQ 发布之间的可靠性，不承担跨库 JOIN。
- 跨服务资料读取接受短暂不可用；主业务写入失败时不会绕过服务边界读取旧表。

## 数据迁移

- Doctor：复制并校验后，由 Booking Flyway V12 删除旧 `doctor`、`department`。
- Identity：复制并校验后，由 Booking Flyway V13 删除旧 `sys_user`、`patient`。
- Booking 的 `doctor_id`、`patient_id` 都保持原 ID，现有预约数据无需重写。

详见 [Doctor 拆分](doctor-service-extraction.md) 与
[Identity 拆分](identity-service-extraction.md)。

## 下一阶段建议

第一阶段至此可以视为完成。后续优先级建议是：

1. 将模块名和 Nacos 服务名 `healthy-server` 正式改为 `healthy-booking`。
2. 给内部接口增加服务身份认证，并限制 8081～8083 仅内网可达。
3. 给 Feign 调用增加明确的超时、指标、追踪与有限降级策略。
4. 增加 Testcontainers 或独立测试库，避免集成测试依赖开发数据库。
5. 做一套可复现的故障演练：Identity/Doctor 宕机、RabbitMQ 宕机、Outbox 堆积与恢复。

不建议为了展示技术栈继续强拆服务或强行引入 Seata。当前强一致聚合留在本地事务中，服务
边界清楚且能解释取舍，比服务数量更多更有说服力。
