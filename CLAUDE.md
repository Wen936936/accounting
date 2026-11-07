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

## 5. 版本控制规范
- 每次完成一个独立的功能模块（如：注册、登录、账单CRUD），必须整理代码并准备提交。
- Git commit 信息必须遵循 Angular 规范：`feat: 新增xx功能`、`fix: 修复xx问题`、`chore: 杂项`。
- 绝对禁止将数据库密码、JWT Secret 等敏感信息硬编码在代码中，统一在 `application.yml` 中配置，并保留 `application.yml.example` 模板。

## 6. 安全与鉴权进阶规范（Review 补充）
1. **跨域与拦截器放行**：JwtInterceptor 中必须放行 `OPTIONS` 预检请求，WebMvcConfig 中必须排除 `/error` 路径，防止 CORS 拦截和死循环。
2. **登出接口无状态化**：登出接口 `/api/v1/auth/logout` 放行拦截，通过接收参数中的 `refreshToken` 去 Redis 删除，不依赖 Header 中可能已过期的 AccessToken。
3. **HTTP 状态码对齐**：鉴权失败（Token过期/无效/刷新失败）必须返回 HTTP 401 状态码，而不仅仅是修改 Body 里的 code。
4. **防重放与并发**：
    - 刷新 Token 时：必须校验用户是否存在于数据库。
    - 多端登录控制：Redis Key 结构改为 `refresh_token:{userId}:{deviceType}`，允许通过传入 deviceType 区分设备（如 PC、APP），避免强行顶号。
    - 防重放：在刷新接口中实现 Token 轮换（Rotation），每次刷新下发新的 refreshToken，旧的作废。
5. **滑动过期**：每次成功刷新 AccessToken 时，重置 Redis 中对应 refreshToken 的 TTL 为 7 天，实现活跃用户无感续期。
6. **NPE 防护**：JwtUtil 解析 userId 时必须判空，防止旧版本 Token 导致空指针崩溃。
7. **登录防爆破**：登录接口中，使用 Redis 记录 `login_fail:{username}`，失败超过 5 次锁定 15 分钟。
8. **返回值完善**：登录和刷新接口的 VO 中必须增加 `expiresIn` 字段（单位秒），告知前端 AccessToken 的有效期。

## 7. 鉴权业务决策（最终版）
1. **滑动过期**：每次刷新 AccessToken 时，必须重置 Redis 中 refreshToken 的 TTL 为 7 天（活跃用户永不掉线）。
2. **多端并存**：Redis Key 格式严格为 `refresh_token:{userId}:{deviceType}`。登录和刷新时必须携带 deviceType（如 web, app, pc），不同设备互不影响。
3. **登出策略**：为了保持高并发，AccessToken 保持无状态。登出时仅删除 Redis 中的 refreshToken，AccessToken 允许自然过期（30分钟），不做 Redis 黑名单拦截。
4. **防爆破**：登录失败使用 Redis 记录 `login_fail:{username}`，错误 5 次锁定 15 分钟（TTL 15分钟）。