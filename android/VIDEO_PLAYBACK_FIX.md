# 安卓客户端视频播放修复

## 问题诊断

**症状**：登录后台地址后，剧库列表正常显示，但点击播放视频时无法播放。

**根本原因**：ExoPlayer 播放视频时，没有携带后端要求的身份验证请求头（`X-Juku-Viewer` 和 `Sec-Fetch-Site`），导致后端拒绝播放请求或无法正确识别用户权限。

## 修复内容

### 1. PlayerScreen.kt - 添加自定义 HTTP 请求头

**文件**：`android/app/src/main/java/com/juku/app/ui/player/PlayerScreen.kt`

**修改**：
- 新增导入：`DefaultHttpDataSource` 和 `DefaultMediaSourceFactory`
- 添加 `viewerId: String = ""` 参数
- 为 ExoPlayer 配置自定义 DataSource.Factory，自动添加必要的 HTTP 请求头

**关键代码**：
```kotlin
val httpDataSourceFactory = DefaultHttpDataSource.Factory()
    .setUserAgent("JukuApp/1.0")
    .setConnectTimeoutMs(15000)
    .setReadTimeoutMs(20000)
    .setAllowCrossProtocolRedirects(true)

if (viewerId.isNotBlank()) {
    httpDataSourceFactory.setDefaultRequestProperties(
        mapOf(
            "X-Juku-Viewer" to viewerId,
            "Sec-Fetch-Site" to "same-origin"
        )
    )
}

val mediaSourceFactory = DefaultMediaSourceFactory(context)
    .setDataSourceFactory(httpDataSourceFactory)

ExoPlayer.Builder(context)
    .setMediaSourceFactory(mediaSourceFactory)
    .build()
```

### 2. MainScreen.kt - 传递 viewerId

**文件**：`android/app/src/main/java/com/juku/app/ui/MainScreen.kt`

**修改**：
- 在调用 `PlayerScreen` 时，传递 `viewerId = repository.apiClient.getViewerId()`

**关键代码**：
```kotlin
PlayerScreen(
    drama = activeDrama!!,
    chapters = activeChapters,
    currentEpisodeIndex = activeEpisodeIndex,
    videoUrl = activeVideoUrl,
    viewerId = repository.apiClient.getViewerId(), // ✅ 新增
    danmakuList = danmakuList,
    onBack = { activeDrama = null },
    onEpisodeChange = { newIdx -> openDrama(activeDrama!!, newIdx) },
    onProgressUpdate = { pos, dur -> ... }
)
```

## 工作原理

### 完整播放流程

1. **用户点击剧集** → `MainScreen.openDrama()`
2. **获取剧集详情** → `repository.getDramaDetail(dramaId)`
3. **获取播放计划** → `repository.getPlaybackPlan(dramaId, chapterId, episodeIndex)`
   - 后端返回 `PlaybackPlan`，包含：
     - `url`: 播放地址（相对路径或完整URL）
     - `delivery`: 传输方式（`proxy`/`redirect`/`local`）
     - `directURL`: 直连地址（仅 `redirect` 模式）
4. **构建播放 URL**：
   ```kotlin
   activeVideoUrl = if (plan.delivery == "redirect" && !plan.directURL.isNullOrBlank()) {
       plan.directURL  // 直连源站
   } else {
       serverUrl.trimEnd('/') + plan.url  // 通过服务器代理
   }
   ```
5. **ExoPlayer 播放**：
   - 使用自定义 DataSource.Factory
   - 自动附加 `X-Juku-Viewer` 令牌
   - 自动附加 `Sec-Fetch-Site: same-origin`
   - 后端识别用户身份，返回视频流

### 请求头说明

| 请求头 | 值 | 用途 |
|--------|-----|------|
| `X-Juku-Viewer` | viewerId（如 `v_abc123...`） | 后端用于识别用户身份和权限 |
| `Sec-Fetch-Site` | `same-origin` | 防止 CSRF 攻击，后端验证请求来源 |
| `User-Agent` | `JukuApp/1.0` | 标识客户端类型 |

## 测试步骤

### 1. 前置条件
- 确保后端服务（Go 服务器）已启动并可访问
- 后端地址示例：`http://192.168.1.108:8999/`

### 2. 测试流程
1. **启动 App**
2. **进入设置页面**（底部导航第4个）
3. **填写服务器地址**，点击"测试连接"
   - 应显示：`连接正常 · 延迟 XXms`
4. **可选：登录账号**（如果后端开启了登录要求）
5. **返回首页**（底部导航第1个）
6. **点击任意剧集封面**
7. **验证播放**：
   - ✅ 视频正常加载并播放
   - ✅ 进度条显示正确
   - ✅ 可以拖动进度条
   - ✅ 可以切换到下一集
   - ✅ 右侧选集按钮可以切换任意集

### 3. 故障排查

#### 如果仍然无法播放：

**检查后端日志**（在服务器控制台或 `data/logs/app.log`）：
```bash
# 查看最近100行日志
tail -n 100 data/logs/app.log
```

**关键日志字段**：
- `playback.plan` - 播放计划生成
- `playback.failed` - 播放失败
- `media.failed` - 媒体连接失败
- `ffmpeg.failed` - FFmpeg 处理失败

**常见错误及解决方法**：

| 错误代码 | 原因 | 解决方法 |
|---------|------|----------|
| 401 Unauthorized | 未登录或 viewerId 缺失 | 确保 `repository.fetchViewer()` 已执行 |
| 403 Forbidden | 权限不足 | 检查账号权限（站源、仅在线观看设置） |
| 410 Gone | 播放会话过期 | 重新打开播放器 |
| 502/504 | 后端无法连接源站 | 检查后端网络和代理设置 |

**调试命令**：
```bash
# 检查后端是否可访问
curl -v http://192.168.1.108:8999/api/ui/viewer

# 检查播放接口（需要替换实际参数）
curl -v -H "X-Juku-Viewer: v_xxx" \
  "http://192.168.1.108:8999/api/ui/playback/plan" \
  -d '{"dramaId":"hongguo:xxx","chapterId":"xxx","episodeIndex":1}'
```

## 技术细节

### 为什么需要自定义请求头？

1. **后端会话管理**：后端通过 `X-Juku-Viewer` 令牌来：
   - 识别匿名用户或登录账号
   - 检查用户的站源权限
   - 检查是否为"仅在线观看"账号
   - 保存播放历史和续播进度

2. **安全验证**：后端检查 `Sec-Fetch-Site` 防止：
   - CSRF 攻击
   - 未授权的外部访问
   - 播放链接被直接分享

3. **会话保活**：播放期间需要定期发送心跳，维持后端的播放会话：
   - 每 20 秒保活一次
   - 无活动 10 分钟后释放资源
   - 前台恢复时自动重连

### ExoPlayer DataSource 架构

```
ExoPlayer
  └─> MediaSourceFactory
       └─> DataSource.Factory (DefaultHttpDataSource.Factory)
            ├─> setUserAgent()
            ├─> setConnectTimeout()
            ├─> setReadTimeout()
            ├─> setAllowCrossProtocolRedirects()
            └─> setDefaultRequestProperties() ← 在这里添加自定义请求头
                 ├─> X-Juku-Viewer
                 └─> Sec-Fetch-Site
```

## 相关文件

- `android/app/src/main/java/com/juku/app/ui/player/PlayerScreen.kt` - 播放器界面
- `android/app/src/main/java/com/juku/app/ui/MainScreen.kt` - 主界面和播放调度
- `android/app/src/main/java/com/juku/app/data/api/ApiClient.kt` - API 客户端（自动添加请求头）
- `android/app/src/main/java/com/juku/app/data/api/JukuApiService.kt` - API 接口定义
- `android/app/src/main/java/com/juku/app/data/repository/JukuRepository.kt` - 数据仓库

## 后续优化建议

1. **添加播放错误处理**：
   - 捕获 ExoPlayer 的播放错误
   - 显示友好的错误提示
   - 提供重试按钮

2. **添加加载指示器**：
   - 显示缓冲状态
   - 显示缓存百分比

3. **支持断点续播**：
   - 从后端获取上次播放位置
   - 自动跳转到续播点

4. **优化切集体验**：
   - 切集时显示加载动画
   - 预加载下一集

5. **添加播放质量选择**：
   - 支持切换画质（720p/1080p）
   - 根据网络自动调整

## 版本信息

- 修复日期：2026-09-21
- 影响范围：Android 客户端视频播放功能
- 后端兼容性：果果剧库 Go 服务端（所有版本）
