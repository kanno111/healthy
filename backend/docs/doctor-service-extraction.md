# doctor-service 拆分清单

## 1. 命名与职责

- 业务名称：`doctor-service`
- Maven 模块：`healthy-doctor`
- Nacos 服务名：`healthy-doctor`
- 计划端口：`8082`
- 拥有的数据：`department`、`doctor`
- 不拥有的数据：排班、号源、预约、候补、患者、账号和会话

医生账号绑定只在 doctor-service 保存 `userId`。账号本身及登录认证仍属于未来的 identity-service，不建立跨服务数据库外键。

## 2. 本次拆分不做什么

第一轮只拆医生和科室，不同时进行以下工作：

- 不拆 identity-service。
- 不迁移 JWT 和 Redis 会话。
- 不拆 MySQL 实例。
- 不引入分布式事务。
- 不移动排班和库存。
- 不修改 Outbox、RabbitMQ 候补超时消费和最终补偿。

第一轮允许 doctor-service 和 healthy-server 连接同一个本机 MySQL。当前 `healthy-server` 已删除医生、科室的 Controller、Service 与 Mapper；booking 只能通过 Feign 访问所需主数据。代码边界已经完成，下一阶段再拆独立 Schema 与 Flyway。

## 3. 当前迁移结果

### HTTP 接口

- 管理端 `/api/admin/doctors/**`、`/api/admin/departments/**` 已迁移到 `healthy-doctor`，由 Gateway 直接路由。
- 患者端 `/api/user/departments` 已迁移到 `healthy-doctor`，仅返回启用科室的公开字段。
- 患者端 `/api/user/doctors/**` 继续留在 booking：它需要把医生资料与排班可用性组合成患者视图，但医生基础资料已经通过 `DoctorDirectory` 远程读取。

`PatientResourceController` 中的 `/doctors/{id}/schedule-slots` 始终属于 booking。

### 业务服务

- `DoctorQueryService`、`DoctorCommandService`、`DepartmentQueryService`、`DepartmentCommandService` 已由 `healthy-doctor` 承担。
- `healthy-server` 中重复的医生/科室 Service 与过渡期科室命令代理已删除。
- `PatientResourceService` 保留患者视图和号源可用性聚合，通过 Feign 医生目录取得主数据。

### 数据访问

- `healthy-doctor` 使用自己的医生/科室 Query Mapper 与 Command Mapper。
- `healthy-server` 中的医生/科室 Mapper、XML 和 `LocalDoctorDirectory` 已删除。
- DTO、实体和 VO 暂时位于共享模块，等数据库 Schema 拆分时再继续收窄共享模型。

## 4. 继续留在 booking 的代码

- `DoctorScheduleSlotMapper` 及排班服务
- `AppointmentMapper` 及预约服务
- `AppointmentWaitlistMapper` 及候补服务
- Redis 号源预扣和库存校准
- Outbox Relay
- RabbitMQ 生产、消费、重试和故障队列
- 候补超时最终扫描补偿

booking 只保存和传递 `doctorId`，通过 doctor-service 批量取得医生及科室展示信息。

## 5. booking 需要的内部接口

doctor-service 需要提供以下内部能力，路径在落地前通过契约测试固定：

```text
GET  /api/internal/doctors/{doctorId}
GET  /api/internal/doctors/visible
POST /api/internal/doctors/batch
GET  /api/internal/doctors/by-user/{userId}
GET  /api/internal/departments/{departmentId}/doctor-ids
```

其中批量查询接口一次接收多个 `doctorId`，用于预约、排班和候补列表装配，禁止在列表循环中逐条发起 Feign 请求。

医生与科室写操作现在由 doctor-service 的公开管理接口直接调用本地 Command Service，并在本地事务中完成。迁移期的 `/api/internal/doctors`、`/api/internal/departments` 写接口已经删除，避免留下绕过 Gateway 角色校验的重复写入口。

## 6. 当前依赖点

以下 booking 代码会通过 `DoctorDirectory` 读取医生数据：

- `BookingViewAssembler`：批量装配医生和科室名称
- `ScheduleSlotServiceImpl`：校验医生、展示排班
- `DoctorAppointmentServiceImpl`：由登录用户定位医生身份
- `AdminAppointmentWaitlistServiceImpl`：按科室解析医生 ID
- `PatientResourceServiceImpl`：远程分页读取医生资料，再与 booking 的可用排班聚合

当前 booking 侧只保留以下端口与远程适配器：

```text
DoctorDirectory
FeignDoctorDirectory
domain.doctor
```

`healthy-server` 不再直接执行医生 SQL。

## 7. 外部接口迁移策略

采用渐进式迁移，避免一次移动认证、Controller 和数据库：

1. doctor-service 提供内部查询与命令接口。（已完成）
2. booking 查询链路切换到 Feign `DoctorDirectory`。（已完成）
3. Gateway 校验 JWT 与 Redis 会话，并传递可信身份上下文。（已完成）
4. doctor-service 暴露管理端医生接口，Gateway 直接路由。（已完成）
5. 删除 healthy-server 中重复的医生 Controller、Service、Mapper 和本地适配器。（已完成）
6. 迁移管理端科室与患者端科室公开接口，删除 healthy-server 的科室访问代码。（已完成）
7. 后续拆分数据库 Schema 与 Flyway。

## 8. Gateway 路由边界

当前直接路由到 doctor-service 的接口：

```text
/api/admin/doctors
/api/admin/doctors/**
/api/admin/departments
/api/admin/departments/**
/api/user/departments
```

以下接口仍路由到 `healthy-server`：

```text
/api/user/doctors
/api/user/doctors/{id}
/api/user/doctors/{id}/schedule-slots
```

患者医生接口需要聚合 booking 拥有的排班可用性，所以暂不直接路由到 doctor-service。Gateway 为 doctor-service 公共路由设置更高优先级，再由 `/api/**` 兜底到 `healthy-server`。

### Gateway 身份上下文契约

Gateway 在转发受保护请求前校验 JWT 签名及 Redis 会话，删除客户端提交的
`X-Auth-User-Id`、`X-Auth-Role`，再写入验证后的同名请求头。下游服务只信任
Gateway 所在内部网络传递的这两个头；生产部署只对公网暴露 Gateway。

`healthy-doctor` 已将这两个身份头转换为请求属性，并对公开的 `/admin/**`
接口统一要求 `STAFF` 角色、对 `/user/**` 接口要求 `PATIENT` 角色。现有只读
`/internal/**` 接口不经过该拦截器，当前服务间调用链保持不变。

Gateway 与发放 Token 的服务必须使用相同的 `JWT_SECRET` 和 Redis database，
否则 Gateway 会将有效登录会话误判为未登录。

## 9. 逐步提交顺序

每完成一步就停止，不连续实现下一步：

1. `docs: define doctor service extraction scope`（已完成）
2. `refactor: rename provider boundary to doctor directory`（已完成）
3. `feat: scaffold doctor service`（已完成）
4. `feat: add doctor service internal gateway route`（已完成）
5. `feat: move department queries to doctor service`（已完成）
6. `feat: move doctor queries to doctor service`（已完成）
7. `feat: add Feign doctor directory adapter`（已完成）
8. `refactor: query doctor data through Feign`（已完成）
9. `feat: configure doctor client timeouts`
10. `feat: protect doctor calls with Sentinel`
11. `feat: move department commands to doctor service`
    - [x] doctor-service 提供内部科室命令接口与测试
    - [x] healthy-server 通过 Feign 转发现有管理端科室命令
12. `feat: move doctor commands to doctor service`
    - [x] doctor-service 提供内部医生命令接口与测试
13. `refactor: share JWT verification support`（已完成）
14. `feat: authenticate gateway requests`（已完成）
15. `feat: protect doctor APIs with gateway identity`（已完成）
16. `feat: expose doctor admin API`（已完成）
17. `feat: route doctor API through gateway`（已完成）
18. `refactor: remove local doctor persistence access`（已完成）
19. `feat: expose department APIs from doctor service`（已完成）
20. `refactor: remove local department persistence access`（已完成）
21. `refactor: isolate doctor database schema`
22. `refactor: separate doctor Flyway migrations`

## 10. 每一步的验收规则

- 单次提交只包含一个目标。
- 修改前先说明调用链是否变化。
- 修改后列出必须审核的文件和机械修改文件。
- 执行相关单元测试；涉及启动配置时再执行真实注册和路由验证。
- 未通过当前步骤验收前，不开始下一步骤。
