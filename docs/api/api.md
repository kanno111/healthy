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
