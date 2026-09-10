# 智约医疗接口文档

## 1. 登录

### `POST /api/auth/login`

#### 请求体

```json
{
  "username": "patient_demo",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `username` | string | 是 | 非空，最长 50 个字符 |
| `password` | string | 是 | 非空，最长 72 个字符 |

#### 成功响应：`200 OK`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "name": "患者演示账号",
    "role": "PATIENT"
  }
}
```

| 字段 | 说明 |
| --- | --- |
| `token` | 后续受保护接口需要携带的 JWT |
| `userId` | 当前登录用户 ID |
| `name` | 用户展示名称 |
| `role` | `PATIENT`（患者）或 `STAFF`（运营人员） |

#### 失败响应：`401 Unauthorized`

```json
{
  "code": 40101,
  "message": "账号或密码错误",
  "data": null
}
```

#### 参数错误：`400 Bad Request`

```json
{
  "code": 40001,
  "message": "账号不能为空",
  "data": null
}
```

## 2. 登出

### `POST /api/auth/logout`

#### 请求头

```http
Authorization: Bearer <token>
```

#### 成功响应：`200 OK`

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 未登录或 Token 已失效：`401 Unauthorized`

```json
{
  "code": 40100,
  "message": "未登录或登录已失效",
  "data": null
}
```

## 3. 健康检查

### `GET /api/health`

#### 成功响应：`200 OK`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "UP"
  }
}
```
