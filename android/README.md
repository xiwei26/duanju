# 果果剧库 Android 原生客户端

`android/` 是果果剧库 Go 服务端的原生 Android 客户端，使用 Kotlin、Jetpack Compose、Retrofit/OkHttp 和 AndroidX Media3（ExoPlayer）。它直接调用仓库中的 `/api/ui/*` 接口，不包含独立服务端；使用前必须先启动根目录的 Go 程序。

## 当前可用功能

- 读取剧库、按分类筛选、搜索和手动刷新。
- 可在剧库和播放器中加入或取消追剧，显示追剧清单与观看历史，并从服务端记录的位置续播。
- 创建播放会话、加载分集、拖动进度、暂停、切集和切换 `1.0x`、`1.25x`、`1.5x`、`2.0x` 倍速。
- 显示服务端下载任务、进度、速度和错误；可为已有的已完成分集提交合并任务。
- 支持访客身份、账号登录、退出和修改密码；首次管理员登录需要强制改密时，客户端会显示专用入口。
- 可在“我的”中保存并测试服务端地址。

## 连接服务端

先在仓库根目录启动服务端：

```powershell
go run .
```

服务端默认监听 `0.0.0.0:8999`。Android 客户端默认地址为 `http://10.0.2.2:8999/`，这是 Android 官方模拟器访问宿主机的专用地址。

| 运行位置 | 客户端中填写的地址 |
| --- | --- |
| Android 官方模拟器，服务端在当前电脑 | `http://10.0.2.2:8999` |
| 真机与服务端电脑在同一局域网 | `http://电脑的局域网IP:8999` |
| 已配置 HTTPS 的远程服务 | `https://你的域名` |

真机不能用 `127.0.0.1` 访问电脑上的服务端。请确认手机能访问该地址，并允许防火墙的 TCP 8999 入站连接。客户端允许局域网 HTTP 明文连接；公网部署建议使用 HTTPS。

在客户端进入“我的”，填写地址后点击“保存并连接”，再用“测试连接”检查身份初始化和延迟。地址会通过 DataStore 保存，末尾 `/` 会自动补齐。

## 身份与账号

客户端连接服务端时先调用 `/api/ui/viewer` 创建或恢复访客身份。服务端 Cookie 会按协议、主机和端口持久保存；应用重启后会用该 Cookie 恢复身份，并重新读取 `X-Juku-Viewer` 标识。API、封面和代理媒体请求都会携带当前 Cookie、viewer 标识及服务端要求的同源请求头，因此匿名历史和登录状态不会因为普通重启而丢失。

如果服务端开启“必须登录才能使用”，请在“我的”中登录。首次启动生成的 `admin` 密码显示在 Go 服务端控制台；该账号第一次登录后必须修改初始密码。服务端会在改密完成前拒绝业务接口，客户端会显示“立即修改密码”，修改成功后再加载业务数据。新密码要求 10–128 个字符。

切换服务端地址、登录、退出或改密后，客户端会清空当前页面缓存并重新建立对应服务端的身份。清除 Android 应用数据会同时清除保存的地址和本机 Cookie；需要跨设备同步观看记录时应登录同一账号。

## 播放协议与进度

播放按下面的顺序工作：

1. 调用 `/api/ui/playback/open` 创建会话并取得分集与续播位置。
2. 调用 `/api/ui/playback/plan`，默认请求 `proxy` 模式，让媒体继续经过果果剧库服务端鉴权与中转。
3. 如果服务端返回 legacy 播放计划，客户端用更高的 stream version 调用 `/api/ui/playback/hls/open` 获取兼容的原生 HLS 地址。
4. ExoPlayer 使用持久 Cookie 和 viewer 请求头读取 MP4/HLS；媒体请求失败时会依次尝试兼容模式，并在会话过期时重建一次会话。

强制使用服务端代理可以避免重定向到上游媒体地址时携带果果剧库的账号 Cookie。播放请求使用递增版本号，旧分集或旧会话的迟到响应不会覆盖用户刚选择的新内容。

应用在前台时，播放会话每 20 秒发送一次心跳。播放中大约每 10 秒上报进度；暂停、拖动结束、切集、正常播放结束和进入后台时会补报最新位置。页面内返回、Android 系统返回或 Activity 销毁时会关闭播放器并释放服务端会话。服务端返回 HTTP 410 时，客户端会尝试在当前分集与位置重建一次会话。

## 构建与测试

已验证的本地构建环境：

- JDK 21
- Android SDK 35
- Gradle Wrapper 8.11.1
- Min SDK 26，Target SDK 35

`android/local.properties` 中的 `sdk.dir` 是机器本地路径。换电脑后需要改成该机器的 Android SDK 目录，不要照搬现有绝对路径。

Windows PowerShell：

```powershell
cd android
java -version
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
Copy-Item .\app\build\outputs\apk\debug\app-debug.apk ..\juku-app-debug.apk -Force
```

Linux / macOS：

```bash
cd android
chmod +x gradlew
./gradlew testDebugUnitTest
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk ../juku-app-debug.apk
```

构建产物：

- Debug：`android/app/build/outputs/apk/debug/app-debug.apk`
- Release（未签名）：`android/app/build/outputs/apk/release/app-release-unsigned.apk`
- 项目根目录同步副本：`juku-app-debug.apk`

连接设备后可执行：

```powershell
.\gradlew.bat installDebug
# 或在仓库根目录执行
adb install -r .\juku-app-debug.apk
```

单元测试覆盖客户端请求路径与 JSON 协议、viewer/Cookie 请求头、追剧更新、legacy 播放回退版本，以及播放进度的 10 秒上报节流。

## 当前限制

Android 客户端目前覆盖常用的浏览、账号和在线播放链路，但还没有与 Web 端完全对齐：

- 客户端尚未提供新建下载、横屏切换、画质选择和弹幕加载；对应的未接线控件不会显示。
- 播放结束会保存完成进度，但不会自动切到下一集；可使用“下一集”按钮或选集面板。
- 下载页只监控服务端已有任务并提交合并，不负责新建、暂停、继续或取消下载任务。这些操作当前请使用 Web 端。
- 注册、用户管理、服务端高级设置、下载文件管理等管理能力仍需使用 Web 端。

## 代码结构

```text
android/app/src/main/java/com/juku/app/
├── data/
│   ├── api/          # Retrofit 接口、Cookie 与播放兼容逻辑
│   ├── model/        # 与 Go JSON 协议对应的数据模型
│   └── repository/   # 页面使用的数据与会话操作
└── ui/
    ├── home/         # 剧库与搜索
    ├── following/    # 追剧与历史
    ├── player/       # Media3 播放器、选集与进度
    ├── download/     # 任务状态与合并入口
    └── settings/     # 服务地址、连接与账号
```
