# Postman 接口测试指南

> **维护约定**：每次新增或修改接口后，必须同步更新本文件，补充对应的请求信息、正常场景和异常场景。

---

## 一、环境变量配置

在 Postman 中创建两套 Environment，分别对应两个服务：

### BiliBili-Web（客户端）
| 变量名 | 初始值 | 说明 |
|---|---|---|
| `base_url` | `http://localhost:7071` | 客户端服务地址 |
| `token` | （空） | 登录后由脚本自动写入 |
| `checkCodeKey` | （空） | 获取验证码后由脚本自动写入 |

### BiliBili-Admin（管理端）
| 变量名 | 初始值 | 说明 |
|---|---|---|
| `base_url` | `http://localhost:7072/admin` | 管理端服务地址（含 context-path） |
| `checkCodeKey` | （空） | 获取验证码后由脚本自动写入 |

> 所有 POST 接口 Body 类型均为 `raw → JSON`，需在 Headers 中携带 `Content-Type: application/json`（Postman 选择 JSON 后自动添加）。
>
> 管理端登录态基于 Session Cookie，Postman 默认自动携带 Cookie，无需手动配置。

---

## 二、客户端接口（C端）

> 测试顺序：checkCode → register → login → autoLogin → logout

---

### 2.1 获取验证码

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/checkCode` |
| 请求参数 | 无 |

**Tests 脚本**（自动提取 checkCodeKey 存入环境变量）：
```javascript
const res = pm.response.json();
pm.test("响应状态为 success", () => pm.expect(res.status).to.eql("success"));
pm.environment.set("checkCodeKey", res.data.checkCodeKey);
console.log("验证码图片 Base64（复制到浏览器预览）：", res.data.checkCodeBase64);
```

**正常场景**：返回 `status=success`，data 包含 `checkCodeKey` 和 `checkCodeBase64`。

---

### 2.2 用户注册

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/client/register` |
| Body 类型 | `raw → JSON` |

**请求体示例**：
```json
{
  "account": "testuser01",
  "password": "Test@123456",
  "nickName": "测试用户",
  "checkCode": "3",
  "checkCodeKey": "{{checkCodeKey}}"
}
```

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("注册成功", () => pm.expect(res.status).to.eql("success"));
```

**正常场景**：返回 `code=200, status=success`。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 验证码错误 | checkCode 填写错误答案 | `code=601, status=error` |
| 账号已存在 | 重复注册同一 account | `code=602, status=error` |

---

### 2.3 用户登录

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/client/login` |
| Body 类型 | `raw → JSON` |

**请求体示例**：
```json
{
  "account": "testuser01",
  "password": "Test@123456",
  "checkCode": "3",
  "checkCodeKey": "{{checkCodeKey}}"
}
```

**Tests 脚本**（登录成功后自动保存 token）：
```javascript
const res = pm.response.json();
pm.test("登录成功", () => pm.expect(res.status).to.eql("success"));
if (res.data && res.data.token) {
    pm.environment.set("token", res.data.token);
    console.log("Token 已保存：", res.data.token);
}
```

**正常场景**：返回 `code=200`，data 包含 `token` 和 `userInfo`（无 password 字段）。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 验证码错误 | checkCode 填写错误答案 | `code=601` |
| 账号不存在 | account 填写不存在的账号 | `code=603` |
| 密码错误 | password 填写错误密码 | `code=603` |
| 账号禁用 | 手动将数据库 status=0 后登录 | `code=604` |

---

### 2.4 自动登录

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/client/autoLogin` |
| Headers | `Authorization: Bearer {{token}}` |
| Body | 无 |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("自动登录成功", () => pm.expect(res.status).to.eql("success"));
pm.test("返回用户信息不含密码", () => pm.expect(res.data).to.not.have.property("password"));
```

**正常场景**：返回 `code=200`，data 为最新用户信息，`lastLoginIp` 已更新。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 无 Token | 删除 Authorization 头 | `code=401` |
| Token 无效 | Authorization 填写随机字符串 | `code=401` |

---

### 2.5 退出登录

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/client/logout` |
| Headers | `Authorization: Bearer {{token}}` |
| Body | 无 |

**Tests 脚本**（退出后清除本地 token 变量）：
```javascript
const res = pm.response.json();
pm.test("退出成功", () => pm.expect(res.status).to.eql("success"));
pm.environment.unset("token");
```

**验证步骤**：退出后再调用 2.4（autoLogin），应返回 `code=401`，确认 Token 已失效。

---

## 三、管理端接口（B端）

> 测试顺序：checkCode → register → login → logout
>
> 管理端 Session 由 Cookie 自动维持，Postman 需确认 **Settings → Cookies** 已开启。

---

### 3.1 获取验证码

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/admin/checkCode` |
| 请求参数 | 无 |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("响应状态为 success", () => pm.expect(res.status).to.eql("success"));
pm.environment.set("checkCodeKey", res.data.checkCodeKey);
console.log("管理端验证码 Base64：", res.data.checkCodeBase64);
```

---

### 3.2 管理员注册

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/register` |
| Body 类型 | `raw → JSON` |

**请求体示例**：
```json
{
  "account": "admin01",
  "password": "Admin@123456",
  "nickName": "超级管理员",
  "checkCode": "5",
  "checkCodeKey": "{{checkCodeKey}}"
}
```

**正常场景**：返回 `code=200`，数据库中 permission=3。

**异常场景**：验证码错误 → `code=601`；账号已存在 → `code=602`。

---

### 3.3 管理员登录

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/login` |
| Body 类型 | `raw → JSON` |

**请求体示例**：
```json
{
  "account": "admin01",
  "password": "Admin@123456",
  "checkCode": "5",
  "checkCodeKey": "{{checkCodeKey}}"
}
```

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("管理员登录成功", () => pm.expect(res.status).to.eql("success"));
pm.test("返回用户信息不含密码", () => pm.expect(res.data).to.not.have.property("password"));
// Session Cookie 由 Postman 自动保存，无需手动处理
```

**正常场景**：返回 `code=200`，响应头 Set-Cookie 包含 JSESSIONID。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 验证码错误 | checkCode 填错 | `code=601` |
| 账号或密码错误 | account/password 填错 | `code=603` |
| 普通用户尝试登录管理端 | 使用 permission=1 的账号 | `code=605` |

---

### 3.4 管理员退出

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/logout` |
| Body | 无（Cookie 自动携带） |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("退出成功", () => pm.expect(res.status).to.eql("success"));
```

**验证步骤**：退出后重新调用任意受保护接口，应返回 `code=401`，确认 Session 已销毁。

---

## 四、分类管理接口

> 管理端（saveCategory / delCategory）需先完成管理员登录，Session Cookie 自动携带。
> 客户端查询接口（loadCategory / loadRootCategory / loadLastLevelCategory）为公开接口，无需 Token。

---

### 4.1 新增/更新分类（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/category/saveCategory` |
| Body 类型 | `raw → JSON` |

**新增顶级分类请求体示例**：
```json
{
  "pCategoryId": 0,
  "categoryCode": "anime",
  "categoryName": "动画",
  "icon": "/icon/anime.png",
  "background": "/bg/anime.jpg"
}
```

**更新已有分类（携带 categoryId）**：
```json
{
  "categoryId": 1,
  "pCategoryId": 0,
  "categoryCode": "anime",
  "categoryName": "动漫"
}
```

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("保存分类成功", () => pm.expect(res.status).to.eql("success"));
```

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| pCategoryId 缺失 | 请求体中去掉该字段 | `code=400（Spring Validation）` |
| 未登录 | 不携带 Session Cookie | `code=401` |

---

### 4.2 删除分类（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/category/delCategory?categoryId=1` |
| 请求参数 | `categoryId`（Query Param） |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("删除分类成功", () => pm.expect(res.status).to.eql("success"));
```

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 存在子分类 | 删除仍有子分类的父级 | `code=606` |
| 未登录 | 不携带 Session Cookie | `code=401` |

---

### 4.3 上传分类图片（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/category/uploadImage` |
| Body 类型 | `form-data` |
| 字段名 | `file`（类型选 File） |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("上传成功", () => pm.expect(res.status).to.eql("success"));
pm.test("返回完整图片URL", () => pm.expect(res.data).to.include("http://"));
console.log("图片地址：", res.data);
```

**正常场景**：返回 `code=200`，data 为完整图片 URL（如 `http://localhost:7072/admin/images/category/abc123.jpg`），可直接粘贴到浏览器地址栏预览。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 非图片文件 | 上传 `.txt`、`.pdf` 等 | `code=600` |
| 未选文件 | file 字段为空 | `code=600` |
| 未登录 | 不携带 Session Cookie | `code=401` |

---

### 4.4 批量更新分类排序（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/category/updateSort` |
| Body 类型 | `raw → JSON`（数组） |

**请求体示例**（前端拖拽完成后将当前层级的完整顺序提交）：
```json
[
  {"categoryId": 3, "sort": 1},
  {"categoryId": 1, "sort": 2},
  {"categoryId": 2, "sort": 3}
]
```

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("排序更新成功", () => pm.expect(res.status).to.eql("success"));
```

**正常场景**：返回 `code=200`，Redis 缓存自动清空，下次查询分类树将按新顺序返回。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 数组元素缺少 categoryId | 去掉某元素的 categoryId 字段 | `code=400` |
| 未登录 | 不携带 Session Cookie | `code=401` |

---

### 4.4 加载全量分类树（客户端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/category/loadCategory` |
| 请求参数 | 无 |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("返回分类树成功", () => pm.expect(res.status).to.eql("success"));
pm.test("data 为数组", () => pm.expect(res.data).to.be.an("array"));
```

**正常场景**：返回树形结构数组，每个节点包含 `children` 字段（叶子节点为空数组）。
首次调用从 DB 查询并写入 Redis；后续调用直接命中缓存。

---

### 4.4 加载顶级分类（客户端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/category/loadRootCategory` |
| 请求参数 | 无 |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("返回顶级分类成功", () => pm.expect(res.status).to.eql("success"));
pm.test("所有分类均为顶级节点", () => {
    res.data.forEach(c => pm.expect(c.pCategoryId).to.eql(0));
});
```

---

### 4.5 加载叶子分类（客户端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/category/loadLastLevelCategory` |
| 请求参数 | 无 |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("返回叶子分类成功", () => pm.expect(res.status).to.eql("success"));
pm.test("所有节点无子分类", () => {
    res.data.forEach(c => {
        pm.expect(!c.children || c.children.length === 0).to.be.true;
    });
});
```

**正常场景**：返回平铺的叶子节点列表（无 children 或 children 为空），用于视频发布时的分类选择。

---

## 五、通用注意事项

1. **POST 接口统一使用 JSON Body**：Body 类型选 `raw → JSON`，Postman 会自动添加 `Content-Type: application/json`，不要使用 `form-data` 或 `x-www-form-urlencoded`。

2. **验证码一次性**：每次使用验证码前必须重新调用 checkCode 接口，旧 checkCodeKey 使用后立即失效。

3. **客户端 Token 续期**：Token 有效期 7 天，剩余不足 1 天时拦截器自动续期，无需手动处理。

4. **管理端 Session 有效期**：30 分钟无操作后自动过期，过期后需重新登录。关闭 Postman 不等同于关闭浏览器，Session Cookie 仍然保留，需手动调用 logout 或等待超时。

5. **返回结构统一**：所有接口响应体格式为：
```json
{
  "status": "success | error",
  "code": 200,
  "info": "操作成功",
  "data": {}
}
```
