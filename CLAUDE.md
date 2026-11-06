# 记账系统项目 AI 开发规范 (CLAUDE.md)

## 1. 项目背景与技术栈
- 项目名称：accounting（记账系统）
- 核心包名：`com.tom.accounting`
- 技术栈：Spring Boot 3.2.5 + MyBatis-Plus + MySQL 8 + Lombok + spring-security-crypto (BCrypt)
- 前端：暂不涉及，目前专注后端 API 开发。

## 2. 核心架构规范
1. **严格分层**：必须使用 Controller -> Service -> Mapper 三层架构。
    - Controller：只负责接收参数、调用 Service、返回 Result，**绝对不允许写业务逻辑**。
    - Service：分为接口和实现类（放在 `impl` 包下），负责核心业务逻辑、校验、抛异常。
    - Mapper：只负责数据库交互。
2. **统一返回格式**：所有接口必须返回 `Result<T>`，格式为 `{code, msg, data}`。
3. **全局异常处理**：使用 `@RestControllerAdvice` 统一捕获异常。业务错误抛出 `BusinessException`，参数校验失败通过 `@Valid` 自动拦截。
4. **数据传输**：请求参数一律使用 DTO 接收，并加上 `@NotBlank`、`@Size` 等校验注解。实体类 Entity 不作为接口入参或出参（敏感字段如 password 必须剔除）。

## 3. 数据库与安全规范
1. 所有表主键统一使用 `BIGINT` 自增。
2. 密码必须使用 `BCryptPasswordEncoder` 加密后入库，明文绝不入库、绝不返回。
3. 金额字段统一使用 `DECIMAL(10,2)`，禁用 Float/Double。
4. 涉及用户数据的操作（增删改查），必须校验**数据归属权**（例如：只能删除自己的账单）。

## 4. AI 代码输出要求
1. **禁止擅自发挥**：只输出我要求修改或新增的文件代码，不要输出项目结构说明，不要生成多余的不相关的类。
2. **按文件输出**：如果涉及多个文件，请按包路径逐个输出完整代码（包含 package 和 import）。
3. **保持上下文连贯**：在修改已有代码时，保持原有的代码风格和命名规范。