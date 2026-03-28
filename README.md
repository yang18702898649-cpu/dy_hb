# 抖音红包助手 🤖

一个基于 Android 无障碍服务的抖音直播间自动抢红包工具。

[![Build APK](https://github.com/YOUR_USERNAME/YOUR_REPO_NAME/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/YOUR_REPO_NAME/actions/workflows/build.yml)

## 📱 功能特性

- ✅ 自动打开抖音APP
- ✅ 自动进入直播频道
- ✅ **截图识别红包** - 只需提供红包截图模板
- ✅ 自动点击抢红包
- ✅ 自动滑动切换直播间
- ✅ 悬浮窗控制面板
- ✅ **详细统计** - 每个直播间的点击/成功/礼物数据
- ✅ 运行日志显示

## 🚀 快速开始

### 方式1：直接下载 APK（推荐）

1. 进入 [Releases](../../releases) 页面
2. 下载最新版本的 APK
3. 安装到手机

### 方式2：自行编译

```bash
# 克隆仓库
git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
cd YOUR_REPO_NAME

# 编译 Debug 版本
./gradlew assembleDebug

# 或编译 Release 版本
./gradlew assembleRelease

# APK 输出位置
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release-unsigned.apk
```

## 📸 使用方法

### 1. 准备红包截图

将抖音直播间左上角的**红包/福袋图标**截图保存：

```
推荐路径（任选其一）：
├── /sdcard/Pictures/红包.png          ← 手机相册
├── /sdcard/DCIM/红包.png              ← 相机目录
├── app/src/main/assets/红包.png       ← 打包进APK
└── 项目根目录/红包.png                 ← 项目目录
```

**截图要求**：
- 文件名：`红包.png` 或 `红包2.png`
- 内容：只截取红包图标本身
- 尺寸：50x50 ~ 150x150 像素

### 2. 开启权限

1. 打开应用
2. 点击 **"开启无障碍服务"**
3. 在系统设置中找到 **"抖音红包助手"** 并开启
4. 允许 **悬浮窗权限**

### 3. 开始抢红包

1. 点击 **"开始抢红包"**
2. 悬浮窗会出现，可以拖动位置
3. 应用会自动打开抖音并开始抢红包
4. 查看 **"详细统计"** 了解抢红包数据

## 📊 统计功能

应用会记录详细的抢红包数据：

| 统计项 | 说明 |
|-------|------|
| **房间统计** | 每个直播间的点击次数、成功率、获得礼物 |
| **历史记录** | 每次抢红包的详细记录 |
| **排行榜** | 成功率最高的房间排名 |
| **礼物统计** | 礼物类型、数量、价值 |

数据保存在本地，重启应用不会丢失。

## ⚙️ 配置参数

在 `DouYinAutomation.kt` 中可以调整：

```kotlin
var swipeDistance: Int = 800        // 滑动距离
var swipeDuration: Long = 300       // 滑动持续时间
var swipeInterval: Long = 200       // 滑动间隔（毫秒）
var grabWaitTime: Long = 5000       // 抢红包后等待时间
var maxNoLiveCount: Int = 15        // 最大无红包次数
```

## 🏗️ 项目结构

```
yh_tool/
├── app/src/main/
│   ├── java/com/example/douyinredpacket/
│   │   ├── MainActivity.kt              # 主界面
│   │   ├── StatisticsActivity.kt        # 统计界面
│   │   ├── automation/
│   │   │   └── DouYinAutomation.kt      # 抖音自动化操作
│   │   ├── detector/
│   │   │   └── RedPacketDetector.kt     # 红包检测器（截图匹配）
│   │   ├── service/
│   │   │   ├── FloatingWindowService.kt # 悬浮窗服务
│   │   │   └── RedPacketAccessibilityService.kt # 无障碍服务
│   │   ├── data/
│   │   │   └── StatisticsManager.kt     # 统计管理器
│   │   └── utils/
│   │       ├── ErrorHandler.kt          # 错误处理
│   │       └── ScreenUtils.kt           # 屏幕工具
│   └── res/                             # 布局和资源文件
├── .github/workflows/build.yml          # GitHub Actions 自动打包
├── 红包.png                             # 红包截图模板（可选）
└── README.md
```

## 🔧 系统要求

- Android 8.0+ (API 26+)
- 无需 ROOT 权限
- 需要开启无障碍服务和悬浮窗权限

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

---

**注意**：本工具仅供学习交流使用，请遵守抖音平台规则。
