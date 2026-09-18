# 系统规格（System Specification）

## 1. 领域模型

### Book

保存书目元数据和数字授权数量：

- `totalLicenses`：授权总数；
- `availableLicenses`：当前可用授权数；
- `borrowLicense()`：可用授权减一；
- `returnLicense()`：可用授权加一。

### Loan

保存一次借阅事件：

- `id`：不透明的 UUID；
- `book`：关联书籍；
- `userId`：借阅用户；
- `borrowedAt`、`dueAt`、`returnedAt`：借阅生命周期时间。

活跃状态由 `returnedAt == null` 推导，不设置独立的 `status` 持久化字段。

## 2. 必须保持的不变式

```text
0 <= availableLicenses <= totalLicenses
active loans = total licenses - available licenses
one active loan per user and book
an active loan transitions to returned at most once
```

这些不变式是设计的核心验收标准。功能是否返回 200 不是充分条件；并发场景下仍必须保持它们成立。

## 3. 应用架构

```text
BookController  -> BookService  -> BookRepository  -> H2
LoanController  -> LoanService  -> BookRepository
                                  -> LoanRepository
AdminController -> LoanService  -> LoanRepository
```

- Controller 负责 HTTP 参数映射和响应状态；
- Service 负责用例编排、事务和业务规则；
- Entity 聚合内部维护授权数量和借阅状态；
- DTO 与持久化实体分离，避免直接暴露数据库模型；
- Repository 负责数据访问和带行锁的书籍查询。

## 4. 事务与并发规格

### 借阅

1. 在事务中对目标 `Book` 获取 `PESSIMISTIC_WRITE` 行锁。
2. 检查用户是否已有该书的活跃借阅。
3. 检查可用授权是否大于零。
4. 扣减授权并创建 `Loan`。
5. 提交事务；任一步失败则整体回滚。

### 归还

1. 在事务中获取对应 `Book` 的行锁。
2. 重新读取或刷新 `Loan`，避免并发归还时使用过期实体状态。
3. 校验借阅归属。
4. 仅当借阅仍活跃时标记归还并恢复授权。
5. 提交事务；重复归还不得二次释放授权。

同一本书的借阅和归还被数据库行锁串行化；不同书籍之间可以并发。应用进程内锁不作为一致性方案，因为多实例部署时会失效。

## 5. API 与错误规格

- 分页：`page >= 0`，`1 <= size <= 50`；
- 错误统一使用 `ProblemDetail` 风格，并携带稳定业务码；
- `400`：参数、JSON、分页或身份请求头非法；
- `403`：归还他人借阅或访问管理接口缺少管理员角色；
- `404`：书籍或借阅记录不存在；
- `409`：重复借阅或无可用授权；
- `201`：借阅创建成功；
- 归还成功返回更新后的借阅记录，保留历史而非删除记录。

详细接口见 [03-api-contract.md](03-api-contract.md)。

## 6. 持久化规格

- 作业环境使用 H2 内存数据库；
- 生产环境预期使用 PostgreSQL 等共享事务数据库；
- `books.isbn` 唯一；
- `loans.book_id` 外键关联 `books.id`；
- 借阅历史保留，活跃状态由 `returned_at IS NULL` 过滤；
- 当前未通过数据库部分唯一索引表达“每用户每书一个活跃借阅”，而是由持有书籍行锁的应用事务检查。

完整字段、索引和生产演进见 [../zh-CN/database-design.md](../zh-CN/database-design.md)。
