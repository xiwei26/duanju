# Agents 工作指引与项目打包规范

本文档为 AI Agent 及开发者提供《果果剧库》项目的构建、打包与部署操作指南。

---

## 目录
- [一、项目架构简述](#一项目架构简述)
- [二、Android 原生客户端打包 (APK)](#二android-原生客户端打包-apk)
  - [1. 构建前环境依赖](#1-构建前环境依赖)
  - [2. 打包命令](#2-打包命令)
  - [3. 产物路径与同步规范](#3-产物路径与同步规范)
  - [4. 安装与调试运行](#4-安装与调试运行)
  - [5. 常见问题排查](#5-常见问题排查)
- [三、Go 服务端打包](#三go-服务端打包)
  - [1. Windows 独立可执行程序](#1-windows-独立可执行程序)
  - [2. 跨平台编译 (Linux / macOS)](#2-跨平台编译-linux--macos)
  - [3. Docker 容器镜像](#3-docker-容器镜像)
- [四、Agent 自动化执行 CheckList](#四agent-自动化执行-checklist)

---

## 一、项目架构简述

- **根目录 / Go 服务端**：基于 Go 原生 HTTP / 内置静态资源的前后端一体化服务，处理剧库搜索、分集解析、视频代理与鉴权。
- **`android/`**：Android 原生客户端，基于 Kotlin + Jetpack Compose + AndroidX Media3 (ExoPlayer) + Material3 构建。

---

## 二、Android 原生客户端打包 (APK)

### 1. 构建前环境依赖

- **JDK 版本**：OpenJDK 21 (已验证支持 OpenJDK 21.0.12 LTS)。
- **Android SDK**：
  - Compile SDK: `35`
  - Min SDK: `26`
  - Target SDK: `35`
  - SDK 路径配置位于 `android/local.properties`：
    ```properties
    sdk.dir=C\:\\Users\\xiwei\\AppData\\Local\\Android\\Sdk
    ```
    > **注**：在不同机器上运行时，若 SDK 路径变动，请优先检查或更新 `android/local.properties`。

### 2. 打包命令

构建脚本位于 `android/` 目录中。请切换到 `android/` 目录或直接指定路径执行：

#### Windows 环境 (PowerShell / CMD)
```powershell
# 进入 android 目录
cd android

# 1. 打包 Debug APK（推荐日常调试与快速验证）
.\gradlew.bat assembleDebug

# 2. 打包 Release APK
.\gradlew.bat assembleRelease

# 3. 清理并重新打包
.\gradlew.bat clean assembleDebug
```

#### Linux / macOS 环境 (Bash / Zsh)
```bash
cd android
chmod +x gradlew
./gradlew assembleDebug
```

### 3. 产物路径与同步规范

- **Gradle 默认输出目录**：
  - Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`
  - Release APK: `android/app/build/outputs/apk/release/app-release-unsigned.apk`
- **项目根目录快捷同步规范**：
  项目根目录下维护一份快捷访问的 `juku-app-debug.apk`。
  当 Agent 或开发者重新打包后，**必须将生成的 APK 复制同步到根目录**，便于用户直接获取：
  ```powershell
  # PowerShell 同步命令（在项目根目录下执行）
  Copy-Item android/app/build/outputs/apk/debug/app-debug.apk -Destination ./juku-app-debug.apk -Force
  ```

### 4. 安装与调试运行

当连接了 Android 手机（开启 USB 调试）或 Android 模拟器时：

```powershell
# 方式 A：通过 Gradle 直接安装到当前设备
cd android
.\gradlew.bat installDebug

# 方式 B：使用 ADB 安装根目录产物
adb install -r juku-app-debug.apk
```

### 5. 常见问题排查

1. **`SDK location not found`**：
   - 检查 `android/local.properties` 是否存在且 `sdk.dir` 指向正确的 Android SDK 绝对路径。
2. **Gradle Daemon 内存或锁占用**：
   - 可执行 `.\gradlew.bat --stop` 停止后台守护进程后重试。
3. **网络依赖下载超时**：
   - 检查 Maven 仓库配置（位于 `android/settings.gradle.kts`），确保 `google()`、`mavenCentral()` 可正常访问。

---

## 三、Go 服务端打包

### 1. Windows 独立可执行程序

直接运行根目录下批处理脚本或使用 Go CLI：

```powershell
# 使用现成构建脚本
.\scripts\build.bat

# 或手动构建：
$env:CGO_ENABLED="0"
$env:GOOS="windows"
$env:GOARCH="amd64"
go build -trimpath -ldflags="-s -w" -o dist/juku_windows_amd64.exe .
```
产物将输出至：`dist/juku_windows_amd64.exe`。

### 2. 跨平台编译 (Linux / macOS)

```bash
# Linux amd64
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath -ldflags="-s -w" -o dist/juku_linux_amd64 .

# Linux arm64 (常用于树莓派/部分云服务器)
CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -trimpath -ldflags="-s -w" -o dist/juku_linux_arm64 .

# macOS Apple Silicon (arm64)
CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build -trimpath -ldflags="-s -w" -o dist/juku_darwin_arm64 .
```

### 3. Docker 容器镜像

```bash
# 构建镜像
docker build -t juku:latest .

# 使用 docker-compose 启动
docker compose up -d
```

---

## 四、Agent 自动化执行 CheckList

当接收到关于 **重新打包 Android APK** 的指令时，Agent 应遵循以下标准化工作流：

1. **环境自检**：
   - 检查 Java 版本：`java -version`（确认 Java 21 可用）。
   - 检查 Android SDK 路径：确认 `android/local.properties` 中的路径存在。
2. **执行构建**：
   - 工作目录设置为 `android/`，执行 `.\gradlew.bat assembleDebug`。
3. **产物验证与同步**：
   - 校验 `android/app/build/outputs/apk/debug/app-debug.apk` 生成时间与大小。
   - 复制更新根目录文件：`Copy-Item android/app/build/outputs/apk/debug/app-debug.apk -Destination ./juku-app-debug.apk -Force`。
   - 检验根目录下 `juku-app-debug.apk` 的属性确认同步成功。
4. **结果交付**：
   - 向用户清晰列出打包结果、文件大小、生成时间及本地绝对/相对路径。
