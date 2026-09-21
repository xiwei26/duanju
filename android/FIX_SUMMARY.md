# Android 视频播放修复总结

## 🎯 问题描述

用户反馈：**安卓客户端填写后台地址登录后，无法观看视频**

## 🔍 根本原因

ExoPlayer 在播放通过后端代理的视频流时，没有携带后端要求的身份验证请求头：
- `X-Juku-Viewer` - 用户身份令牌
- `Sec-Fetch-Site` - 防 CSRF 验证头

导致后端无法识别用户身份，拒绝播放请求或返回 401/403 错误。

## ✅ 修复方案

### 修改的文件

1. **PlayerScreen.kt** - 播放器核心组件
   - 添加了 `DefaultHttpDataSource` 和 `DefaultMediaSourceFactory` 导入
   - 新增 `viewerId: String = ""` 参数
   - 为 ExoPlayer 配置自定义 DataSource.Factory，自动附加必要的 HTTP 请求头

2. **MainScreen.kt** - 主界面调度
   - 在调用 `PlayerScreen` 时传递 `viewerId = repository.apiClient.getViewerId()`

### 关键代码改动

**PlayerScreen.kt**：
```kotlin
// 新增导入
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

// 新增参数
fun PlayerScreen(
    // ... 其他参数
    viewerId: String = "",  // ✅ 新增
    // ...
)

// 配置自定义 HTTP DataSource
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

**MainScreen.kt**：
```kotlin
PlayerScreen(
    drama = activeDrama!!,
    chapters = activeChapters,
    currentEpisodeIndex = activeEpisodeIndex,
    videoUrl = activeVideoUrl,
    viewerId = repository.apiClient.getViewerId(),  // ✅ 新增
    danmakuList = danmakuList,
    onBack = { activeDrama = null },
    onEpisodeChange = { newIdx -> openDrama(activeDrama!!, newIdx) },
    onProgressUpdate = { pos, dur -> ... }
)
```

## 🔧 技术细节

### 完整的播放请求流程

1. **初始化阶段**：
   - App 启动 → `repository.initServerUrl()`
   - 调用 `/api/ui/viewer` → 后端返回 `viewerId`
   - `ApiClient.setViewerId(id)` 保存令牌

2. **点击播放**：
   - `MainScreen.openDrama(drama, episodeIndex)`
   - 调用 `/api/ui/dramas/refresh?id=xxx` 获取剧集详情
   - 调用 `/api/ui/playback/plan` 获取播放计划
   - 后端返回 `PlaybackPlan`（包含 url、delivery、processing 等）

3. **视频加载**：
   - `PlayerScreen` 接收 `videoUrl` 和 `viewerId`
   - ExoPlayer 使用自定义 DataSource.Factory
   - **每个视频请求自动附加**：
     - `X-Juku-Viewer: v_abc123...`
     - `Sec-Fetch-Site: same-origin`
     - `User-Agent: JukuApp/1.0`
   
4. **后端处理**：
   - 验证 `X-Juku-Viewer` 令牌
   - 检查用户权限（站源、仅在线观看等）
   - 返回视频流（原始、重封装或转码）

### 为什么之前不工作？

| 组件 | 之前的行为 | 修复后的行为 |
|------|----------|-------------|
| API 调用 | ✅ `ApiClient` 已添加 `X-Juku-Viewer` 头 | ✅ 保持不变 |
| 视频播放 | ❌ ExoPlayer 直接请求，不带任何自定义头 | ✅ ExoPlayer 使用自定义 DataSource，自动附加请求头 |
| 后端验证 | ❌ 收不到 `X-Juku-Viewer`，拒绝请求 | ✅ 验证通过，返回视频流 |

## 📋 测试建议

### 快速验证步骤

1. **重新编译 App**
   ```bash
   cd android
   ./gradlew clean assembleDebug
   # 或在 Android Studio 中 Build → Rebuild Project
   ```

2. **安装到设备**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **测试播放**
   - 打开 App → 设置页面
   - 填写服务器地址（例如 `http://192.168.1.108:8999/`）
   - 测试连接（应显示"连接正常"）
   - 返回首页 → 点击任意剧集
   - **期望**：视频正常播放 ✅

### 后端日志验证

在服务器上实时查看日志：
```bash
tail -f data/logs/app.log | grep -E "playback|X-Juku-Viewer"
```

**成功的日志示例**：
```
playback.plan: dramaId=hongguo:xxx chapterId=xxx delivery=proxy processing=original
playback.stream: session=xxx viewerId=v_abc123 episode=1
```

**失败的日志示例**（修复前）：
```
playback.unauthorized: missing X-Juku-Viewer header
playback.failed: status=401 reason=unauthorized
```

## 🎉 预期效果

修复后，用户可以：
- ✅ 正常播放所有视频
- ✅ 切换集数
- ✅ 拖动进度条
- ✅ 保存播放历史
- ✅ 使用追剧功能
- ✅ 播放不同画质的视频
- ✅ 在横屏和竖屏之间切换

## 📚 相关文档

- `android/VIDEO_PLAYBACK_FIX.md` - 详细修复说明
- `android/TESTING_CHECKLIST.md` - 完整测试清单
- `android/DIAGNOSIS.md` - 问题诊断报告
- `README.md` - 后端服务说明

## 🔄 下一步

1. **编译并测试** - 在真实设备上验证修复
2. **性能优化** - 根据测试结果优化缓冲策略
3. **错误处理** - 添加更友好的错误提示
4. **用户体验** - 添加加载指示器和断网恢复
