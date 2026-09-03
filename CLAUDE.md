# BiliBili_backend — 项目指南

## 协作约定

- **所有对话均使用中文**，包括代码注释之外的一切回复、说明、建议。
- **新增或修改接口后，必须同步更新** `docs/postman/API_TEST_GUIDE.md`，补充对应接口的路径、请求参数、正常场景和异常场景，保持测试文档与代码始终一致。
- **完成一个功能模块后，必须在下方"已开发功能"章节简要记录**，包含模块名称、核心接口和关键设计点。
- **未完成的功能以 TODO 形式记录在下方"待开发 TODO"章节**，完成开发后立即从该章节删除对应条目。

---

## 代码注释规范

注释语言统一使用**中文**，但只在真正有价值的地方添加，不做逐行注释。

### 必须加注释的场景

| 场景 | 示例 |
|---|---|
| 关键业务步骤（多步骤流程中的每个阶段） | 登录流程中的验证码校验、密码比对、Token 生成各步骤 |
| 安全约束或防攻击设计 | 账号不存在与密码错误返回同一错误码（防枚举攻击） |
| 非显而易见的技术决策 | 为什么用 `getSession(false)` 而不是 `getSession()` |
| 副作用或资源清理 | ThreadLocal 在 `afterCompletion` 中清除，防止线程池内存泄漏 |
| 公共接口/方法 | Controller 方法、Service 接口方法用 `/** */` Javadoc 风格说明 |

### 禁止添加注释的场景

- 代码本身已自解释（变量名、方法名已清晰表达含义）
- 仅重复代码逻辑的"翻译注释"（如 `// 设置用户名` 对着 `setUserName()`）
- 临时调试注释（不得提交到代码库）

### 注释格式

- **方法级**：使用 `/** */` Javadoc 风格，简述功能 + 关键约束
- **步骤级**：使用 `//` 单行注释，置于代码行上方
- 注释内容聚焦"为什么"而非"做了什么"

---

## 已开发功能

### 用户认证与权限管理模块

**客户端（C端）** — `medium_web`，端口 7071

| 接口 | 方法 | 路径 |
|---|---|---|
| 获取验证码 | GET | `/api/v1/client/checkCode` |
| 用户注册 | POST | `/api/v1/client/register` |
| 用户登录 | POST | `/api/v1/client/login` |
| 自动登录 | POST | `/api/v1/client/autoLogin` |
| 退出登录 | POST | `/api/v1/client/logout` |

- 登录态：无状态 Token + Redis（TTL 7 天，剩余不足 1 天自动续期）
- 鉴权：`TokenAuthInterceptor` 解析 `Authorization: Bearer <token>`

**管理端（B端）** — `medium_admin`，端口 7072，context-path `/admin`

| 接口 | 方法 | 路径 |
|---|---|---|
| 获取验证码 | GET | `/api/v1/admin/checkCode` |
| 管理员注册 | POST | `/api/v1/admin/register` |
| 管理员登录 | POST | `/api/v1/admin/login` |
| 管理员退出 | POST | `/api/v1/admin/logout` |

- 登录态：HttpSession（30 分钟无操作失效，关闭浏览器失效）
- 鉴权：`AdminAuthInterceptor` 校验 Session 中 `ADMIN_USER` 属性
- 权限控制：登录时强校验 `permission=3`，非管理员拒绝访问

**公共基础设施**（均在 `medium_common`）
- `ResponseVO`：统一响应体，含 status / code / info / data
- `BusinessException` + `ErrorCodeEnum`：业务异常体系（401/601~605/500）
- `GlobalExceptionHandler`：全局异常处理，统一返回 JSON
- `RedisUtils`：Redis 封装，含所有 Key 前缀常量和 TTL 常量
- `UserContext`：ThreadLocal 用户上下文，请求结束后自动清除
- `BCryptPasswordEncoder`：密码加密，禁止明文存储与比对

---

## 待开发 TODO

### 用户认证与权限管理模块（待补全）

- [ ] **C端 - 用户信息修改**：修改昵称、头像、个人简介等基本资料
- [ ] **C端 - 修改密码**：需校验旧密码后方可修改
- [ ] **B端 - 用户列表查询**：分页查询所有用户，支持按账号/状态筛选
- [ ] **B端 - 启用/禁用用户**：管理员修改 `status` 字段，禁用后该用户无法登录
- [ ] **B端 - 修改用户权限**：管理员调整用户 `permission`（如升级为 VIP）

---

## 项目简介

基于 Spring Boot 的多模块 Maven 项目，实现类 BiliBili 视频平台的后端服务，采用共享库模式将公共逻辑下沉到 `medium_common` 模块。

- **包根路径：** `com.xypu`
- **JDK：** 1.8
- **Spring Boot：** 2.7.15

---

## 模块架构

| 模块 | 职责 | 端口 | 上下文路径 |
|---|---|---|---|
| `BiliBili_backend`（父模块） | 统一管理依赖版本，不含业务代码 | — | — |
| `medium_common` | 公共库：实体、Mapper、Service、配置类、工具类 | — | — |
| `medium_web` | 客户端 REST API | 7071 | `/` |
| `medium_admin` | 管理后台 REST API | 7072 | `/admin` |

`medium_web` 和 `medium_admin` 均依赖 `medium_common`，通过组件扫描（`com.xypu`）自动加载其中所有 Bean。

---

## 技术栈

| 类别 | 技术 |
|---|---|
| Web 框架 | Spring Boot 2.7.15、Spring MVC |
| ORM | MyBatis 2.3.1 + MyBatis-Plus 3.5.2 |
| 代码生成 | MyBatis-Plus Generator 3.5.3.1 + FreeMarker 2.3.30 |
| 数据库 | MySQL 8.0.31，HikariCP 连接池 |
| 缓存 | Redis（Spring Data Redis） |
| 搜索 | Elasticsearch（Spring Data ES 2.7.6） |
| 日志 | Logback 1.2.11（滚动文件 + 控制台） |
| AOP | AspectJ（aspectjweaver 1.9.6） |
| 工具库 | Lombok、FastJSON 1.2.54、Apache Commons（lang3/codec/io）、Easy-Captcha 1.6.2 |
| 校验 | Spring Validation |
| 密码加密 | Spring Security Crypto 5.7.12（BCryptPasswordEncoder） |

---

## 代码放置规范

所有共享代码放在 `medium_common`，web/admin 模块只存放各自的控制器和模块专属配置。

```
medium_common/src/main/java/com/xypu/
├── entity/
│   ├── po/          # MyBatis-Plus 实体类（type-aliases-package 指向此处）
│   ├── vo/          # 视图对象，用于接口返回（不含敏感字段）
│   └── dto/         # 数据传输对象，用于接收请求体（@RequestBody）
├── mapper/          # MyBatis-Plus Mapper 接口
├── service/         # Service 接口及实现类
├── config/          # 公共 Spring 配置类（Redis、BCrypt 等）
├── utils/           # 工具类（Redis 工具、文件工具等）
├── enums/           # 枚举类（权限、状态等）
├── exception/       # 业务异常、错误码枚举、全局异常处理器
├── response/        # 统一响应体 ResponseVO
└── context/         # ThreadLocal 用户上下文 UserContext

medium_common/src/main/resources/
└── mappers/         # MyBatis XML 映射文件

medium_web/src/main/java/com/xypu/
├── controller/      # 客户端 REST 控制器
├── interceptor/     # 客户端拦截器（Token 鉴权）
└── config/          # 客户端 MVC 配置（注册拦截器）

medium_admin/src/main/java/com/xypu/
├── controller/      # 管理后台 REST 控制器
├── interceptor/     # 管理端拦截器（Session 鉴权）
└── config/          # 管理端 MVC 配置（注册拦截器）

docs/
└── postman/
    └── API_TEST_GUIDE.md   # Postman 接口测试指南（随接口同步维护）
```

---

## 本地开发配置

### 数据库（MySQL）
- URL：`jdbc:mysql://127.0.0.1:3307/bilibilimedium`
- 用户名：`root`，密码：`123456`
- HikariCP：min-idle=5，max-pool-size=10

### Redis
- 地址：`127.0.0.1:6379`，database `0`

### Elasticsearch
- 视频索引名：`video`（通过配置项 `es.index.video` 注入）

### 文件存储
- 本地上传目录：`e:/work/BiliBili_backend/`
- medium_web 最大上传：5000MB（视频文件）
- medium_admin 最大上传：10MB

### 日志
- 输出路径：`${project.folder}/logs/${spring.application.name}.log`
- 滚动策略：单文件 20MB，保留 30 天
- 默认级别：`debug`（由 `application.yaml` 中 `log.root.level` 控制）

### MyBatis-Plus
- Mapper XML 扫描：`classpath:/mappers/*.xml`（web）/ `classpath*:mappers/*.xml`（admin，跨 jar 扫描）
- 类型别名包：`com.xypu.entity.po`
- SQL 日志：`StdOutImpl`（开发环境输出到控制台）

---

## 启动类

| 模块 | 主类 |
|---|---|
| medium_web | `com.xypu.MediumwebRunApplication` |
| medium_admin | `com.xypu.MediumadminRunApplication` |

两者均使用 `@SpringBootApplication(scanBasePackages = "com.xypu")`，可自动扫描 `medium_common` 中的所有 Bean。

---

## 注意事项

1. **`medium_common` 是普通 jar**，没有 Spring Boot Maven 插件，不可独立运行，仅供 web 和 admin 模块依赖。

2. **Mapper XML 路径必须正确** — XML 文件须放在 `medium_common/src/main/resources/mappers/`。admin 模块使用 `classpath*:` 跨 jar 扫描；web 模块使用 `classpath:`，只扫描单一 classpath 根。

3. **Lombok 注解处理器** — IDE 中必须开启注解处理（Annotation Processing），否则实体类和 Service 编译失败。

4. **父 pom 中的 mainClass 残留** — `pom.xml` 里的 `<mainClass>` 标签引用了旧类名 `com.xyzy.EasylivewebRunApplication`，属于模板遗留，不影响运行时，实际主类见上表。

5. **Elasticsearch 版本固定** — `spring-boot-starter-data-elasticsearch` 在父 pom 中硬编码为 `2.7.6`，未使用 Spring Boot BOM 管理，升级时需注意。

6. **数据库 password 字段长度** — BCrypt 哈希固定 60 字符，`user_info.password` 字段须为 `varchar(100)` 以上，建表时若使用 `varchar(50)` 需执行 ALTER 扩容。
