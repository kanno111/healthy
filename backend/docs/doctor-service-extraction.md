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

第一轮允许 doctor-service 和 healthy-server 连接同一个本机 MySQL，但 healthy-server 切换到 Feign 后不得再直接访问 `doctor`、`department` Mapper。调用稳定后再拆独立 Schema。

## 3. 将迁移到 doctor-service 的代码

### HTTP 接口

- `controller/admin/DepartmentController`
- `controller/admin/DoctorController`
- `controller/user/PatientResourceController` 中的科室和医生查询接口

`PatientResourceController` 中的 `/doctors/{id}/schedule-slots` 不迁移，它属于 booking。

### 业务服务

- `DepartmentService`、`DepartmentServiceImpl`
- `DoctorService`、`DoctorServiceImpl`
- `PatientResourceService` 中的科室、医生查询能力需要拆开；排班查询继续留在 booking

### 数据访问

- `DepartmentMapper`、`DepartmentMapper.xml`
- `DoctorMapper`、`DoctorMapper.xml`
- `Department`、`Doctor` 领域实体
- 与科室、医生 CRUD 直接相关的 DTO 和 VO

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
POST /api/internal/doctors/batch
GET  /api/internal/doctors/by-user/{userId}
GET  /api/internal/departments/{departmentId}/doctor-ids
```

其中批量查询接口一次接收多个 `doctorId`，用于预约、排班和候补列表装配，禁止在列表循环中逐条发起 Feign 请求。

## 6. 当前依赖点

以下 booking 代码会通过当前 `ProviderDirectory` 读取医生数据：

- `BookingViewAssembler`：批量装配医生和科室名称
- `ScheduleSlotServiceImpl`：校验医生、展示排班
- `DoctorAppointmentServiceImpl`：由登录用户定位医生身份
- `AdminAppointmentWaitlistServiceImpl`：按科室解析医生 ID

当前命名将在单独的小提交中调整：

```text
ProviderDirectory      -> DoctorDirectory
LocalProviderDirectory -> LocalDoctorDirectory
domain.provider        -> domain.doctor
```

这次重命名只调整名称，不改变 SQL、业务逻辑或接口返回。

## 7. 外部接口迁移策略

采用渐进式迁移，避免一次移动认证、Controller 和数据库：

1. doctor-service 先提供内部只读接口。
2. healthy-server 增加 Feign `DoctorDirectory` 实现，可通过配置在 local/remote 间切换。
3. booking 查询链路切换到 remote，验证批量调用、超时和熔断。
4. 再迁移科室和医生写接口。
5. doctor-service 具备独立鉴权后，Gateway 才把外部医生、科室接口直接路由给它。
6. 最后删除 healthy-server 中的医生、科室 Mapper，并拆分数据库 Schema 与 Flyway。

## 8. Gateway 路由边界

未来可直接路由到 doctor-service 的接口：

```text
/api/admin/departments/**
/api/admin/doctors/**
/api/user/departments
/api/user/doctors
/api/user/doctors/{id}
```

以下接口仍路由到 booking，并且路由优先级必须更高：

```text
/api/user/doctors/{id}/schedule-slots
```

## 9. 逐步提交顺序

每完成一步就停止，不连续实现下一步：

1. `docs: define doctor service extraction scope`（当前步骤）
2. `refactor: rename provider boundary to doctor directory`
3. `feat: scaffold doctor service`
4. `feat: add doctor service internal gateway route`
5. `feat: move department queries to doctor service`
6. `feat: move doctor queries to doctor service`
7. `feat: add Feign doctor directory adapter`
8. `refactor: query doctor data through Feign`
9. `feat: configure doctor client timeouts`
10. `feat: protect doctor calls with Sentinel`
11. `feat: move department commands to doctor service`
12. `feat: move doctor commands to doctor service`
13. `refactor: remove local doctor persistence access`
14. `refactor: isolate doctor database schema`
15. `refactor: separate doctor Flyway migrations`

## 10. 每一步的验收规则

- 单次提交只包含一个目标。
- 修改前先说明调用链是否变化。
- 修改后列出必须审核的文件和机械修改文件。
- 执行相关单元测试；涉及启动配置时再执行真实注册和路由验证。
- 未通过当前步骤验收前，不开始下一步骤。
