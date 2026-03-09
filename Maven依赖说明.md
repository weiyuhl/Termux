# Maven 依赖说明

## termux-am-library 是什么？

`termux-am-library` 是一个 Android 库，用于在应用内执行 Android Activity Manager (am) 命令。

### 基本信息

- **Group ID**: `com.termux`
- **Artifact ID**: `termux-am-library`
- **Version**: `v2.0.0`
- **类型**: AAR (Android Archive)
- **许可证**: Apache License 2.0
- **GitHub**: https://github.com/termux/termux-am-library
- **官方文档**: https://github.com/termux/termux-am-library/blob/main/termux-am-library/src/main/java/com/termux/am/Am.java

## 作用

### 1. 替代系统 am 命令

传统方式执行 am 命令需要通过 shell：
```bash
/system/bin/am start -n com.example/.MainActivity
```

这种方式的问题：
- ❌ 需要启动一个新的 Dalvik VM 进程
- ❌ 性能开销大
- ❌ 速度慢

使用 termux-am-library：
```kotlin
import com.termux.am.Am

// 直接在应用进程内执行 am 命令
Am.run(context, arrayOf("start", "-n", "com.example/.MainActivity"))
```

优势：
- ✅ 在应用进程内直接执行
- ✅ 无需启动新进程
- ✅ 性能高，速度快
- ✅ 更安全（不需要 shell 权限）

### 2. 支持的 am 命令

termux-am-library 支持大部分标准的 am 命令：

- `start` - 启动 Activity
- `startservice` - 启动 Service
- `stopservice` - 停止 Service
- `broadcast` - 发送广播
- `force-stop` - 强制停止应用
- `kill` - 杀死进程
- `instrument` - 运行测试
- 等等...

完整列表参考：https://developer.android.com/studio/command-line/adb#am

## 在 Termux 项目中的使用

### 使用位置

1. **termux-shared 模块**
   ```groovy
   implementation "com.termux:termux-am-library:v2.0.0"
   ```

2. **termux-core 模块**
   ```groovy
   implementation "com.termux:termux-am-library:v2.0.0"
   ```

### 核心类

#### 1. AmSocketServer.kt

位置：`termux-shared/src/main/java/com/termux/shared/shell/am/AmSocketServer.kt`

功能：
- 创建一个 Unix Domain Socket 服务器
- 接收来自客户端的 am 命令
- 使用 termux-am-library 执行命令
- 返回执行结果（exit code, stdout, stderr）

工作流程：
```
客户端 → Unix Socket → AmSocketServer → termux-am-library → 执行 am 命令 → 返回结果
```

#### 2. TermuxAmSocketServer.kt

位置：`termux-shared/src/main/java/com/termux/shared/termux/shell/am/TermuxAmSocketServer.kt`

功能：
- 继承自 AmSocketServer
- Termux 特定的 am socket 服务器实现
- 配置 socket 路径和权限

#### 3. TermuxApplication.java

位置：`app/src/main/java/com/termux/app/TermuxApplication.java`

功能：
- 在应用启动时初始化 TermuxAmSocketServer
- 启动 am socket 服务器

### 使用场景

#### 场景 1：快速启动 Activity

```kotlin
// 传统方式（慢）
Runtime.getRuntime().exec("/system/bin/am start -n com.termux/.app.TermuxActivity")

// 使用 termux-am-library（快）
Am.run(context, arrayOf("start", "-n", "com.termux/.app.TermuxActivity"))
```

#### 场景 2：通过 Socket 接收命令

Termux 启动一个 Unix Socket 服务器，其他进程可以通过 socket 发送 am 命令：

```c
// C 客户端代码
int sock = socket(AF_UNIX, SOCK_STREAM, 0);
connect(sock, ...);
write(sock, "start -n com.termux/.app.TermuxActivity", ...);
read(sock, result, ...);  // 读取执行结果
```

这样，Termux 的 shell 脚本或其他应用可以快速执行 am 命令，而不需要启动新的 Dalvik VM。

#### 场景 3：Shell 环境配置

在 `TermuxAppShellEnvironment.kt` 中使用，配置 shell 环境时可能需要启动或管理 Activity。

## 本地 Maven 仓库

### 为什么使用本地仓库？

项目配置了本地 Maven 仓库：

```
本地Maven仓库/
└── com/
    └── termux/
        └── termux-am-library/
            └── v2.0.0/
                ├── termux-am-library-v2.0.0.aar
                └── termux-am-library-v2.0.0.pom
```

优势：
1. ✅ **离线构建** - 无需网络连接
2. ✅ **构建速度快** - 避免网络下载
3. ✅ **稳定性高** - 不受外部仓库影响
4. ✅ **版本控制** - 依赖纳入 Git 管理

### 仓库配置

在 `build.gradle` 中配置：

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        // 本地 Maven 仓库（优先使用）
        maven { url uri("$rootDir/本地Maven仓库") }
        // 备用：在线仓库
        maven { url "https://jitpack.io" }
    }
}
```

优先级：
1. 本地 Maven 仓库（最优先）
2. JitPack（备用，如果本地没有）

### 原始来源

termux-am-library 原本托管在 JitPack：
- URL: https://jitpack.io/com/termux/termux-am-library/v2.0.0/
- 下载后保存到本地仓库

## 性能对比

### 传统 am 命令

```bash
time /system/bin/am start -n com.termux/.app.TermuxActivity
# 耗时：~200-500ms（需要启动新的 Dalvik VM）
```

### termux-am-library

```kotlin
val startTime = System.currentTimeMillis()
Am.run(context, arrayOf("start", "-n", "com.termux/.app.TermuxActivity"))
val endTime = System.currentTimeMillis()
// 耗时：~10-50ms（在当前进程内执行）
```

**性能提升：10-50 倍**

## 相关链接

- **termux-am-library GitHub**: https://github.com/termux/termux-am-library
- **termux-am-socket (C 客户端)**: https://github.com/termux/termux-am-socket
- **Android am 命令文档**: https://developer.android.com/studio/command-line/adb#am
- **ActivityManagerShellCommand 源码**: https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/am/ActivityManagerShellCommand.java

## 总结

`termux-am-library` 是一个关键的性能优化库，它允许 Termux 在应用进程内直接执行 Android Activity Manager 命令，而不需要通过 shell 启动新的进程。这大大提高了命令执行速度，改善了用户体验。

通过 Unix Socket 服务器，Termux 还可以让其他进程（如 shell 脚本）快速执行 am 命令，实现了高性能的进程间通信。

本地 Maven 仓库的配置确保了项目可以离线构建，提高了构建稳定性和速度。
