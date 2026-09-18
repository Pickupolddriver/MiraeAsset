# API 契约（API Contract）

## 1. 通用约定

- Base path：`/api`
- 时间：UTC ISO-8601，例如 `2026-09-18T10:15:30Z`
- 分页默认值：`page=0`、`size=20`；`size` 最大为 50
- 运行时机器可读 OpenAPI：`/v3/api-docs`
- Swagger UI：`/swagger-ui.html`

## 2. 身份请求头

| 请求头 | 用途 | 规则 |
|---|---|---|
| `X-User-Id` | 标识普通用户或管理员 | 必填；非空且长度受限 |
| `X-User-Role` | 标识管理角色 | 仅管理接口需要 `ADMIN`；普通接口默认 `USER` |

## 3. 接口清单

| 方法 | 路径 | 成功响应 | 主要失败响应 |
|---|---|---|---|
| `GET` | `/api/books?q=&category=&page=&size=` | `200 PageResponse<BookSummaryResponse>` | `400 INVALID_PAGE` |
| `GET` | `/api/books/{bookId}` | `200 BookDetailResponse` | `404 BOOK_NOT_FOUND` |
| `POST` | `/api/loans` | `201 LoanResponse` | `400`、`404`、`409` |
| `GET` | `/api/loans/current` | `200 LoanResponse[]` | `400 INVALID_REQUEST` 或 `INVALID_USER` |
| `PUT` | `/api/loans/{loanId}/return` | `200 LoanResponse` | `403`、`404` |
| `GET` | `/api/admin/loans/current?page=&size=` | `200 PageResponse<LoanResponse>` | `400`、`403` |

## 4. 请求与响应示例

### 借阅

```http
POST /api/loans
X-User-Id: user-1
Content-Type: application/json

{"bookId": 1}
```

成功返回 `201`，响应包含 `loanId`、`bookId`、`bookTitle`、`userId`、`status`、`borrowedAt` 和 `dueAt`。

### 归还

```http
PUT /api/loans/{loanId}/return
X-User-Id: user-1
```

成功返回状态为 `RETURNED` 的借阅记录。重复调用不会再次释放授权。

### 分页响应

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```
### 错误响应

```json
{
  "type": "about:blank",
  "title": "BOOK_UNAVAILABLE",
  "status": 409,
  "detail": "No available license for this book"
}
```

稳定业务码包括 `BOOK_NOT_FOUND`、`LOAN_NOT_FOUND`、`BOOK_UNAVAILABLE`、`ACTIVE_LOAN_ALREADY_EXISTS`、`LOAN_NOT_OWNED_BY_USER`、`ADMIN_ROLE_REQUIRED` 和 `INVALID_REQUEST`。

管理员接口采用默认拒绝策略：缺失或不是 `ADMIN` 的 `X-User-Role` 都返回 `403 ADMIN_ROLE_REQUIRED`。普通用户接口缺失身份请求头返回 `400 INVALID_REQUEST`，身份头为空或超长返回 `400 INVALID_USER`。

## 5. 契约边界

本文件是面向评审的可读 API 契约。SpringDoc 根据 Controller 和 DTO 在运行时生成机器可读 OpenAPI；如果 API 发生变化，应同时更新本文件、Controller 注解和 API 测试，避免三者漂移。
