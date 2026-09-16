# 电子图书馆性能基线

这是一个针对本次作业实现的轻量、可复现的延迟基线。它测量的是**针对真实运行实例的端到端 HTTP 请求延迟**（curl → Spring Boot → H2），比单元级基准更贴近用户体验。这里的数字是**这台笔记本上的基线**，不代表容量能力；它的用途是为后续改动建立一个可对比的基准线。

## 环境

- 运行时：macOS (Intel)，OpenJDK 21 LTS (Temurin 21.0.12.1)
- Spring Boot 3.4.5，H2 内存库（`MODE=PostgreSQL`），默认内嵌 Tomcat
- 数据集：**5,003 本书**（3 本精选 + 5,000 本通过 `app.seed.bulk.count=5000` 生成）
- 每次测量 = 50 次顺序 HTTP 请求（客户端 `curl`），单客户端、无并发
- 报告指标：**min** / **avg** / **median** / **p95** / **max**，单位**毫秒**

## 结果（毫秒）

| 操作 | min | avg | median | p95 | max |
|---|---|---|---|---|---|
| `GET /api/books?page=0&size=20`（浏览首页） | 3.9 | 7.5 | 6.4 | 9.9 | 32.4 |
| `GET /api/books?q=clean&size=20`（关键词搜索） | 14.5 | 18.1 | 16.5 | 24.9 | 32.2 |
| `GET /api/books?page=200&size=20`（深分页） | 5.4 | 7.3 | 6.7 | 10.6 | 13.2 |
| `GET /api/books/{bookId}`（详情） | 1.9 | 3.2 | 2.6 | 5.2 | 14.6 |
| `POST /api/loans`（借阅，行锁） | 3.9 | 8.9 | 5.3 | 9.9 | 149.6 |
| `PUT /api/loans/{id}/return`（归还，行锁） | 3.1 | 5.1 | 4.1 | 6.7 | 38.9 |
| `GET /api/admin/loans/current`（管理视图） | 2.3 | 3.8 | 3.1 | 6.3 | 12.4 |

## 观察

- **读操作是单位数毫秒。** 浏览、深分页、详情、管理视图的 p95 都在 10ms 以下。即使翻到 5,000 本书的第 200 页，分页成本也保持平稳——因为 H2 只返回请求的那一页切片，而不是整表加载。
- **关键词搜索是最贵的读操作**（中位约 16.5ms）。查询使用 `lower(title) LIKE '%...%'` 和 `lower(author) LIKE`，无法在 H2 上走 B-tree 索引，因此是“全表扫描”。当目录规模更大时，这是索引或全文检索优化的最自然切入点。
- **写操作按设计以锁串行化，且保持快速。** 借阅和归还在 `Book` 行上取 `PESSIMISTIC_WRITE`（悲观写锁）。中位数约 5ms / 4ms；尾部极值（borrow 最大 149.6ms）反映的是当并发事务持有时出现的锁等待，而非稳态成本。在 50 次顺序请求下没有竞争，因此该尾部是一次性异常，不构成趋势。
- **管理视图很廉价。** 它扫描 `loans` 表过滤 `returnedAt IS NULL`。在借阅量很小时很快；一旦借阅历史增多，就需要在 `returned_at`（以及 `user_id`）上加索引。

## 结论 / 取舍

1. **当前架构在一个 H2 实例上从容支撑 5k 本书**，读 p95 < 10ms。对本次作业的数据集，无需任何优化。
2. **`Book` 行锁是正确的并发边界。** 写延迟中位数与读相当，因为它是短事务；对同一本书的操作串行化带来的可预期成本，换来的是正确性收益（不超卖授权）。这与生产设计说明一致：授权分配始终放在唯一权威存储上。
3. **若目录需要进一步扩展**，杠杆最高的是为 `LIKE '%…%'` 浏览查询启用索引或全文检索，而不是借阅路径上的任何东西。架构文档已经规定浏览走只读副本 + 缓存、写操作仅主库。
4. **本文件不是压测。** 并发、吞吐（req/s）和多用户竞争由 `LoanConcurrencyTest` 的不变式（十用户授权竞争下的正确性）另行覆盖，而非这些延迟数字。

## 如何复现

环境要求：Java 21，Maven。

```bash
# 1. 用 5,000 本书启动应用。
JAVA_HOME=$HOME/jdk21/Contents/Home mvn spring-boot:run \
  -Dspring-boot.run.arguments="--app.seed.bulk.enabled=true --app.seed.bulk.count=5000 --server.port=8080"

# 2. 在另一个终端运行基线（默认端口 8080，重复 50 次）。
BASE=http://localhost:8080 REPEATS=50 ./scripts/benchmark.sh

# 只读模式（跳过借阅/归还）：
SKIP_BORROW=1 BASE=http://localhost:8080 REPEATS=50 ./scripts/benchmark.sh
```

批量写入种子与脚本默认关闭，以保证普通运行保持轻量。