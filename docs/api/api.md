# 智约医疗接口文档

## 1. 登录

### `POST /api/auth/login`

```json
{ "username": "patient_demo", "password": "123456" }
```

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `username` | string | 是 | 非空，最长 50 个字符 |
| `password` | string | 是 | 非空，最长 72 个字符 |

成功响应：`200 OK`

```json
{ "code": 0, "message": "success", "data": { "token": "eyJ...", "userId": 1, "name": "患者演示账号", "role": "PATIENT" } }
```

## 2. 患者注册

### `POST /api/auth/register`

公开接口只创建患者账号，并同时创建患者档案。管理员账号由管理端的管理员创建。

```json
{ "username": "patient_new", "password": "password123", "name": "张三", "phone": "13800000000", "gender": 1 }
```

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `username` | string | 是 | 4 到 50 个字符，唯一 |
| `password` | string | 是 | 6 到 72 个字符 |
| `name` | string | 是 | 最长 50 个字符 |
| `phone` | string | 是 | 11 位中国大陆手机号，唯一 |
| `gender` | integer | 是 | `1` 为男，`2` 为女 |

成功响应：`200 OK`

```json
{ "code": 0, "message": "success", "data": null }
```

常见失败响应：账号已存在 `40901`；手机号已存在 `40902`。

## 3. 登出

### `POST /api/auth/logout`

```http
Authorization: Bearer <token>
```

成功响应：`200 OK`

```json
{ "code": 0, "message": "success", "data": null }
```

## 4. 健康检查

### `GET /api/health`

```json
{ "code": 0, "message": "success", "data": { "status": "UP" } }
```

## 5. 管理端科室管理

以下接口均要求管理员在请求头携带有效 Token：

```http
Authorization: Bearer <token>
```

### `GET /api/admin/departments`

查询科室列表。可选查询参数：`name`（名称模糊查询）、`status`（`1` 启用，`0` 停用）。

### `GET /api/admin/departments/{id}`

查询单个科室详情。

### `POST /api/admin/departments`

```json
{ "name": "心血管内科", "description": "提供心血管疾病诊疗服务", "sortOrder": 10 }
```

成功时 `data` 为新建科室的 ID。

### `PUT /api/admin/departments/{id}`

请求体与新建接口一致，用于修改科室名称、简介和排序值。

### `PATCH /api/admin/departments/{id}/status?status=0`

停用科室；传入 `status=1` 可重新启用。科室不提供物理删除接口，以保留医生、排班和预约历史的关联关系。

## 6. 管理端医生管理

以下接口均要求管理员在请求头携带有效 Token。

### `GET /api/admin/doctors`

查询医生列表。可选查询参数：`name`（姓名模糊查询）、`departmentId`（所属科室）、`status`（`1` 启用，`0` 停用）。

### `GET /api/admin/doctors/{id}`

查询单个医生详情。

### `POST /api/admin/doctors`

```json
{
  "name": "张医生",
  "gender": 1,
  "departmentId": 1,
  "doctorCode": "D2026001",
  "title": "主治医师",
  "introduction": "擅长高血压、冠心病等疾病诊疗",
  "avatarUrl": null,
  "sortOrder": 0
}
```

成功时 `data` 为新建医生的 ID。`departmentId` 必须指向一个已启用的科室，`doctorCode` 必须唯一。

### `PUT /api/admin/doctors/{id}`

请求体与新建接口一致，用于修改医生资料。

### `PATCH /api/admin/doctors/{id}/status?status=0`

停用医生；传入 `status=1` 可重新启用。停用不会删除该医生的历史排班与预约记录。

## 7. 管理端批量排班放号

以下接口要求管理员在请求头携带有效 Token。

### `POST /api/admin/schedule-slots/batch`

按日期范围、指定星期和一个或多个连续出诊班次，生成可预约号源。一个 `session` 对应一条号源池记录，例如“上午门诊”或“下午门诊”。只要医生不存在或停用、参数不合法、同一次请求中时段重叠、或与已有排班冲突，整批号源均不会写入。

```json
{
  "doctorId": 1,
  "startDate": "2026-09-14",
  "endDate": "2026-09-18",
  "weekdays": [1, 2, 3, 4, 5],
  "sessions": [
    { "sessionType": "MORNING", "startTime": "08:00", "endTime": "12:00", "capacity": 30, "averageConsultationMinutes": 8 },
    { "sessionType": "AFTERNOON", "startTime": "14:00", "endTime": "17:00", "capacity": 20, "averageConsultationMinutes": 9 }
  ]
}
```

字段说明：`weekdays` 使用 ISO 星期值，`1` 为周一、`7` 为周日；日期范围不能早于当天，最长 31 天；`sessionType` 只能是 `MORNING`、`AFTERNOON`、`OTHER`，前两种名称固定，只有 `OTHER` 需要额外传 `customSessionName`；`capacity` 是整个班次的总号源；`averageConsultationMinutes` 用于患者预约成功后计算预计到诊时间。班次总号源乘以平均接诊时长不能超过班次总时长。

成功响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "createdCount": 10,
    "slots": [
      { "doctorId": 1, "scheduleDate": "2026-09-14", "sessionType": "MORNING", "sessionName": "上午门诊", "startTime": "08:00:00", "endTime": "12:00:00", "averageConsultationMinutes": 8, "totalCapacity": 30, "bookedCapacity": 0, "remainingCapacity": 30, "status": "OPEN" }
    ]
  }
}
```

### `GET /api/admin/schedule-slots?doctorId=1&startDate=2026-09-14&endDate=2026-09-20`

查询日期范围内的排班。`doctorId` 可不传，起止日期必传且范围最长 31 天。返回医生、科室、班次、总号源、已预约和剩余号源。

### `PATCH /api/admin/schedule-slots/{id}/status?status=CLOSED`

关闭班次；传入 `OPEN` 可重新开放。接口使用版本号避免并发修改互相覆盖。

### `PATCH /api/admin/schedule-slots/{id}/capacity?capacity=35`

调整班次总号源。新容量不能小于当前已预约人数，调整后剩余号源会按差额自动重新计算。
