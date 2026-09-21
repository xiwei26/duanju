# 安卓客户端视频播放诊断

## 当前架构分析

### 1. 后端连接流程 ✅ 已实现
- `MainScreen.openDrama()` → 调用 `repository.getPlaybackPlan()` 
- `ApiClient` 自动添加 `X-Juku-Viewer` 和 `Sec-Fetch-Site` 请求头
- 正确处理 `redirect` 和 `proxy` 两种播放模式

### 2. 播放 URL 构建 ✅ 已实现
```kotlin
activeVideoUrl = if (plan.delivery == "redirect" && !plan.directURL.isNullOrBlank()) {
    plan.directURL  // 直连源站
} else {
    serverUrl.trimEnd('/') + plan.url  // 通过服务器代理
}
```

### 3. ExoPlayer 集成 ⚠️ 可能有问题

**问题：ExoPlayer 播放服务器代理的视频流时，缺少必要的请求头**

在 `PlayerScreen.kt` 第70-77行：
```kotlin
LaunchedEffect(videoUrl) {
    if (videoUrl.isNotBlank()) {
        val mediaItem = MediaItem.fromUri(videoUrl)  // ❌ 没有设置 HTTP 请求头
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }
}
```

### 问题根源

ExoPlayer 直接使用 `MediaItem.fromUri(videoUrl)` 播放视频时，**不会自动携带**：
- `X-Juku-Viewer` 身份令牌（后端需要此头来识别用户权限）
- `Sec-Fetch-Site` 头
- Cookie（可能需要用于会话保持）

这导致后端的 `/api/ui/playback/stream` 接口可能会：
- 返回 401/403（未授权/权限不足）
- 返回 410（会话过期）
- 无法正确追踪播放会话

### 解决方案

需要为 ExoPlayer 配置自定义的 DataSource.Factory，添加必要的 HTTP 请求头。

## 验证步骤

1. 在设置页面检查连接状态是否正常
2. 查看后端日志，确认播放请求是否到达服务器
3. 检查后端返回的 HTTP 状态码
4. 确认 `viewerId` 是否已正确设置
