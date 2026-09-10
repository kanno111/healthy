# 接口使用说明

## 基础约定

| 项目 | 说明 |
| --- | --- |
| 网关地址 | `http://localhost:8088` |
| API 前缀 | `/api` |
| 内容类型 | `application/json` |
| 鉴权方式 | `Authorization: Bearer <JWT>` |

所有接口均使用以下统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| 字段 | 说明 |
| --- | --- |
| `code` | 业务状态码；`0` 表示成功 |
| `message` | 响应描述 |
| `data` | 响应数据；无数据时为 `null` |

## 鉴权说明

`POST /api/auth/login` 与 `GET /api/health` 无需登录；其余接口默认都需要在请求头携带 JWT：

```http
Authorization: Bearer <token>
```

后端会同时校验 JWT 签名、JWT 有效期和 Redis `db 3` 中的有效会话。登录成功后，Redis 以 `auth:token:` 加 Token SHA-256 摘要作为键保存会话；TTL 与 JWT 过期时间一致。登出后删除该键，旧 Token 会立即失效。

## 本地演示账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `patient_demo` | `123456` | `PATIENT` |
| `staff_demo` | `123456` | `STAFF` |

## 本地访问链路

```text
浏览器
  → Nginx（http://localhost:8088）
  → /api/* 请求代理
  → Spring Boot（http://127.0.0.1:8080）
  → MySQL / Redis db 3
```

前端静态页面由 Nginx 提供；只有以 `/api/` 开头的请求会被反向代理到 Spring Boot。

## 状态码速查

| HTTP 状态 | 业务码 | 含义 |
| --- | --- | --- |
| `200` | `0` | 请求成功 |
| `400` | `40001` | 参数校验失败 |
| `401` | `40100` | 未登录、Token 无效或 Redis 会话已失效 |
| `401` | `40101` | 账号或密码错误 |
| `403` | `40300` | 没有操作权限 |
| `404` | `40400` | 资源不存在 |
| `409` | `40900` | 资源状态冲突 |
| `500` | `50000` | 服务端内部错误 |
