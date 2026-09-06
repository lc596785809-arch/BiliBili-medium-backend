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
| `uploadId` | （空） | 预上传后由脚本自动写入 |
| `videoId` | （空） | 预上传后由脚本自动写入 |
| `coverUrl` | （空） | 封面上传后由脚本自动写入 |

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

### 4.5 加载全量分类树（客户端）

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

**正常场景**：返回树形结构数组，每个节点包含 `children` 字段（叶子节点为空数组）。首次调用从 DB 查询并写入 Redis；后续调用直接命中缓存。

---

### 4.6 加载顶级分类（客户端）

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

### 4.7 加载叶子分类（客户端）

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

## 五、视频模块接口

> **C端测试顺序**：登录 → preUploadVideo → uploadVideo（逐片）→ uploadImage（封面）→ saveVideoInfo → 管理端审核 → videoResource 播放
>
> C端视频上传/提交接口需携带 `Authorization: Bearer {{token}}`；`getResource` / `videoResource` 为公开接口，无需 Token。
>
> 管理端视频接口需先完成管理员登录，Session Cookie 自动携带。

---

### 5.1 视频预上传（C端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/file/preUploadVideo?fileName=test.mp4&totalChunks=1` |
| Headers | `Authorization: Bearer {{token}}` |

**Tests 脚本**（同时保存 uploadId 和 videoId）：
```javascript
const res = pm.response.json();
pm.test("预上传成功", () => pm.expect(res.status).to.eql("success"));
pm.environment.set("uploadId", res.data.uploadId);
pm.environment.set("videoId", res.data.videoId);
console.log("uploadId:", res.data.uploadId);
console.log("videoId:", res.data.videoId);
```

**说明**：后端在 DB 预创建草稿记录（auditStatus=0）并在 Redis 缓存任务，返回 `{ uploadId, videoId }`。uploadId 用于后续分片上传，videoId 用于转码完成后调用 saveVideoInfo 提交审核。

---

### 5.2 分片上传（C端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/client/file/uploadVideo` |
| Body 类型 | `form-data` |
| Headers | `Authorization: Bearer {{token}}` |

**form-data 字段**：

| 字段名 | 类型 | 值 |
|---|---|---|
| `uploadId` | Text | `{{uploadId}}` |
| `chunkIndex` | Text | 分片序号，从 `0` 开始 |
| `file` | File | 单个分片文件 |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("分片上传成功", () => pm.expect(res.status).to.eql("success"));
```

**说明**：最后一片上传完成后，后端异步触发 FFmpeg 转码（不阻塞响应）。转码完成后 `video/{videoId}/` 目录会出现 `index.m3u8` 和 `0.ts` 等文件。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| uploadId 不存在或已过期 | 传入无效 uploadId | `code=600` |
| chunkIndex 越界 | chunkIndex ≥ totalChunks | `code=600` |
| uploadId 含非法字符 | 传入含 `../` 的 uploadId | `code=600` |

---

### 5.3 取消上传（C端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/file/delUploadVideo?uploadId={{uploadId}}&videoId={{videoId}}&coverPath={{coverPath}}` |
| Headers | `Authorization: Bearer {{token}}` |

> `videoId` 为必传参数（preUploadVideo 返回值中已包含）；`coverPath` 为可选参数，未上传封面时去掉该参数或留空。

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("取消上传成功", () => pm.expect(res.status).to.eql("success"));
pm.environment.unset("uploadId");
pm.environment.unset("videoId");
pm.environment.unset("coverPath");
```

**说明**：删除 Redis 上传任务并清理 `temp/{uploadId}/` 目录下已上传分片，释放磁盘空间。

---

### 5.4 上传视频封面（C端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/client/file/uploadImage` |
| Body 类型 | `form-data` |
| 字段名 | `file`（类型选 File） |
| Headers | `Authorization: Bearer {{token}}` |

**Tests 脚本**（同时保存封面完整 URL 和相对路径）：
```javascript
const res = pm.response.json();
pm.test("封面上传成功", () => pm.expect(res.status).to.eql("success"));
pm.environment.set("coverUrl", res.data);
// 提取相对路径（取 path= 后面的部分），取消上传时用于删除封面文件
const coverPath = res.data.split("path=")[1];
pm.environment.set("coverPath", coverPath);
console.log("封面地址：", res.data);
console.log("封面相对路径：", coverPath);
```

**正常场景**：返回完整图片 URL，格式为 `http://localhost:7071/api/v1/client/file/getResource?path=images/video/xxx.jpg`。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 非图片文件 | 上传 `.txt`、`.pdf` 等 | `code=600` |
| 未选文件 | file 字段为空 | `code=600` |

---

### 5.5 提交视频信息（C端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/client/video/saveVideoInfo` |
| Body 类型 | `raw → JSON` |
| Headers | `Authorization: Bearer {{token}}` |

**请求体示例**：
```json
{
  "videoId": "从 DB video_info 表查询获取",
  "videoName": "测试视频标题",
  "videoCover": "{{coverUrl}}",
  "pCategoryId": 1,
  "categoryId": 3,
  "tags": "测试,视频",
  "introduction": "这是一个测试视频",
  "interaction": 0,
  "isPublic": 1,
  "isVip": 0
}
```

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("提交视频信息成功", () => pm.expect(res.status).to.eql("success"));
```

**说明**：提交成功后 auditStatus 从 0（草稿）变为 1（待审核），视频出现在管理端待审核列表中。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| videoId 不属于当前用户 | 传入他人的 videoId | `code=600` |
| 必填字段缺失 | 去掉 videoName | `code=400` |

---

### 5.6 读取图片资源（公开，无需 Token）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/file/getResource?path=images/video/xxx.jpg` |
| 参数 | `path`：图片相对路径（封面上传返回的 URL 中已包含完整路径） |

**说明**：响应头包含 `Cache-Control: max-age=604800`（7 天缓存）。路径中含 `..` 或 `\` 返回 400（防目录穿越）。

---

### 5.7 读取 m3u8（HLS 播放入口，公开）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/file/videoResource/{fileId}` |
| 说明 | fileId 从 `video_info_file` 表的 `file_id` 字段获取 |

**Tests 脚本**：
```javascript
pm.test("m3u8 返回成功", () => pm.expect(pm.response.code).to.eql(200));
pm.test("Content-Type 正确", () => {
    pm.expect(pm.response.headers.get("Content-Type")).to.include("mpegurl");
});
```

**说明**：每次访问会对该视频播放计数 +1（Redis `video:play:{videoId}`）。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 视频未审核通过（auditStatus ≠ 2） | 视频处于草稿/待审核/驳回状态 | `HTTP 403` |
| 视频已逻辑删除（isDeleted=1） | 已删除的视频 | `HTTP 403` |
| 视频设为私密（isPublic=0） | 私密视频 | `HTTP 403` |
| fileId 不存在 | 传入随机 ID | `HTTP 404` |

---

### 5.8 读取 ts 分片（公开）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/client/file/videoResource/{fileId}/0.ts` |

**说明**：由播放器（如 hls.js）自动请求，通常不需要手动测试。ts 文件名格式必须为 `数字.ts`（如 `0.ts`、`1.ts`），非此格式返回 400。

---

### 5.9 分页查询视频列表（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/loadVideoList` |
| Body 类型 | `raw → JSON` |

**请求体示例**：
```json
{
  "pageNo": 1,
  "pageSize": 20,
  "videoName": "",
  "categoryId": null,
  "auditStatus": null,
  "recommendType": null,
  "isVip": null
}
```

**响应 data 结构**：
```json
{
  "records": [
    {
      "videoId": "abc123",
      "videoCover": "http://...",
      "videoName": "视频标题",
      "userId": "用户ID",
      "nickName": "上传者昵称",
      "introduction": "简介",
      "playCount": 100,
      "lastUpdateTime": "2026-01-01T00:00:00",
      "auditStatus": 2,
      "isVip": 0,
      "recommendType": 0,
      "fileId": "HLS播放凭证"
    }
  ],
  "total": 50,
  "size": 20,
  "current": 1,
  "pages": 3
}
```

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("查询成功", () => pm.expect(res.status).to.eql("success"));
pm.test("返回分页结构", () => {
    pm.expect(res.data).to.have.property("records");
    pm.expect(res.data).to.have.property("total");
});
if (res.data.records.length > 0) {
    const first = res.data.records[0];
    pm.test("记录含 nickName", () => pm.expect(first).to.have.property("nickName"));
    pm.test("记录含 fileId", () => pm.expect(first).to.have.property("fileId"));
    pm.environment.set("adminVideoId", first.videoId);
    pm.environment.set("adminFileId", first.fileId);
}
```

---

### 5.10 视频审核（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/auditVideo` |
| Body 类型 | `raw → JSON` |

**审核通过**：
```json
{
  "videoId": "abc1234567",
  "auditStatus": 2
}
```

**驳回**：
```json
{
  "videoId": "abc1234567",
  "auditStatus": 3,
  "rejectReason": "内容不符合规范"
}
```

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("审核操作成功", () => pm.expect(res.status).to.eql("success"));
```

---

### 5.11 设置推荐（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/recommendVideo?videoId=abc1234567&recommendType=1` |
| 参数 | `videoId`、`recommendType`（1=推荐 0=取消推荐） |

---

### 5.12 逻辑删除视频（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/deleteVideo?videoId=abc1234567` |

---

### 5.13 视频总数统计（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/getVideoCount` |

**Tests 脚本**：
```javascript
const res = pm.response.json();
pm.test("获取总数成功", () => pm.expect(res.status).to.eql("success"));
pm.test("data 为数字", () => pm.expect(res.data).to.be.a("number"));
```

---

### 5.14 下载完整 MP4（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/downloadVideo?videoId=abc1234567` |

**说明**：后端调用 FFmpeg 将 HLS 重封装为 MP4 后以文件流下载，下载完成后临时 MP4 自动删除。Postman 中点击 **Send and Download** 保存文件。

**异常场景**：
| 场景 | 操作 | 预期响应 |
|---|---|---|
| 视频不存在或已删除 | 传入无效 videoId | `code=600` |
| 视频尚未转码完成 | 无 video_info_file 记录 | `code=600` |

---

### 5.15 切换公开/私密状态（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/setVideoPublic?videoId=abc1234567&isPublic=0` |
| 参数 | `videoId`、`isPublic`（1=公开 0=私密） |

---

### 5.16 查询视频详情（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/getVideoDetail?videoId={{videoId}}` |

**响应 data**：VideoInfoVO，含所有视频字段 + `nickName`（上传者昵称）+ `fileId`（HLS 播放凭证）

**异常场景**：
| 场景 | 预期响应 |
|---|---|
| videoId 不存在 | `code=600` |

---

### 5.17 切换 VIP 状态（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| URL | `{{base_url}}/api/v1/admin/videoInfo/setVideoVip?videoId={{videoId}}&isVip=1` |
| 参数 | `videoId`、`isVip`（1=VIP 0=免费） |

**异常场景**：
| 场景 | 预期响应 |
|---|---|
| 视频未审核通过（auditStatus≠2） | `code=600` |

---

### 5.18 管理端播放 m3u8（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{admin_base_url}}/api/v1/admin/videoInfo/videoResource/{{adminFileId}}` |
| 认证 | Session Cookie（管理员已登录即可，浏览器自动携带） |

**说明**：返回 m3u8 索引文件内容，无审核/公开状态限制，管理员可预览任意视频（含草稿、待审核）。m3u8 中 ts 路径已改写为含 `fileId` 的相对路径，hls.js 可直接使用。

**正常场景**：响应 `Content-Type: application/vnd.apple.mpegurl`，内容为标准 m3u8 文本。

**异常场景**：
| 场景 | 预期响应 |
|---|---|
| fileId 不存在 | `HTTP 404` |
| m3u8 文件尚未生成（转码中） | `HTTP 404` |

---

### 5.19 管理端读取 ts 分片（管理端）

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| URL | `{{admin_base_url}}/api/v1/admin/videoInfo/videoResource/{{adminFileId}}/0.ts` |
| 认证 | Session Cookie |

**说明**：返回单个 ts 视频分片流，由 hls.js 自动调用，通常不需要手动测试。ts 文件名格式必须为 `数字.ts`，非此格式返回 400。

---

## 六、通用注意事项

1. **POST 接口统一使用 JSON Body**：Body 类型选 `raw → JSON`，Postman 会自动添加 `Content-Type: application/json`，不要使用 `form-data` 或 `x-www-form-urlencoded`。

2. **验证码一次性**：每次使用验证码前必须重新调用 checkCode 接口，旧 checkCodeKey 使用后立即失效。

3. **客户端 Token 续期**：Token 有效期 7 天，剩余不足 1 天时拦截器自动续期，无需手动处理。

4. **管理端 Session 有效期**：30 分钟无操作后自动过期，过期后需重新登录。关闭 Postman 不等同于关闭浏览器，Session Cookie 仍然保留，需手动调用 logout 或等待超时。

5. **视频上传流程**：preUploadVideo（同时获取 uploadId 和 videoId）→ 逐片 uploadVideo（chunkIndex 从 0 开始）→ 最后一片后等待转码完成（检查 `video/{videoId}/` 目录）→ uploadImage（上传封面）→ saveVideoInfo（提交审核）。

6. **取消上传**：调用 delUploadVideo 时必须传 `uploadId` 和 `videoId`；若已上传封面则同时传 `coverPath`（uploadImage Tests 脚本会自动提取并保存至环境变量）。

7. **返回结构统一**：所有接口响应体格式为：
```json
{
  "status": "success | error",
  "code": 200,
  "info": "操作成功",
  "data": {}
}
```
