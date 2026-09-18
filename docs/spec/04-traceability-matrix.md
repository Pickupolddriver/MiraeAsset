# 需求追踪矩阵（Traceability Matrix）

| 需求                   | 规格规则                                   | API                              | 主要实现                                          | 验证测试                                                                                                                                           |
|------------------------|--------------------------------------------|----------------------------------|---------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| REQ-BOOK-001 浏览书籍  | 分页、关键词、分类；非法分页返回 400       | `GET /api/books`                 | `BookController`、`BookService`                   | `LibraryApiTest.canBrowseAndViewBookDetails`、`rejectsInvalidPaginationAndMalformedRequests`                                                       |
| REQ-BOOK-002 书籍详情  | 不存在返回 404                             | `GET /api/books/{bookId}`        | `BookController`、`BookService`                   | `canBrowseAndViewBookDetails`、`returnsStableErrorsForMissingResourcesAndHeaders`                                                                  |
| REQ-LOAN-001 借阅      | 有授权、禁止重复借阅、扣减与创建原子提交   | `POST /api/loans`                | `LoanService`、`Book.borrowLicense()`             | `borrowAndReturnMaintainsAvailability`、`duplicateBorrowIsRejected`、`unavailableBookIsRejected`                                                   |
| REQ-LOAN-002 当前借阅  | 只返回当前用户的活跃借阅                   | `GET /api/loans/current`         | `LoanService.currentLoans()`                      | `canBorrowViewCurrentLoansAndReturn`                                                                                                               |
| REQ-LOAN-003 归还      | 所有权校验、幂等、恢复授权                 | `PUT /api/loans/{loanId}/return` | `LoanService.returnLoan()`、`Loan.markReturned()` | `returnMustBelongToRequestingUser`、`repeatedReturnIsIdempotent`、`cannotReturnAnotherUsersLoan`                                                   |
| REQ-ADMIN-001 管理视图 | 仅 ADMIN；只读分页查询                     | `GET /api/admin/loans/current`   | `AdminController`、`AdminRoleInterceptor`         | `adminCanSeeAllActiveLoansAcrossUsers`、`userRoleIsForbiddenFromAdminEndpoint`                                                                     |
| 并发不变式             | 可用授权不为负；同书操作串行；归还最多一次 | 借阅和归还接口                   | `PESSIMISTIC_WRITE` 书籍行锁                      | `onlyAvailableLicensesCanBeBorrowedConcurrently`、`sameUserCanOnlyBorrowTheSameBookOnceConcurrently`、`repeatedReturnsAreIdempotentWhenConcurrent` |
| 错误契约               | 400/403/404/409 与稳定业务码               | 全部 API                         | `ApiExceptionHandler`                             | `rejectsInvalidPaginationAndMalformedRequests`、`returnsStableErrorsForMissingResourcesAndHeaders`                                                 |

## 代码与文档入口

- Controller：`src/main/java/com/miraeasset/elibrary/book/BookController.java`、`loan/LoanController.java`、
  `loan/AdminController.java`
- 领域模型：`book/Book.java`、`loan/Loan.java`
- 事务用例：`loan/LoanService.java`
- 错误契约：`common/ApiExceptionHandler.java`
- API 测试：`src/test/java/com/miraeasset/elibrary/LibraryApiTest.java`
- 并发测试：`src/test/java/com/miraeasset/elibrary/loan/LoanConcurrencyTest.java`

## 验证命令

```bash
mvn clean verify
```

验收标准不是只有测试通过，还包括：核心不变式在并发测试中成立、API 错误语义稳定、需求变更可以沿矩阵找到受影响的设计和测试。
