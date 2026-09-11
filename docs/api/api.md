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
