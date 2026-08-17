# SSAID (Android ID 修改工具)

<p align="center">
  <strong>一款为 Android 打造的现代化 SSAID（Android ID）查询、修改、备份与管理工具</strong>
</p>

<p align="center">
  <strong>简体中文</strong> |
  <a href="README.md"><strong>English</strong></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-10%2B%20(API%2029--37)-3DDC84?style=flat&logo=android&logoColor=white" alt="Android Version" />
  <img src="https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=flat&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/JDK-21-ED8B00?style=flat&logo=openjdk&logoColor=white" alt="JDK 21" />
  <img src="https://img.shields.io/badge/Root-KernelSU%20%7C%20APatch%20%7C%20Magisk-critical?style=flat" alt="Root" />
</p>

---

## 🌟 核心特色

### 1. ⚡ 专为 KernelSU / APatch / Magisk 优化
- **一键静默授权**：点击“查询并修改 SSAID”即可直接唤起超级用户授权弹窗，无需额外中间对话框。
- **高级自定义路径**：**长按“查询并修改 SSAID”按钮** 即可随时自定义 `su` 执行路径（例如 `/data/adb/ksu/bin/su` 或 `/system/xbin/su`）。

### 2. 📄 深度适配 Android 系统底层 XML
- **支持 Android Binary XML (ABX)**：针对 Android 12 及以上版本系统启用的二进制 XML 格式，内置专属 ABX 编解码器，支持二进制与明文格式无缝读写。
- **实时写入系统配置**：精准修改 `/data/system/users/{userId}/settings_ssaid.xml`，自动保持文件权限（`600`）与所有者（`system:system`）。

### 3. 🔍 多维度分类过滤与实时搜索
- **实时搜索**：支持通过应用名称、包名（Package Name）或 16 进制 SSAID 数值快速过滤。
- **分类过滤芯片（Filter Chips）**：
  - **全部**：查看所有存在 SSAID 记录的应用。
  - **用户应用**：快速筛选第三方安装的 App。
  - **系统应用**：查看系统内置或预装组件。
  - **已修改**：即时查看本机有历史修改记录的应用。

### 4. 🎲 自定义、随机生成与历史还原
- **十六进制格式校验**：严格确保符合 16 位十六进制（`[0-9a-fA-F]{16}`）标准格式。
- **一键随机**：快速生成标准格式的随机 SSAID。
- **完整历史轨迹**：本地记录每一次修改的旧值、新值与时间戳，支持一键回滚还原。

### 5. 🧹 应用数据重置与重启菜单
- **一键清除数据（Clear Data）**：修改 SSAID 后可直接对目标 App 执行数据与缓存清除（`pm clear`），重置为全新安装状态。
- **快速软重启（Soft Reboot）**：支持 5 秒快速重启 Zygote / SystemServer，免关机即可让新 SSAID 立即生效。

---

## 📱 使用指南

1. **获取与授权**：
   - 打开应用程序，点击 **“查询并修改 SSAID”**，在系统弹出的 Root 授权窗口（KernelSU / Magisk / APatch）中点击 **允许**。
   - *提示：如需指定特殊 `su` 路径，请**长按**“查询并修改 SSAID”按钮。*
2. **搜索与过滤**：
   - 通过顶部搜索栏或“用户应用 / 系统应用 / 已修改”标签快速定位目标程序。
3. **修改与生效**：
   - 点击对应项目的 **[ ✏️ 修改 ]** 或 **[ 🎲 随机 ]** 应用新 SSAID。
   - 点击卡片上的 **[ 🧹 清除 ]** 清除目标 App 的本地缓存数据。
   - 点击顶部 **电源图标** 执行 **快速软重启** 或 **完整重启系统**，新 SSAID 即可全局生效！

---

## 🔨 源码构建 (Building from Source)

### 依赖要求
- **JDK 21**
- **Android SDK** (API 37, Build-Tools 35.0.0+)
- **Git**（版本号 `versionCode` 自动关联 Git Commits 总数）

### 构建命令

```powershell
# 克隆仓库
git clone https://github.com/HSSkyBoy/AndroidIDChnage.git
cd AndroidIDChnage

# 构建 Debug 测试版 APK
./gradlew assembleDebug

# 构建 Release 发行版 APK
./gradlew assembleRelease
```

- **输出路径**：
  - `app/build/outputs/apk/debug/app-debug.apk`
  - `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 📄 免责声明 (Disclaimer)

本工具需要 Root 权限修改系统底层数据。修改 SSAID 可能会导致部分应用程序的本地登录状态或设备绑定信息重置。请在清楚了解操作目的的前提下谨慎使用。
