# SpecDD 设计入口

本目录记录电子图书馆服务从需求分析到实现验证的规格化设计。它把原始需求、领域规则、API 契约、验收标准和测试映射串联起来，作为开发与评审的共同入口。

## 1. 开发链路

```text
需求分析
   ↓
SpecDD：领域、API、数据、并发与验收标准
   ↓
Agentic Coding：按规格拆分任务并实现
   ↓
自动化验证：单元测试、API 测试、并发测试、覆盖率
```

| 阶段     | 主要产物                                                                                                         | 目的                               |
|----------|------------------------------------------------------------------------------------------------------------------|------------------------------------|
| 需求分析 | [01-requirement-analysis.md](01-requirement-analysis.md)                                                         | 澄清范围、角色、假设和非目标       |
| 系统规格 | [02-system-spec.md](02-system-spec.md)                                                                           | 定义领域不变式、事务边界和错误语义 |
| API 契约 | [03-api-contract.md](03-api-contract.md)                                                                         | 定义接口、请求、响应和状态码       |
| 验收追踪 | [04-traceability-matrix.md](04-traceability-matrix.md)                                                           | 将需求映射到代码和测试             |
| 详细设计 | [../zh-CN/architecture.md](../zh-CN/architecture.md)、[../zh-CN/database-design.md](../zh-CN/database-design.md) | 记录架构、数据库和生产演进方案     |

## 2. 与现有文档的关系

- [../initialRequirement.md](../initialRequirement.md)：原始任务要求与范围。
- [../expandedRequirement.md](../expandedRequirement.md)：结构化后的需求说明。
- [../zh-CN/design-answers.md](../zh-CN/design-answers.md)：面向原始题目的逐项应答。
- [../../README.zh-CN.md](../../README.zh-CN.md)：运行方式、设计摘要和演示入口。
- 运行时 OpenAPI：应用启动后访问 `/swagger-ui.html` 或 `/v3/api-docs`。

## 3. SpecDD 使用规则

后续修改按以下顺序更新：

1. 先更新需求或假设，并说明变更原因。
2. 更新受影响的领域规则、API 契约或数据设计。
3. 更新追踪矩阵和验收测试。
4. 再修改代码并运行 `mvn clean verify`。
5. 在 README 或提交说明中记录关键取舍。

## 4. Agentic Coding 的边界

Agent 可以协助拆分任务、生成实现草稿、补充测试和执行验证，但以下决策由项目负责人确认：

- 需求解释与范围控制；
- 领域不变式和事务边界；
- API 兼容性与错误语义；
- 并发策略及其取舍；
- 测试是否真正验证了规格，而不只是提高覆盖率。
