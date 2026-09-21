# 🚨 安卓客户端关键问题清单

最后更新：2026-09-21

---

## ✅ 已修复问题

### 1. 视频播放身份验证失败 ✅

**问题描述**：
- ExoPlayer 播放视频时未携带 `X-Juku-Viewer` 身份令牌
- 后端拒绝未授权的视频请求，返回 403 错误
- 用户无法观看任何视频内容

**修复方案**：
- ✅ 修改 `PlayerScreen.kt`：为 ExoPlayer 配置自定义 `DefaultHttpDataSource.Factory`
- ✅ 修改 `MainScreen.kt`：传递 `viewerId` 给 `PlayerScreen`
- ✅ 自动附加必要请求头：
  ```
  X-Juku-Viewer: v_abc123...
  Sec-Fetch-Site: same-origin
  User-Agent: JukuApp/1.0
  ```

**验证状态**：✅ 代码已修改，等待编译测试

**相关文件**：
- `app/src/main/java/com/juku/app/ui/player/PlayerScreen.kt`
- `app/src/main/java/com/juku/app/ui/MainScreen.kt`

---

## ⚠️ 待修复问题

### 2. 任务管理功能不完整 ⚠️

**问题描述**：
- 用户可以看到下载任务列表
- 但无法重试失败任务、取消运行任务、清除完成任务
- 后端接口已实现，但客户端未集成

**影响范围**：中等 - 用户体验受限，需手动管理任务

**建议修复**：
```kotlin
// 添加到 JukuApiService.kt
@POST("api/ui/tasks/retry")
suspend fun retryTasks(@Body request: TaskActionRequest): Response<TasksResponse>

@POST("api/ui/tasks/cancel")
suspend fun cancelTasks(@Body request: TaskActionRequest): Response<TasksResponse>

@POST("api/ui/tasks/clear")
suspend fun clearTasks(@Body request: TaskActionRequest): Response<TasksResponse>

@Serializable
data class TaskActionRequest(
    val ids: List<String> = emptyList(),
    val dramaIds: List<String> = emptyList()
)
```

**优先级**：中

---

### 3. 缺少播放历史和断点续播 ⚠️

**问题描述**：
- 客户端调用 `POST /api/ui/playback/progress` 上报进度
- 但未实现读取历史记录（`GET /api/ui/playback/history`）
- 用户无法从上次位置继续观看

**影响范围**：中等 - 用户需手动查找上次观看位置

**建议修复**：
```kotlin
// 添加到 JukuApiService.kt
@GET("api/ui/playback/history")
suspend fun getPlaybackHistory(): Response<HistoryResponse>

@Serializable
data class HistoryItem(
    val dramaId: String,
    val dramaTitle: String,
    val episode: String,
    val position: Double,
    val duration: Double,
    val updatedAt: String
)

@Serializable
data class HistoryResponse(
    val items: List<HistoryItem> = emptyList()
)
```

**优先级**：中

---

### 4. 缺少配置界面 ⚠️

**问题描述**：
- 后端有 `GET/POST /api/ui/config` 接口
- 可以查询和修改下载目录、并发数等设置
- 客户端未集成，用户无法自定义配置

**影响范围**：低 - 默认配置可用

**优先级**：低

---

### 5. 无管理员功能 ⚠️

**问题描述**：
- 后端有完整的管理员接口（账户管理、权限设置、Emby 同步等）
- 安卓客户端完全未实现
- 管理员仍需使用 Web 端

**影响范围**：低 - Web 端可替代

**优先级**：低（可选）

---

## 🎯 修复优先级总结

### P0 - 关键（已修复）
- ✅ 视频播放身份验证

### P1 - 重要（建议1-2周内修复）
- ⚠️ 任务管理功能（重试/取消/清除）
- ⚠️ 播放历史和断点续播

### P2 - 可选（按需）
- ⚠️ 配置界面
- ⚠️ 管理员功能

---

## 📋 测试检查清单

编译新版本后，请按以下顺序测试：

### 关键功能测试
- [ ] 登录成功
- [ ] 设置服务器地址
- [ ] 网络检测通过
- [ ] 浏览剧集列表
- [ ] 搜索剧集
- [ ] 点击剧集封面
- [ ] **播放视频（核心修复点）**
- [ ] 切换剧集
- [ ] 追剧功能
- [ ] 查看下载任务

### 视频播放详细测试
- [ ] 播放开始正常
- [ ] 视频流畅无卡顿
- [ ] 可以拖动进度条
- [ ] 切换清晰度（如果支持）
- [ ] 全屏播放正常
- [ ] 播放完成后自动跳转下一集
- [ ] 检查 Logcat 无 403 错误

### 边界情况测试
- [ ] 网络中断后恢复播放
- [ ] App 切到后台再恢复
- [ ] 多个剧集快速切换
- [ ] 长时间播放（> 30分钟）

---

## 🔍 调试指南

如果视频播放仍有问题，检查以下日志：

### Android Logcat
```bash
adb logcat | grep -E "PlayerScreen|ExoPlayer|HttpDataSource|X-Juku-Viewer"
```

### 后端日志
查看 Go 服务端输出，搜索：
- `403` 错误
- `X-Juku-Viewer` 缺失
- `viewer not found`

### 网络抓包
使用 Charles/Fiddler 检查：
- 视频请求是否包含 `X-Juku-Viewer` 请求头
- 请求头值是否正确（格式：`v_xxx`）
- 响应码（应为 200，不是 403）

---

**下一步**：编译并测试修复后的客户端，验证视频播放功能是否正常。
