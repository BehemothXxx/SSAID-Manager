# SSAID (Android ID Modifier)

<p align="center">
  <strong>A modern Android SSAID (Android ID) inspector, modifier, backup, and management utility</strong>
</p>

<p align="center">
  <a href="README_zh.md"><strong>简体中文</strong></a> |
  <strong>English</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-10%2B%20(API%2029--37)-3DDC84?style=flat&logo=android&logoColor=white" alt="Android Version" />
  <img src="https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=flat&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/JDK-21-ED8B00?style=flat&logo=openjdk&logoColor=white" alt="JDK 21" />
  <img src="https://img.shields.io/badge/Root-KernelSU%20%7C%20APatch%20%7C%20Magisk-critical?style=flat" alt="Root" />
</p>

---

## 🌟 Key Features

### 1. ⚡ Optimized for KernelSU / APatch / Magisk
- **Seamless Silent Authorization**: Tap **"Query & Modify SSAID"** to directly trigger the superuser authorization prompt without unnecessary intermediate dialogs.
- **Custom SU Path**: **Long-press the "Query & Modify SSAID" button** at any time to configure a custom `su` binary path (e.g., `/data/adb/ksu/bin/su` or `/system/xbin/su`).

### 2. 📄 Deep System XML & Binary XML (ABX) Compatibility
- **Android Binary XML (ABX) Support**: Built-in codec natively supports both plain XML and binary XML formats used by Android 12+.
- **Direct System Settings Write**: Accurately inspects and modifies `/data/system/users/{userId}/settings_ssaid.xml` while preserving file permissions (`600`) and ownership (`system:system`).

### 3. 🔍 Multi-Dimensional Filter Chips & Real-time Search
- **Instant Search**: Search by app name, package name, or hexadecimal SSAID values.
- **Filter Chips**:
  - **All**: View all apps with SSAID records.
  - **User Apps**: Quickly isolate third-party installed applications.
  - **System Apps**: Filter system packages and pre-installed components.
  - **Modified**: View apps that have local modification/backup history.

### 4. 🎲 Custom Editing, Random Generation & History Rollback
- **Hex Validation**: Ensures valid 16-character hexadecimal values (`[0-9a-fA-F]{16}`).
- **One-click Randomize**: Generates compliant, randomized SSAIDs instantly.
- **Full History Tracking**: Records previous values, new values, and timestamps with one-click restore capabilities.

### 5. 🧹 App Reset & Quick Reboot Menu
- **One-click Clear Data**: Directly execute `pm clear <package>` from any app card to reset cache and force the app to obtain the new SSAID upon next launch.
- **Fast Soft Reboot**: Supports a 5-second soft reboot (Zygote / SystemServer restart) to apply changes globally without a full device power cycle.

### 6. 🎨 Modern Neo UI Design
- Clean card architecture inspired by **[Neo Art Jiagu](https://github.com/HSSkyBoy/Art-Jiagu)** UI, dynamic status indicator dots, and adaptive Dark / Light theme palettes.

---

## 📱 User Guide

1. **Grant Root Access**:
   - Open SSAID, tap **"Query & Modify SSAID"**, and allow root access in your root manager (KernelSU / Magisk / APatch).
   - *Tip: If you need a non-standard `su` binary path, **long-press** the "Query & Modify SSAID" button.*
2. **Search & Filter**:
   - Use the search bar or filter chips (User Apps / System Apps / Modified) to locate target apps.
3. **Modify & Apply**:
   - Tap **[ ✏️ Edit ]** or **[ 🎲 Random ]** to assign a new SSAID.
   - Tap **[ 🧹 Clear ]** to wipe the target app's local data/cache.
   - Tap the **Power Icon** in the top header to execute a **Soft Reboot** or **Full Reboot** to activate your changes.

---

## 🔨 Building from Source

### Requirements
- **JDK 21**
- **Android SDK** (API 37, Build-Tools 35.0.0+)
- **Git** (version code is automatically derived from the total Git commit count)

### Build Commands

```powershell
# Clone repository
git clone https://github.com/HSSkyBoy/AndroidIDChnage.git
cd AndroidIDChnage

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

- **Output Paths**:
  - `app/build/outputs/apk/debug/app-debug.apk`
  - `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 📄 Disclaimer

This tool requires root access to modify low-level Android system configuration files. Changing an app's SSAID may reset its local login state or device binding information. Use responsibly at your own discretion.
