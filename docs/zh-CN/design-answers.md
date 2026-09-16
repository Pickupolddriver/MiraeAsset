# 需求应答与设计说明（Design Answers）

> 本文件针对 [initialRequirement.md](../initialRequirement.md) 逐项作答：功能覆盖、以及题目强调的六个关注点（应用架构 / 领域模型 / API 设计 / 代码组织 / 错误处理 / 可维护性）。同时说明本作业刻意控制范围的取舍。
>
> 更多细节见 [README.zh-CN.md](../../README.zh-CN.md) 与 [architecture.md](architecture.md)。

## 一、需求覆盖对照

| 需求（原文） | 实现 | 现状 |
|---|---|---|
| 瀏覽書籍 Browse books | `GET /api/books?q=&category=&page=&size=`（分页 + 关键词/分类过滤） | ✅ |
| 查詢書籍詳細資料 Book detail | `GET /api/books/{bookId}` | ✅ |
| 借閱書籍 Borrow | `POST /api/loans`（`X-User-Id` + `{ "bookId" }`） | ✅ |
| 歸還書籍 Return | `PUT /api/loans/{loanId}/return` | ✅ |
| 查看目前已借閱的書籍 Current loans | 用户侧 `GET /api/loans/current`；管理侧 `GET /api/admin/loans/current` | ✅ |

题目允许“未明确的点可作合理假设”。我们对如下点做了显式假设并写进 README：

- **“目前已借阅的书籍”** 解释为用户侧“我自己的活跃借阅”，另外补一个管理侧“所有用户的活跃借阅”。
- **数字授权并发模型**：每本书有有限授权数，借阅消耗一个、归还释放一个。这让借阅有意义，也形成了并发边界。
- **身份边界**：用 `X-User-Id` 头标识调用者，`X-User-Role`（默认 `USER`）标识角色。这是刻意的最小假设，生产会换成认证主体。

### 范围克制（题目：请控制实现范围在合理程度）

本作业**有意不实现**：认证、图书目录管理（新增/编辑/删除书目）、预约/等待列表、续借、罚款、数字文件存储与流媒体。这些超出题干五个功能点，会在“设计取舍”里说明为什么不纳入。

## 二、应用架构（Application Architecture）

两条层分离：**读路径**与**命令路径**。

- 读路径：`BookController` → `BookService`（只读查询）→ `BookRepository` → H2。浏览/详情是无状态、可横向扩展的查询。
- 命令路径：`LoanController` → `LoanService`（事务命令）→ `BookRepository` + `LoanRepository`。

**并发边界是 `Book` 行**，不是全局应用锁：

- 借阅/归还在该行上取 `PESSIMISTIC_WRITE`，同一本书的操作被数据库串行化，不同书可并行。
- 临界区很小——只有改变授权数的短事务。既避免超卖（oversell），也避免整库串行化。

生产版见 [architecture.md](architecture.md)：读走 Redis + 只读副本，借/还必须走 PostgreSQL 主库同事务，授权决策永不由缓存或滞后副本决定。

**为什么行锁而不是分布式锁？** 跨实例的进程内锁（如 JVM `synchronized`）在水平扩展时会失效；而数据库行锁在单实例（作业）与多实例（生产）下天然一致，且代码最简单。只有出现更复杂分配策略（等待列表/FIFO）时，才需要每本书持久的有序序列。

## 三、领域模型设计（Domain Modelling）

两个聚合根，职责清晰：

| 实体 | 字段 / 职责 | 关键域逻辑 |
|---|---|---|
| `Book` | 书目元数据 + `totalLicenses` / `availableLicenses` | `borrowLicense()` / `returnLicense()` 维护 0 ≤ available ≤ total 不变式 |
| `Loan` | `book, userId, borrowedAt, dueAt, returnedAt` | 活跃状态由 `returnedAt == null` **推导**；`markReturned()` 幂等 |

建模决策：

- **状态用推导而非存储**：Loan 没有 `status` 列，用 `returnedAt` 是否为 null 推导。避免状态字段与时间戳不一致。
- **生命周期保证（domain invariants）**：
  - 0 ≤ availableLicenses ≤ totalLicenses
  - active loans = total licenses − available licenses
  - 每用户每书只有一个活跃借阅
  - 一个活跃借阅最多只转一次“已归还”
- **借阅是有界业务事件**；归还幂等（重复归还不重复释放授权）。
- **时间用可注入的 `Clock` + UTC**：测试可确定。`dueAt` 由 `borrowedAt + 14 天` 推导，而非独立可写字段。

`Book.userId` 为什么是字符串：没有任何用户注册表，userId 是身份边界而非外键。这是本题“最小假设”的直接体现。

## 四、API 设计（API Design）

### 资源式 REST、语义化动词

```text
GET    /api/books?q=&category=&page=&size=   浏览（分页 + 过滤）
GET    /api/books/{bookId}                   详情
POST   /api/loans                            借阅（创建 Loan）
GET    /api/loans/current                    当前用户活跃借阅
PUT    /api/loans/{loanId}/return            归还
GET    /api/admin/loans/current              管理侧：所有用户活跃借阅（需 ADMIN）
```

- `POST /api/loans` 返回 `201` + 资源；`PUT .../return` 是动词化子资源，表达“归还”这个业务动作，比 `DELETE` 更贴切（loan 记录仍保留）。
- 读取一律用 GET；写用 POST/PUT，符合命令查询关注点分离。

### 校验与错误契约（见 §五）

- 分页参数显式校验（`page≥0`、`1≤size≤50`）。
- 返回显式 `PageResponse` 封装（content/page/size/totalElements），不暴露 Spring Data `PageImpl`，保持契约稳定。
- 身份校验集中在 `PrincipalArgumentResolver`（`X-User-Id` / `X-User-Role`），一次拒绝非法值。

## 五、错误处理（Error Handling）

统一采用 Spring `ProblemDetail` + 稳定业务码，通过 `ApiExceptionHandler`（`@RestControllerAdvice`）：

| 场景 | HTTP | 业务码示例 |
|---|---|---|
| 资源不存在 | 404 | `BOOK_NOT_FOUND`, `LOAN_NOT_FOUND` |
| 业务冲突 | 409 | `ACTIVE_LOAN_ALREADY_EXISTS`, `BOOK_UNAVAILABLE` |
| 非本人借阅 / 非 ADMIN | 403 | `LOAN_NOT_OWNED_BY_USER`, `ADMIN_ROLE_REQUIRED` |
| 输入非法（含分页、畸形 JSON、类型不匹配、请求头缺失/非法） | 400 | `INVALID_REQUEST`, `INVALID_PAGE`, `INVALID_USER`, `INVALID_ROLE` |

设计要点：

- **明确区分客户端错误(4xx)与业务冲突(409)**，语义清晰。
- **不外泄框架底层细节**：`HttpMessageNotReadableException`、`MethodArgumentTypeMismatchException` 等被映射成统一的 `INVALID_REQUEST`，不给客户端暴露 Jackson/序列化内部。
- 错误码是**稳定的应用码**（title 属性同时带 code），便于客户端分支与排障。

## 六、代码组织与可维护性（Code Organization & Maintainability）

按“领域包”而非“技术分层”组织：

```text
com.miraeasset.elibrary
├── book/            # 书籍聚合：Book, BookRepository, BookService, dto/
├── loan/            # 借阅聚合：Loan, LoanRepository, LoanService,
│                    #           LoanController, AdminController, dto/
├── identity/        # Principal(userId, role), Role
├── common/          # PageResponse, 异常类型, ApiExceptionHandler, TimeConfig
└── config/          # OpenApiConfig, DataSeeder, PrincipalArgumentResolver, WebConfig, Favicon, TimeConfig
```

可维护性手段：

- **薄 Controller、业务在 Service**：Controller 只做参数映射，事务与领域逻辑在 Service。
- **DTO 与实体分离**：`BookDetailResponse` / `BookSummaryResponse` / `LoanResponse` / `BorrowBookRequest` 是 record，通过静态 `from()` 映射，避免实体直接漏出 JSON。
- **领域规则封装在聚合内部**（`Book.borrowLicense()` 抛 `IllegalStateException`），Service 再把它翻译成 `BookUnavailable` 业务冲突。
- **可测试性**：`Clock` 注入、无全局单例、依赖注入。测试覆盖正常流 + 边界 + 十用户并发竞争 + 幂等返还。

## 七、交付（Deliverables）

- **源代码**：Spring Boot 项目，Java 21，Maven 构建。
- **README（中英）**：范围、设计、API、运行方式。
- **架构文档（中英）**：作业部署 + 生产部署、路由规则、故障边界、取舍。
- **性能基线**：`scripts/benchmark.sh` + `docs/zh-CN/performance-baseline.md`（5000 本书实测 p95/avg/中位，含结论）。
- **测试**：`mvn clean verify` 17 个测试全绿（含并发不变式测试）。

## 八、题目备注部分的回应（Notes）

> 本題沒有唯一正確答案。請著重乾淨、易維護的程式碼與合理工程決策。相比功能完整，更重視思考過程與設計取捨（Trade-offs）。

- **没有唯一答案** → 我们在 README/架构/本文中显式记录了每个关键决策的“为什么”与“备选方案”。
- **干净、易维护** → 领域包划分、薄控制器、DTO 隔离、统一错误契约、可注入 Clock、构造型注入。
- **设计取舍** → 处处写下了 trade-off：行锁 vs 全局锁、H2 vs PostgreSQL、推导状态 vs 存储状态、用户 header vs 认证、管理面做 vs 不做。

**最重要的取舍：控制范围。** 我们刻意没有把“认证系统、目录管理、等待列表”做成一个大而全的系统，因为：(a) 超出题干五个功能点；(b) 会增加未被要求的复杂度；(c) 题目明确要求“合理程度”。我们把省下来的复杂度用于把核心借阅路径做正确、可维护、有并发保证、可重复测量。