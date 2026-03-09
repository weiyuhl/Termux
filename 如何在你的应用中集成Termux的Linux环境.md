# 如何在你的应用中集成 Termux 的 Linux 环境

## 需要复制的模块

从 Termux 项目复制以下 4 个库模块到你的项目：

```
你的项目/
├── terminal-emulator/          # 终端模拟器核心
├── terminal-view/              # 终端视图
├── termux-shared/              # 共享工具库
└── termux-core/                # 核心模块
    └── bootstrap/              # Bootstrap 子模块（包含 Linux 环境）
```

## 需要复制的代码文件

从 app 模块复制以下文件到你的应用：

```
app/src/main/java/com/termux/app/TermuxInstaller.kt
```

复制到你的应用中，例如：
```
你的应用/src/main/java/你的包名/TermuxInstaller.kt
```

## 项目配置

### 1. settings.gradle

```groovy
include ':你的应用'
include ':terminal-emulator'
include ':terminal-view'
include ':termux-shared'
include ':termux-core'
include ':termux-core:bootstrap'
```

### 2. 你的应用/build.gradle

```groovy
dependencies {
    implementation project(":termux-core:bootstrap")
    implementation project(":terminal-view")
    implementation project(":termux-shared")
    
    // 其他必需的依赖
    implementation "androidx.core:core:1.13.1"
    implementation "com.google.android.material:material:1.12.0"
    implementation "com.google.guava:guava:24.1-jre"
    implementation "io.noties.markwon:core:4.6.2"
    implementation "io.noties.markwon:ext-strikethrough:4.6.2"
    implementation "io.noties.markwon:linkify:4.6.2"
    implementation "io.noties.markwon:recycler:4.6.2"
}
```

## 使用方法

在你的应用中调用以下方法来安装 Linux 环境：

```kotlin
TermuxInstaller.setupBootstrapIfNeeded(activity) {
    // Bootstrap 安装完成后的回调
    // 在这里可以启动终端或执行其他操作
}
```

## 说明

- **首次运行**：会自动解压 Bootstrap 到 `/data/data/你的包名/files/usr`
- **后续运行**：检测到已安装则直接跳过
- **安装大小**：解压后约 200-300MB
- **支持架构**：arm、aarch64、x86、x86_64

## 注意事项

1. 确保应用有足够的存储空间（至少 500MB）
2. 首次安装需要几秒到几十秒时间（取决于设备性能）
3. 安装过程会显示进度对话框
4. 安装失败会显示错误信息并提供崩溃报告选项
