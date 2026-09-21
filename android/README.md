# 果果剧库 - Android 原生客户端

果果剧库专用的 Android 原生客户端应用，完全基于 **Stitch** 生成的高保真 UI 原型设计与设计规范（**Cinema Noir Stream 影视黑金暗黑美学**）构建，采用现代 **Kotlin + Jetpack Compose + AndroidX Media3 (ExoPlayer)** 原生技术栈。

---

## 一、 功能亮点

1. **Stitch 高保真原生短剧体验**：
   - **剧库首页**：支持红果搜索联想、分类胶囊切换（全部、真人剧、漫剧、AI剧、榜单）、焦点爆款横幅、双列短剧海报网格。
   - **沉浸式播放器**：9:16 全屏竖屏短剧沉浸式播放体验、实时弹幕飘字、超清 1080P 画质切换、倍速切换（1.0X ~ 2.0X）、自动下一集、进度拖拽。
   - **选集抽屉**：底部半屏毛玻璃抽屉、1-30/31-60 分段分片、播放中跳动波形提示、已看/未看状态标记、一键批量缓存。
   - **追剧与历史**：双 Tab 切换、置顶“继续观看”卡片带百分比进度条与“继续播放 ▶”主按钮。
   - **下载与全集合并**：实时下载网速（⚡ MB/s）、分集进度条、独家短剧**全集智能无损合并**（将整季分集探测并极速合并为完整单部 MP4）。
   - **服务端动态连接**：支持在“我的”中随时配置与测试 Go 后端地址（例如 `http://192.168.1.108:8999` 或公网域名）。

---

## 二、 Stitch UI 设计资源

- **Stitch 项目**：`果果剧库 Android 客户端` (`projects/14963489094070225144`)
- **设计规范**：`Cinema Noir Stream` (`assets/c9558aa2d5fa40aabed83c0d883daf43`)
  - 主色调：`#FF4D4F`（活力红果暖橙红）
  - 辅助色：`#FFB800`（特权琥珀金）
  - 底色：`#0F1117` / `#111319`（深邃暗黑夜间模式）
  - 容器面：`#1A1D26` / `#242938`（暗调毛玻璃）
- **核心屏幕清单**：
  1. 剧库首页：`f4885335fa5842038298566b1d24cfa7`
  2. 沉浸式短剧播放器：`89116ee1fad74da88521d466ddc5bcb3`
  3. 选集半屏抽屉：`416f7b221f82497484192e211048b3dc`
  4. 追剧与观看历史：`c024c679c58e4551b7c61d08c5e8ee9f`
  5. 下载管理与全集合并：`31f5480c1b4949e0a01a155bdc31f757`
  6. 个人中心与服务器接入：`86dbd0d8e7144e90a087b71899ad3689`

---

## 三、 运行与编译指南

### 1. 使用 Android Studio 打开与运行
1. 打开 **Android Studio** (推荐 Hedgehog / Iguana / Ladybug 或更高版本)。
2. 选择 **Open**，定位并打开当前项目的 `android/` 目录。
3. Android Studio 将自动通过 Gradle Sync 下载依赖项。
4. 连接真实 Android 设备（开启 USB 调试）或启动 Android 模拟器。
5. 点击顶部的 **Run 'app'** 绿色三角形按钮即可安装并运行。

### 2. 连接你的果果剧库服务端
1. 启动项目根目录下的 Go 服务端：
   ```bash
   go run .
   # 默认监听 0.0.0.0:8999
   ```
2. 查看运行 Go 服务端电脑的局域网 IP（例如 `192.168.1.108` 或 `10.0.2.2` 若使用官方模拟器）。
3. 在手机 App 底部点击 **我的** -> 在 **Go 后端服务接入** 中输入：
   ```text
   http://192.168.1.108:8999
   ```
4. 点击 **测试连接**，显示绿色”已连接 · 延迟 xx ms”即表示连通成功，剧库、播放、追剧、下载全功能即可无缝使用！

---

## 四、 视频播放修复说明 (2026-09-21)

### 问题描述
早期版本中，用户填写后台地址并登录后，剧库列表正常显示，但**点击播放视频时无法正常播放**。

### 根本原因
ExoPlayer 在播放通过后端代理的视频流时，没有携带后端要求的身份验证请求头（`X-Juku-Viewer` 和 `Sec-Fetch-Site`），导致后端无法识别用户身份，返回 401/403 权限错误。

### 修复内容
✅ **已完成修复**，现在视频可以正常播放：

1. **PlayerScreen.kt** - 为 ExoPlayer 配置自定义 HTTP DataSource
   - 自动附加 `X-Juku-Viewer` 身份令牌
   - 自动附加 `Sec-Fetch-Site` 防 CSRF 验证头
   - 设置合理的连接和读取超时

2. **MainScreen.kt** - 传递用户身份信息
   - 将 `viewerId` 从后端获取后传递给播放器
   - 确保每个视频请求都能正确验证身份

### 技术细节
```kotlin
// ExoPlayer 现在使用自定义 DataSource.Factory
val httpDataSourceFactory = DefaultHttpDataSource.Factory()
    .setDefaultRequestProperties(
        mapOf(
            “X-Juku-Viewer” to viewerId,
            “Sec-Fetch-Site” to “same-origin”
        )
    )
```

### 验证方法
1. 编译并安装最新版本
2. 连接到后端服务器
3. 点击任意剧集封面
4. **期望结果**：视频正常播放，可以拖动进度条，可以切换集数

### 相关文档
- 详细修复说明: `android/VIDEO_PLAYBACK_FIX.md`
- 测试清单: `android/TESTING_CHECKLIST.md`
- 验证报告: `android/VERIFICATION_REPORT.md`
- 快速总结: `android/FIX_SUMMARY.md`

---

## 五、 开发与贡献

### 项目结构
```
android/
├── app/src/main/java/com/juku/app/
│   ├── ui/
│   │   ├── player/PlayerScreen.kt          # 播放器界面 (已修复)
│   │   ├── home/HomeScreen.kt              # 首页
│   │   ├── following/FollowingScreen.kt    # 追剧页面
│   │   ├── download/DownloadScreen.kt      # 下载管理
│   │   ├── settings/SettingsScreen.kt      # 设置页面
│   │   └── MainScreen.kt                   # 主界面 (已修复)
│   ├── data/
│   │   ├── api/ApiClient.kt                # API 客户端
│   │   ├── api/JukuApiService.kt           # API 接口定义
│   │   ├── repository/JukuRepository.kt    # 数据仓库
│   │   └── model/Models.kt                 # 数据模型
│   └── ui/theme/                           # Cinema Noir 主题
└── docs/                                   # 技术文档
```

### 技术栈
- **UI**: Jetpack Compose + Material3
- **网络**: Retrofit + OkHttp + Kotlinx Serialization
- **视频**: AndroidX Media3 (ExoPlayer)
- **异步**: Kotlin Coroutines + Flow
- **架构**: MVVM + Repository 模式

### 构建命令
```bash
# 清理构建
./gradlew clean

# 调试版本
./gradlew assembleDebug

# 发布版本
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
```

---

**维护状态**: ✅ 活跃开发中  
**最后更新**: 2026-09-21  
**主要修复**: 视频播放身份验证问题
