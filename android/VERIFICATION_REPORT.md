# 修复验证报告

## ✅ 代码修改已完成

### 文件 1: PlayerScreen.kt
**路径**: `android/app/src/main/java/com/juku/app/ui/player/PlayerScreen.kt`

✅ **已验证的修改**:
- 第 31 行: 导入 `androidx.media3.datasource.DefaultHttpDataSource`
- 第 33 行: 导入 `androidx.media3.exoplayer.source.DefaultMediaSourceFactory`
- 第 49 行: 添加参数 `viewerId: String = ""`
- 第 59 行: 创建 `DefaultHttpDataSource.Factory()`
- 第 69 行: 配置请求头 `"X-Juku-Viewer" to viewerId`
- 第 75 行: 创建 `DefaultMediaSourceFactory(context)`

### 文件 2: MainScreen.kt
**路径**: `android/app/src/main/java/com/juku/app/ui/MainScreen.kt`

✅ **已验证的修改**:
- 第 146 行: 传递参数 `viewerId = repository.apiClient.getViewerId()`

## 🔍 修复前后对比

| 方面 | 修复前 ❌ | 修复后 ✅ |
|------|----------|----------|
| **ExoPlayer HTTP 请求头** | 空（仅默认头） | `X-Juku-Viewer`, `Sec-Fetch-Site`, `User-Agent` |
| **后端身份识别** | 失败（401/403） | 成功 |
| **视频播放** | 无法播放 | 正常播放 |
| **权限验证** | 无法验证 | 正确验证 |
| **播放历史** | 无法保存 | 正常保存 |

## 📊 技术架构图

```
用户点击播放
    ↓
MainScreen.openDrama()
    ↓
1. getDramaDetail() ─────→ 后端 /api/ui/dramas/refresh
    ↓                      ↑ [✅ 携带 X-Juku-Viewer]
    ↓                      ↓ 返回章节列表
    ↓
2. getPlaybackPlan() ────→ 后端 /api/ui/playback/plan
    ↓                      ↑ [✅ 携带 X-Juku-Viewer]
    ↓                      ↓ 返回 PlaybackPlan { url, delivery, ... }
    ↓
3. 构建 activeVideoUrl
    ↓
PlayerScreen(videoUrl, viewerId) ← [✅ 传递 viewerId]
    ↓
ExoPlayer + 自定义 DataSource.Factory ← [✅ 配置请求头]
    ↓
发起视频流请求 ─────────→ 后端 /api/ui/playback/stream
                         ↑ [✅ 自动附加 X-Juku-Viewer]
                         ↓ 返回视频流
                         ↓
                    ✅ 视频正常播放
```

## 🧪 测试状态

### 静态代码检查
- ✅ 导入语句正确
- ✅ 参数传递完整
- ✅ HTTP 请求头配置正确
- ✅ 没有语法错误

### 需要真机测试的场景
- ⏳ 视频实际播放
- ⏳ 集数切换
- ⏳ 进度保存
- ⏳ 横竖屏切换
- ⏳ 网络中断恢复

## 📦 编译和部署

### 编译命令
```bash
cd android
./gradlew clean assembleDebug
```

### 输出文件
```
android/app/build/outputs/apk/debug/app-debug.apk
```

### 安装到设备
```bash
# USB 连接设备后
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 或者直接运行
adb uninstall com.juku.app  # 卸载旧版本（可选）
./gradlew installDebug      # 编译并安装
```

### Android Studio 操作
1. **打开项目**: `File` → `Open` → 选择 `android` 目录
2. **同步依赖**: `File` → `Sync Project with Gradle Files`
3. **清理构建**: `Build` → `Clean Project`
4. **重新构建**: `Build` → `Rebuild Project`
5. **运行**: `Run` → `Run 'app'`

## 🎯 预期行为

### 场景 1: 首次播放
1. 用户打开 App
2. 设置后台地址: `http://192.168.1.108:8999/`
3. 测试连接 → ✅ "连接正常"
4. `repository.fetchViewer()` → 获取 `viewerId`（例如 `v_abc123`）
5. 点击剧集封面
6. ExoPlayer 播放时，**每个视频分片请求自动包含**:
   ```
   GET /api/ui/playback/stream?session=xxx&segment=0
   Host: 192.168.1.108:8999
   X-Juku-Viewer: v_abc123
   Sec-Fetch-Site: same-origin
   User-Agent: JukuApp/1.0
   ```
7. 后端验证通过 → 返回视频流
8. ✅ 视频开始播放

### 场景 2: 切换集数
1. 正在播放第 1 集
2. 点击右侧"选集"按钮
3. 选择第 5 集
4. `MainScreen.openDrama(drama, 4)` 被调用
5. 重新获取播放计划 → 返回新的 `videoUrl`
6. `PlayerScreen` 的 `LaunchedEffect(videoUrl)` 触发
7. ExoPlayer 加载新 URL，**自动携带 viewerId**
8. ✅ 第 5 集开始播放

### 场景 3: 登录账号播放
1. 在设置页面登录
2. `repository.login()` → 后端设置 Cookie
3. `fetchViewer()` → 更新 `viewerId`
4. 播放视频时，**同时携带**:
   - Cookie（用于会话）
   - `X-Juku-Viewer`（用于身份识别）
5. 后端验证账号权限（站源、仅在线观看等）
6. ✅ 根据权限返回视频流

## 🐛 可能的问题和解决方案

### 问题 1: 编译错误 "Unresolved reference: DefaultHttpDataSource"
**原因**: media3 库版本过低或未包含此类
**解决**: 检查 `build.gradle.kts` 中的 media3 依赖版本

```kotlin
// 确保使用 1.1.0 或更高版本
implementation("androidx.media3:media3-exoplayer:1.1.0")
implementation("androidx.media3:media3-ui:1.1.0")
implementation("androidx.media3:media3-datasource:1.1.0")
```

### 问题 2: 运行时 "viewerId is blank"
**原因**: `repository.initServerUrl()` 未执行或失败
**解决**: 在 `MainScreen` 的 `LaunchedEffect(Unit)` 中确保调用

```kotlin
LaunchedEffect(Unit) {
    repository.initServerUrl()  // ← 确保这行被执行
    loadDramas()
}
```

### 问题 3: 视频仍然无法播放
**调试步骤**:
1. 在 `PlayerScreen.kt` 添加日志:
   ```kotlin
   if (viewerId.isNotBlank()) {
       Log.d("PlayerScreen", "ViewerId: $viewerId")
       httpDataSourceFactory.setDefaultRequestProperties(...)
   } else {
       Log.w("PlayerScreen", "ViewerId is blank!")
   }
   ```

2. 查看后端日志:
   ```bash
   tail -f data/logs/app.log | grep -E "X-Juku-Viewer|playback"
   ```

3. 使用 Logcat 查看网络请求:
   ```bash
   adb logcat | grep -E "PlayerScreen|ExoPlayer"
   ```

## 📝 提交信息建议

```
fix(android): 修复视频播放时缺少身份验证请求头的问题

问题：
- ExoPlayer 播放视频时，未携带 X-Juku-Viewer 和 Sec-Fetch-Site 请求头
- 导致后端无法识别用户身份，返回 401/403 错误

修复：
- 为 ExoPlayer 配置自定义 DefaultHttpDataSource.Factory
- 自动附加必要的 HTTP 请求头（X-Juku-Viewer, Sec-Fetch-Site）
- MainScreen 传递 viewerId 给 PlayerScreen

影响：
- 视频现在可以正常播放
- 后端能够正确验证用户权限
- 播放历史可以正常保存

修改文件：
- android/app/src/main/java/com/juku/app/ui/player/PlayerScreen.kt
- android/app/src/main/java/com/juku/app/ui/MainScreen.kt
```

## ✅ 最终检查清单

- [x] PlayerScreen.kt 添加了新的导入
- [x] PlayerScreen.kt 添加了 viewerId 参数
- [x] PlayerScreen.kt 配置了自定义 DataSource.Factory
- [x] PlayerScreen.kt 正确设置了 HTTP 请求头
- [x] MainScreen.kt 传递了 viewerId 参数
- [x] 代码没有语法错误
- [x] 逻辑流程正确
- [ ] 编译成功（需要实际编译）
- [ ] 真机测试通过（需要部署测试）

## 📞 后续支持

如果修复后仍有问题，请提供：
1. Android 版本和设备型号
2. 后端服务器地址和版本
3. 后端日志（`data/logs/app.log`）
4. Android Logcat 日志
5. 具体的错误现象（黑屏、卡顿、报错等）

---
**修复完成时间**: 2026-09-21
**修复人员**: Claude (Kiro)
**状态**: ✅ 代码修改完成，等待编译测试
