# BiliBili_backend 前后端对接接口文档

## 通用约定

### 服务地址

| 端 | Base URL |
|---|---|
| C端（客户端） | `http://localhost:7071` |
| B端（管理后台） | `http://localhost:7072/admin` |

### 认证方式

**C端**：登录后服务端返回 Token，每次请求在 Header 中携带：
```
Authorization: Bearer <token>
```

**B端**：登录后 Session Cookie 由浏览器自动携带，无需额外处理。

### 统一响应格式

```json
{
  "status": "success",
  "code": 200,
  "info": "操作成功",
  "data": {}
}
```

| 字段 | 说明 |
|---|---|
| `status` | `"success"` 或 `"error"` |
| `code` | 200 表示成功，其余见错误码表 |
| `info` | 提示信息 |
| `data` | 业务数据，失败时为 `null` |

### 错误码表

| code | 说明 |
|---|---|
| 401 | 未登录或会话已过期 |
| 500 | 服务器内部错误 |
| 600 | 请求参数错误 |
| 601 | 验证码错误或已过期 |
| 602 | 账号已存在 |
| 603 | 账号或密码错误 |
| 604 | 账号已禁用 |
| 605 | 无后台管理访问权限 |
| 606 | 该分类下存在子分类，无法删除 |

---

## 一、C端接口（port 7071）

### 1.1 用户认证

#### 获取验证码

```
GET /api/v1/client/checkCode
```

无需认证，返回验证码图片 Base64 和 checkCodeKey。

**响应 data**：
```json
{
  "checkCode": "data:image/png;base64,xxx",
  "checkCodeKey": "uuid字符串"
}
```

---

#### 用户注册

```
POST /api/v1/client/register
Content-Type: application/json
```

**请求体**：
```json
{
  "nickName": "昵称",
  "account": "账号",
  "password": "密码（明文，后端加密存储）",
  "checkCode": "验证码",
  "checkCodeKey": "验证码Key"
}
```

**响应 data**：`null`（注册成功即 status=success）

---

#### 用户登录

```
POST /api/v1/client/login
Content-Type: application/json
```

**请求体**：
```json
{
  "account": "账号",
  "password": "密码",
  "checkCode": "验证码",
  "checkCodeKey": "验证码Key"
}
```

**响应 data**：
```json
{
  "token": "Bearer Token字符串",
  "userId": "用户ID",
  "nickName": "昵称",
  "avatar": "头像路径"
}
```

> Token 有效期 7 天，剩余不足 1 天时接口自动续期，新 Token 在响应 Header `Authorization` 中返回，前端需更新本地存储。

---

#### 自动登录

```
POST /api/v1/client/autoLogin
Authorization: Bearer <token>
```

无请求体。用于页面刷新时静默校验 Token 是否有效并续期。

**响应 data**：同登录接口

---

#### 退出登录

```
POST /api/v1/client/logout
Authorization: Bearer <token>
```

无请求体。销毁服务端 Token。

**响应 data**：`null`

---

### 1.2 分类查询（无需 Token）

#### 加载全量分类树

```
GET /api/v1/client/category/loadCategory
```

**响应 data**：树形结构列表
```json
[
  {
    "categoryId": 1,
    "categoryName": "游戏",
    "icon": "图标URL",
    "background": "背景图URL",
    "sort": 1,
    "children": [
      {
        "categoryId": 10,
        "categoryName": "手游",
        "sort": 1,
        "children": []
      }
    ]
  }
]
```

---

#### 加载顶级分类

```
GET /api/v1/client/category/loadRootCategory
```

**响应 data**：仅包含一级分类的列表（无 children）

---

#### 加载叶子分类

```
GET /api/v1/client/category/loadLastLevelCategory
```

**响应 data**：所有末级分类（无子节点）的平铺列表，用于视频发布时选择分类

---

### 1.3 视频上传

#### 预上传（获取上传凭证）

```
GET /api/v1/client/file/preUploadVideo
Authorization: Bearer <token>
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `fileName` | String | 是 | 视频文件名（含扩展名） |
| `totalChunks` | Integer | 是 | 分片总数，前端将文件切分的段数 |

**响应 data**：
```json
{
  "uploadId": "分片上传凭证",
  "videoId": "视频ID（提交信息时使用）"
}
```

> 前端拿到 `videoId` 后需保存，视频提交审核（saveVideoInfo）时使用。

---

#### 分片上传

```
POST /api/v1/client/file/uploadVideo
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**请求体（form-data）**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `uploadId` | String | 是 | 预上传返回的 uploadId |
| `chunkIndex` | Integer | 是 | 当前分片序号，从 0 开始 |
| `file` | File | 是 | 当前分片的二进制数据 |

**响应 data**：`null`

> 最后一片（`chunkIndex == totalChunks - 1`）上传完成后，后端自动触发异步转码。转码期间 `audit_status=0`，前端可轮询视频状态。

---

#### 取消上传

```
GET /api/v1/client/file/delUploadVideo
Authorization: Bearer <token>
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `uploadId` | String | 是 | 上传凭证 |
| `videoId` | String | 是 | 预上传返回的 videoId |
| `coverPath` | String | 否 | 已上传封面的相对路径（如 `images/video/xxx.png`），有则传，用于删除封面文件 |

**响应 data**：`null`

---

#### 上传视频封面

```
POST /api/v1/client/file/uploadImage
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**请求体（form-data）**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | File | 是 | 图片文件，支持 jpg/jpeg/png/gif/webp |

**响应 data**：封面完整访问 URL，格式如下：
```
http://localhost:7071/api/v1/client/file/getResource?path=images/video/xxx.jpg
```

> 前端需保存该 URL 用于 `saveVideoInfo`；同时从 URL 中提取 `path=` 后的相对路径（如 `images/video/xxx.jpg`），取消上传时作为 `coverPath` 传入。

---

#### 读取图片资源（无需 Token）

```
GET /api/v1/client/file/getResource?path=images/video/xxx.jpg
```

直接返回图片二进制流，设置浏览器长缓存（7天）。

---

#### 播放 HLS 视频（无需 Token）

```
GET /api/v1/client/file/videoResource/{fileId}
```

返回 m3u8 索引文件内容，供 hls.js 等播放器加载。

**可见条件**：视频必须满足 `audit_status=2`（审核通过）、`is_public=1`（公开）、`is_deleted=0`，否则返回 403。

> 每次访问 m3u8 会自动计入播放次数（Redis 计数）。

---

#### 获取 TS 分片（无需 Token）

```
GET /api/v1/client/file/videoResource/{fileId}/{tsName}
```

返回单个 ts 视频分片流，由 hls.js 自动调用，前端无需手动请求。

---

### 1.4 视频投稿

#### 提交视频信息

```
POST /api/v1/client/video/saveVideoInfo
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**：
```json
{
  "videoId": "预上传返回的 videoId（必填）",
  "videoName": "视频标题（必填）",
  "videoCover": "封面完整URL（必填）",
  "pCategoryId": 1,
  "categoryId": 10,
  "tags": "标签1,标签2",
  "introduction": "视频简介",
  "interaction": 0,
  "isPublic": 1,
  "isVip": 0
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `videoId` | String | 是 | 预上传返回的视频ID |
| `videoName` | String | 是 | 视频标题 |
| `videoCover` | String | 是 | 封面完整URL |
| `pCategoryId` | Integer | 是 | 一级分类ID |
| `categoryId` | Integer | 否 | 二级分类ID |
| `tags` | String | 否 | 标签，多个用逗号分隔 |
| `introduction` | String | 否 | 视频简介 |
| `interaction` | Integer | 否 | 互动设置：0=全开 1=关评论 2=关弹幕 |
| `isPublic` | Integer | 否 | 0=私密 1=公开（默认1） |
| `isVip` | Integer | 否 | 0=免费 1=VIP专属（默认0） |

**响应 data**：`null`

> 提交后 `audit_status` 从 0（草稿）变为 1（待审核），等待管理端审核。

---

## 二、B端接口（port 7072，context-path /admin）

### 2.1 管理员认证

#### 获取验证码

```
GET /api/v1/admin/checkCode
```

响应格式同 C 端验证码接口。

---

#### 管理员登录

```
POST /api/v1/admin/login
Content-Type: application/json
```

**请求体**：
```json
{
  "account": "管理员账号",
  "password": "密码",
  "checkCode": "验证码",
  "checkCodeKey": "验证码Key"
}
```

**响应 data**：
```json
{
  "userId": "管理员ID",
  "nickName": "昵称",
  "account": "账号"
}
```

> Session 30 分钟无操作后过期，过期后需重新登录。

---

#### 管理员退出

```
POST /api/v1/admin/logout
```

销毁 Session，响应 data 为 `null`。

---

### 2.2 分类管理

#### 新增 / 更新分类

```
POST /api/v1/admin/category/saveCategory
Content-Type: application/json
```

**请求体**：
```json
{
  "categoryId": null,
  "categoryName": "分类名称",
  "parentId": 0,
  "icon": "图标URL（可选）",
  "background": "背景图URL（可选）",
  "sort": 1
}
```

> `categoryId` 为 `null` 时新增，有值时更新。`parentId=0` 表示顶级分类。

---

#### 删除分类

```
POST /api/v1/admin/category/delCategory
Content-Type: application/json
```

**请求体**：
```json
{
  "categoryId": 10
}
```

> 若该分类下存在子分类，返回 `code=606` 拒绝删除。

---

#### 批量更新排序

```
POST /api/v1/admin/category/updateSort
Content-Type: application/json
```

**请求体**：分类列表，每项含 `categoryId` 和新 `sort` 值
```json
[
  { "categoryId": 1, "sort": 1 },
  { "categoryId": 2, "sort": 2 }
]
```

---

#### 上传分类图片

```
POST /api/v1/admin/category/uploadImage
Content-Type: multipart/form-data
```

**请求体（form-data）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `file` | File | 图片文件 |
| `type` | String | `"icon"` 或 `"background"` |

**响应 data**：图片访问 URL

---

### 2.3 视频管理

#### 分页查询视频列表

```
POST /api/v1/admin/videoInfo/loadVideoList
Content-Type: application/json
```

**请求体**：
```json
{
  "videoName": "关键词（模糊查询，可不传）",
  "categoryId": null,
  "auditStatus": null,
  "recommendType": null,
  "isVip": null,
  "pageNo": 1,
  "pageSize": 20
}
```

| 字段 | 说明 |
|---|---|
| `videoName` | 视频名称模糊搜索，不传查全部 |
| `categoryId` | 末级分类ID筛选，不传查全部 |
| `auditStatus` | 0=草稿 1=待审核 2=已通过 3=已驳回，不传查全部 |
| `recommendType` | 0=未推荐 1=已推荐，不传查全部 |
| `isVip` | 0=免费 1=VIP，不传查全部 |

**响应 data**：MyBatis-Plus 分页对象，每条记录含以下字段：
```json
{
  "records": [
    {
      "videoId": "...",
      "videoCover": "封面URL",
      "videoName": "视频名称",
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
  "total": 100,
  "size": 20,
  "current": 1,
  "pages": 5
}
```

---

#### 视频审核

```
POST /api/v1/admin/videoInfo/auditVideo
Content-Type: application/json
```

**请求体**：
```json
{
  "videoId": "视频ID",
  "auditStatus": 2,
  "rejectReason": "驳回原因（驳回时必填）"
}
```

| `auditStatus` | 含义 |
|---|---|
| 2 | 审核通过 |
| 3 | 驳回 |

---

#### 设置推荐

```
POST /api/v1/admin/videoInfo/recommendVideo?videoId=xxx&recommendType=1
```

| 参数 | 说明 |
|---|---|
| `videoId` | 视频ID |
| `recommendType` | 0=取消推荐，1=设置推荐 |

---

#### 逻辑删除视频

```
POST /api/v1/admin/videoInfo/deleteVideo?videoId=xxx
```

逻辑删除（`is_deleted=1`），前台不再展示，数据库保留记录。

---

#### 统计视频总数

```
GET /api/v1/admin/videoInfo/getVideoCount
```

**响应 data**：未删除视频总数（Long）

---

#### 下载完整 MP4

```
GET /api/v1/admin/videoInfo/downloadVideo?videoId=xxx
```

服务端将 HLS 转封装为 MP4 后以文件流返回，浏览器直接触发下载。响应 Header：
```
Content-Disposition: attachment; filename*=UTF-8''视频标题.mp4
```

---

#### 切换公开/私密

```
POST /api/v1/admin/videoInfo/setVideoPublic?videoId=xxx&isPublic=1
```

| 参数 | 说明 |
|---|---|
| `isPublic` | 0=私密，1=公开 |

---

#### 查询视频详情

```
GET /api/v1/admin/videoInfo/getVideoDetail?videoId=xxx
```

返回单条视频完整信息，含 `nickName`（上传者昵称）和 `fileId`（用于 HLS 播放），供详情弹窗使用。

**响应 data**：VideoInfoVO（字段同列表接口单条记录）

---

#### 切换 VIP 状态

```
POST /api/v1/admin/videoInfo/setVideoVip?videoId=xxx&isVip=1
```

| 参数 | 说明 |
|---|---|
| `isVip` | 0=免费，1=VIP专属 |

> 仅 `auditStatus=2`（审核通过）的视频可操作，否则返回 `code=600`。

---

#### 管理端播放 HLS 视频（m3u8）

```
GET /api/v1/admin/videoInfo/videoResource/{fileId}
```

返回 m3u8 索引文件内容，无审核/公开状态限制，管理员可预览任意视频。需携带 Session Cookie（已登录即可）。

---

#### 管理端获取 TS 分片

```
GET /api/v1/admin/videoInfo/videoResource/{fileId}/{tsName}
```

返回单个 ts 视频分片流，由 hls.js 自动调用，前端无需手动请求。

---

## 三、视频完整上传流程

```
1. 预上传
   GET /api/v1/client/file/preUploadVideo?fileName=xxx.mp4&totalChunks=N
   → 拿到 uploadId 和 videoId

2. 分片上传（循环 N 次）
   POST /api/v1/client/file/uploadVideo
   body: { uploadId, chunkIndex: 0~N-1, file: 当前分片 }
   → 最后一片触发后端异步转码

3. 上传封面
   POST /api/v1/client/file/uploadImage
   → 拿到封面 URL 和相对路径

4. 提交视频信息（转码完成后）
   POST /api/v1/client/video/saveVideoInfo
   body: { videoId, videoName, videoCover, pCategoryId, ... }
   → audit_status 变为 1（待审核）

5. 管理端审核通过后，视频可公开播放
   GET /api/v1/client/file/videoResource/{fileId}
```

> 如需取消上传，任意阶段均可调用：
> `GET /api/v1/client/file/delUploadVideo?uploadId=xxx&videoId=xxx&coverPath=xxx`
