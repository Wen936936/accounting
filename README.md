# Accounting · 个人记账与财务管理系统

一个面向个人用户的记账与财务管理后端系统，提供账单的增删改查、分页检索与月度筛选能力，并内置一套面向高并发场景的安全鉴权体系：**JWT 双 Token + Redis 多端隔离 + Lua 原子轮换 + 滑动过期 + 登录防爆破**。

> 核心亮点：无状态 AccessToken 承载高频业务请求，RefreshToken 落地 Redis 实现多端隔离与轮换防重放，配合 7 天滑动过期，用户活跃期间永不掉线。

前端为配套的极简原生 HTML/CSS/JS 页面，与后端分离部署，开箱即可完成登录、记账、分页浏览与登出全流程。

---

## 技术栈

### 后端

| 组件 | 版本 / 说明 |
| --- | --- |
| Spring Boot | 3.2.5（Java 17） |
| MyBatis-Plus | 3.5.7，含分页插件 |
| Spring Security Crypto | BCrypt 密码加密 |
| JJWT | 0.12.6，HS256 签名 |
| 参数校验 | spring-boot-starter-validation (`@Valid`) |
| 其他 | Lombok、Redis Lua 脚本 |

### 数据库与中间件

| 组件 | 版本 / 说明 |
| --- | --- |
| MySQL | 8.x，存储用户与账单数据 |
| Redis | 7.x，存储 RefreshToken、登录失败计数 |

### 前端

| 组件 | 说明 |
| --- | --- |
| HTML5 / CSS3 | `web/index.html`、`web/login.html`、`web/style.css` |
| 原生 JavaScript | `web/app.js`，含统一请求封装与 401 自动刷新重试 |

---

## 核心功能模块

### 鉴权模块

- **JWT 双 Token**：AccessToken（30 分钟）承载业务请求，RefreshToken（7 天）仅用于换取新令牌；两类令牌通过 `type` claim 严格隔离，RefreshToken 无法直接访问业务接口。
- **Redis 存储与多端隔离**：RefreshToken 以 `refresh_token:{userId}:{deviceType}` 为键写入 Redis，`web` / `app` / `pc` 等设备互不影响，避免多端登录互相顶号。
- **Lua 脚本原子轮换防重放**：刷新时通过 Lua 脚本将「校验旧令牌」与「覆盖新令牌 + 重置 TTL」合并为一次原子操作。同一 RefreshToken 并发刷新只会成功一次，其余请求因比对失败被拒绝，彻底杜绝重放攻击。
- **滑动过期**：每次成功刷新都重置 Redis 中 RefreshToken 的 TTL 为 7 天，活跃用户无感续期，长期不活跃则自然失效。
- **登出收敛**：登出仅删除当前设备对应的 RefreshToken，且要求传入的令牌与 Redis 中现存值一致，历史废弃令牌无法踢下线当前会话；AccessToken 保持无状态，自然过期。
- **登录防爆破**：`login_fail:{username}` 记录失败次数，连续失败 5 次锁定 15 分钟；用户不存在时不累加计数，防止恶意锁定他人账号。
- **统一鉴权响应**：Token 过期、无效、刷新失败均返回 HTTP 401 与统一 `Result` 结构，前端据此自动触发刷新流程。
- **安全细节**：JWT 解析对 `userId` 判空防 NPE；刷新前校验用户是否仍存在；拦截器放行 `OPTIONS` 预检与 `/error`，避免 CORS 与错误页死循环。

### 业务模块

- **账单 CRUD**：新增、修改、删除、分页查询，金额使用 `DECIMAL(10,2)`，杜绝浮点误差。
- **严格的横向越权拦截**：所有写操作先查询账单并校验 `userId` 归属，非本人数据一律拒绝；分页查询强制以当前 `userId` 作为过滤条件，从查询层杜绝越权。
- **分页与月度筛选**：基于 MyBatis-Plus 分页插件实现真实分页，支持 `yyyy-MM` 月度过滤，按账单日期倒序排列。
- **VO 数据脱敏**：接口出参统一为 VO，`User` 实体（含 `password` 字段）与 `Bill` 实体均不直接暴露，密码仅以 BCrypt 密文落库且永不出参。
- **统一返回与异常处理**：所有接口返回 `Result<T>`（`{code, msg, data}`）；全局异常处理器统一转换业务异常、参数校验失败、请求体格式错误、404/405 与兜底 500，HTTP 状态码与业务 code 对齐。

---

## 项目结构

```text
accounting
├── pom.xml
├── web/                                  # 极简前端（原生 HTML/JS/CSS）
│   ├── index.html                        # 账单主页
│   ├── login.html                        # 登录 / 注册页
│   ├── app.js                            # 请求封装、401 自动刷新
│   └── style.css
└── src/main
    ├── java/com/tom/accounting
    │   ├── AccountingApplication.java    # 启动类
    │   ├── common/
    │   │   └── Result.java               # 统一返回结构
    │   ├── config/
    │   │   ├── CorsConfig.java           # 跨域配置
    │   │   ├── MybatisPlusConfig.java    # 分页插件
    │   │   ├── SecurityConfig.java       # BCryptPasswordEncoder
    │   │   └── WebMvcConfig.java         # 拦截器注册与放行规则
    │   ├── controller/
    │   │   ├── AuthController.java       # 注册 / 登录 / 刷新 / 登出
    │   │   └── BillController.java       # 账单接口
    │   ├── service/
    │   │   ├── UserService.java
    │   │   ├── BillService.java
    │   │   └── impl/
    │   │       ├── UserServiceImpl.java  # 双 Token、防爆破、Lua 轮换
    │   │       └── BillServiceImpl.java  # 归属权校验、分页
    │   ├── mapper/
    │   │   ├── UserMapper.java
    │   │   └── BillMapper.java
    │   ├── entity/                       # 数据库实体，不对外暴露
    │   │   ├── User.java
    │   │   └── Bill.java
    │   ├── dto/                          # 请求参数 + 校验注解
    │   ├── vo/                           # 响应数据（脱敏）
    │   ├── interceptor/
    │   │   └── JwtInterceptor.java       # Bearer 校验，注入 userId
    │   ├── exception/                    # 业务异常与全局处理器
    │   └── util/
    │       └── JwtUtil.java              # 令牌签发与解析
    └── resources
        ├── application.yml               # 本地配置（已被 .gitignore 忽略）
        └── application.yml.example       # 配置模板，提交到仓库
```

---

## 本地运行指南

### 1. 环境准备

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Redis 7.x

### 2. 初始化数据库

在 MySQL 中执行建库与建表语句（库名与 `application.yml` 中的连接地址保持一致）：

```sql
CREATE DATABASE IF NOT EXISTS accounting_db
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE accounting_db;

-- 用户表，表名使用反引号避开 MySQL 关键字
CREATE TABLE `user` (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(50)  NOT NULL COMMENT '用户名',
    password   VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    nickname   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 账单表
CREATE TABLE bill (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT        NOT NULL COMMENT '归属用户 id',
    category_id BIGINT        NOT NULL COMMENT '分类 id',
    amount      DECIMAL(10,2) NOT NULL COMMENT '金额',
    type        TINYINT       NOT NULL COMMENT '类型：1 支出 2 收入',
    bill_date   DATE          NOT NULL COMMENT '账单发生日期',
    remark      VARCHAR(200)  DEFAULT NULL COMMENT '备注',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_bill_date (user_id, bill_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账单表';
```

### 3. 启动 Redis

```bash
# Windows
redis-server

# macOS / Linux
redis-server --daemonize yes

# 连通性验证
redis-cli ping   # 期望输出 PONG
```

### 4. 配置 application.yml

复制配置模板并填入本地真实值（该文件已被 `.gitignore` 忽略，**切勿提交真实密码与密钥**）：

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

```yaml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/accounting_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: <YOUR_DB_USERNAME>
    password: <YOUR_DB_PASSWORD>
  data:
    redis:
      host: localhost
      port: 6379
      # password: <YOUR_REDIS_PASSWORD>   # 若 Redis 启用了密码

jwt:
  # HS256 至少 32 字节，生产环境请使用足够长的随机字符串
  secret: <YOUR_JWT_SECRET_KEY>
  access-expiration-ms: 1800000      # 30 分钟
  refresh-expiration-ms: 604800000   # 7 天
```

生成随机密钥的参考命令：

```bash
openssl rand -base64 48
```

### 5. 启动后端

```bash
# 开发模式
mvn spring-boot:run

# 或打包运行
mvn clean package -DskipTests
java -jar target/accounting-1.0-SNAPSHOT.jar
```

启动成功后默认监听 `http://localhost:8080`。

### 6. 启动前端

前端为纯静态页面，可直接用浏览器打开 `web/login.html`，或使用任意静态服务器托管：

```bash
# 示例：Python 内置静态服务器
python -m http.server 5173 --directory web
```

如后端地址或端口不同，请同步修改 `web/app.js` 中的 `API_BASE` 常量。

> 生产部署时请将 `CorsConfig` 中的 `allowedOriginPatterns("*")` 收敛为具体的前端域名。

---

## 接口文档预览

所有接口统一返回 `{ code, msg, data }`，成功时 `code` 为 `200`。除认证接口外的接口均需在请求头携带 `Authorization: Bearer <AccessToken>`。

### 认证模块

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | 用户注册，密码 BCrypt 加密入库 | 否 |
| POST | `/api/v1/auth/login` | 登录，返回 `accessToken`、`refreshToken`、`expiresIn`、`nickname` | 否 |
| POST | `/api/v1/auth/refresh` | 刷新令牌，轮换并下发新的双 Token | 否 |
| POST | `/api/v1/auth/logout` | 登出，删除当前设备的 RefreshToken | 否（凭 RefreshToken） |

**登录请求示例**

```json
{
  "username": "tom",
  "password": "123456",
  "deviceType": "web"
}
```

**登录响应示例**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 1800,
    "nickname": "Tom"
  }
}
```

### 账单模块

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/v1/bills` | 新增账单 | 是 |
| GET | `/api/v1/bills?page=1&size=10&month=2026-11` | 分页查询当前用户账单，`month` 可选（`yyyy-MM`） | 是 |
| PUT | `/api/v1/bills/{id}` | 修改本人账单 | 是 |
| DELETE | `/api/v1/bills/{id}` | 删除本人账单 | 是 |

**新增账单请求示例**

```json
{
  "categoryId": 1,
  "amount": 35.50,
  "type": 1,
  "billDate": "2026-11-11",
  "remark": "午餐"
}
```

**分页响应示例**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 42,
    "records": [
      {
        "id": 101,
        "categoryId": 1,
        "amount": 35.50,
        "type": 1,
        "billDate": "2026-11-11",
        "remark": "午餐",
        "createdAt": "2026-11-11T12:30:00",
        "updatedAt": "2026-11-11T12:30:00"
      }
    ]
  }
}
```

### 状态码约定

| HTTP | code | 说明 |
| --- | --- | --- |
| 200 | 200 | 成功 |
| 400 | 400 | 参数校验失败 / 请求体格式错误 |
| 401 | 401 | 未登录、Token 过期或无效、刷新失败 |
| 404 | 404 | 接口不存在 |
| 405 | 405 | 请求方法不支持 |
| 500 | 500 | 业务异常（凭 `msg` 提示）或系统异常 |

---

## TODO

- [ ] **V2：引入 Vue 3 重构前端**，以组件化方式重构账单列表与表单，接入状态管理与路由，替换当前原生 JS 方案。
- [ ] **分类统计与可视化**：增加按分类、按月的收支汇总接口，并接入 ECharts 输出分类占比饼图与月度趋势折线图。
- [ ] **云端部署上线**：容器化打包，部署至云服务器并配置 HTTPS、Nginx 反向代理与 Redis 持久化，收敛 CORS 白名单。
