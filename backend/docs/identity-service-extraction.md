# Identity Service 拆分说明

## 拆分结果

`healthy-identity` 是独立 Maven 模块和 Nacos 服务，默认端口 `8083`，独占
`healthy_identity` 数据库。

它负责：

- `sys_user` 账号与 `patient` 患者档案；
- 患者注册；
- 密码校验、JWT 签发；
- Redis 登录会话创建和注销；
- 为 Booking 提供患者内部批量查询接口。

它不负责 Gateway 路由，也不负责预约、排班或医生资料。

## 对外与内部接口

Gateway 将原有 URL 直接路由到 Identity，前端无需修改：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`

Booking 通过 Nacos + OpenFeign 调用以下内部接口：

- `GET /api/internal/patients/by-user/{userId}`
- `POST /api/internal/patients/batch`
- `GET /api/internal/patients/search?keyword=...`

批量接口避免预约列表装配时产生 N+1 远程调用。Booking 保留自己的
`PatientDirectory` 领域端口和消费方模型，不依赖 Identity 的实体或 DTO。

## 认证职责

```text
登录：Frontend -> Gateway（放行） -> Identity -> 签发 JWT + 写 Redis session

业务请求：Frontend -> Gateway
                    -> 校验 JWT 签名
                    -> 校验 Redis session
                    -> 覆盖并写入 X-Auth-User-Id / X-Auth-Role
                    -> Booking 或 Doctor

注销：Frontend -> Gateway（先认证） -> Identity -> 删除 Redis session
```

因此 Gateway 是统一认证执行点，Identity 是身份数据和凭证生命周期的拥有者；两者不是
重复关系。Identity 和 Gateway 必须使用同一个 `JWT_SECRET`、Redis 实例、Redis database
以及 `TokenSessionKey` 规则。

## 数据库迁移顺序

升级已有共享数据库时：

1. 停止 Gateway、Identity、Booking 的账号和患者写入。
2. 创建 `healthy_identity` 数据库。
3. 单独启动 `healthy-identity`，由 Flyway V1 创建 `sys_user`、`patient`。
4. 执行 `scripts/db/copy-identity-data.sql`。
5. 确认以下结果全部成立：
   - `sys_user_mismatch_count = 0`
   - `patient_mismatch_count = 0`
   - `patient_without_user_count = 0`
   - 源表和目标表行数一致
6. 启动 Identity 并验证登录、注册和患者内部查询。
7. 最后启动 Booking，让 Flyway V13 删除旧库里的 `patient`、`sys_user`。
8. 校验 `appointment.patient_id`、`appointment_waitlist.patient_id` 在 Identity 中没有孤儿 ID。

所有原 ID 都原样复制。Booking 数据库只保存逻辑 `patient_id`，不建立跨库外键，也不做
跨库 JOIN。

## 一致性边界

注册时创建 `sys_user` 与 `patient` 仍发生在 Identity 的同一个本地事务中，不需要分布式
事务。Booking 创建预约前同步确认患者有效；预约生成后保留患者 ID 和业务快照。资料查询
短暂不可用时返回服务异常，不会越权回退到旧表。

## 回滚思路

V13 执行前可直接回退应用并继续使用共享表。V13 执行后，`healthy_identity` 中的完整目标
数据就是恢复源；如必须回滚，需要先停止写入，再将目标表按原 ID 反向复制回 Booking
数据库，校验完成后再启动旧版本。不要让新旧两个服务同时写同一份身份主数据。
