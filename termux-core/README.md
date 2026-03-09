# Termux Core Module

## 概述

`termux-core` 是 Termux 的核心模块，包含实现 Linux 功能的核心代码。这个模块封装了 Termux 最基础和最关键的功能，独立于 UI 和应用层逻辑。

## 模块职责

### 1. Bootstrap 安装
- 部署完整的 Linux 用户空间环境到 Android 设备
- 解压 Bootstrap zip 文件到 `$PREFIX` 目录
- 创建符号链接和设置文件权限
- 支持多架构（arm, aarch64, i686, x86_64）

### 2. 进程创建
- 通过 JNI 调用 Native 代码创建 Linux 进程
- 使用标准 POSIX `fork()` + `execvp()` 系统调用
- 支持 PTY（伪终端）和标准进程两种模式

### 3. 命令执行
- **AppShell**: 后台命令执行（使用 `Runtime.exec()`）
- **TermuxSession**: 交互式终端会话（使用 PTY）
- 命令状态管理和结果处理

### 4. 环境配置
- Unix Shell 环境变量设置（PATH, HOME, LD_LIBRARY_PATH 等）
- Android Shell 环境适配
- Termux 特定环境配置（PREFIX, TMPDIR 等）
- 支持多种 Shell 环境（bash, zsh, fish 等）

### 5. AM Socket Server
- 提供高性能的 Activity Manager 命令服务
- 使用 Unix Domain Socket 进行进程间通信
- 比传统 `/system/bin/am` 更快（无需启动 dalvik vm）

### 6. 核心服务
- TermuxService: 管理所有终端会话和后台任务
- RunCommandService: 处理外部应用的命令执行请求
- 前台服务通知和 WakeLock 管理

## 文件结构

```
termux-core/
├── src/main/
│   ├── java/com/termux/
│   │   ├── app/                    # 应用层核心
│   │   │   ├── TermuxInstaller.kt  # Bootstrap 安装器
│   │   │   ├── RunCommandService.kt # 命令执行服务
│   │   │   └── TermuxService.java  # 核心服务（保持 Java）
│   │   ├── terminal/
│   │   │   └── JNI.kt              # JNI 接口
│   │   └── shared/
│   │       ├── termux/             # Termux 特定功能
│   │       │   ├── TermuxBootstrap.kt
│   │       │   ├── TermuxConstants.kt
│   │       │   └── shell/          # Shell 管理
│   │       └── shell/              # 通用 Shell 功能
│   │           ├── command/        # 命令执行
│   │           └── am/             # AM Socket Server
│   └── cpp/                        # Native 代码
│       ├── termux-bootstrap.c      # Bootstrap 数据提供
│       ├── termux-bootstrap-zip.S  # Bootstrap 数据嵌入（汇编）
│       ├── Android.mk              # NDK 构建配置
│       └── bootstrap-*.zip         # Bootstrap 文件（多架构）
└── build.gradle
```

## 核心文件清单

### Kotlin 文件（25 个）
1. TermuxInstaller.kt - Bootstrap 安装
2. RunCommandService.kt - 命令执行服务
3. JNI.kt - JNI 接口
4. TermuxBootstrap.kt - Bootstrap 配置
5. TermuxConstants.kt - 常量定义
6. TermuxShellManager.kt - Shell 管理器
7. TermuxShellUtils.kt - Shell 工具
8. TermuxAmSocketServer.kt - AM Socket Server
9. TermuxShellEnvironment.kt - Termux Shell 环境
10. TermuxAppShellEnvironment.kt - App Shell 环境
11. TermuxAPIShellEnvironment.kt - API Shell 环境
12. TermuxShellCommandShellEnvironment.kt - Shell 命令环境
13. TermuxSession.kt - 终端会话
14. ShellUtils.kt - Shell 工具
15. ArgumentTokenizer.kt - 参数解析器
16. StreamGobbler.kt - 流读取器
17. AmSocketServer.kt - AM Socket Server 基类
18. ExecutionCommand.kt - 命令执行模型
19. ShellCommandConstants.kt - Shell 命令常量
20. UnixShellEnvironment.kt - Unix Shell 环境
21. ShellEnvironmentUtils.kt - 环境工具
22. AndroidShellEnvironment.kt - Android Shell 环境
23. ShellCommandShellEnvironment.kt - Shell 命令环境
24. ResultData.kt - 命令结果数据
25. AppShell.kt - 后台命令执行

### Java 文件（1 个）
26. TermuxService.java - 核心服务（保持 Java，重构后会导致终端文字消失）

### Native 代码（2 个）
- termux-bootstrap.c - C 代码
- termux-bootstrap-zip.S - 汇编代码

## 依赖关系

### 依赖的模块
- `terminal-emulator`: 终端模拟器核心
- `terminal-view`: 终端视图
- `termux-shared`: 共享工具类（临时依赖，后续会优化）

### 被依赖的模块
- `app`: Termux 主应用

## 技术栈

- **语言**: Kotlin (主要), Java (TermuxService), C (Native), 汇编 (Bootstrap 嵌入)
- **Android SDK**: API 24+
- **NDK**: 用于编译 Native 代码
- **第三方库**: Guava, Commons IO

## 构建

```bash
# 编译 termux-core 模块
./gradlew :termux-core:assembleDebug

# 编译整个项目
./gradlew assembleDebug
```

## 测试

```bash
# 运行单元测试
./gradlew :termux-core:test

# 运行集成测试
./gradlew :termux-core:connectedAndroidTest
```

## 注意事项

1. **TermuxService.java**: 这个文件保持 Java 版本，因为重构为 Kotlin 后会导致终端文字显示异常。详见 `不能重构的Java文件记录.md`。

2. **Bootstrap 文件**: Bootstrap zip 文件通过汇编的 `.incbin` 指令嵌入到 APK 中，确保这些文件在编译前存在于 `src/main/cpp/` 目录。

3. **依赖优化**: 当前模块依赖整个 `termux-shared`，后续会优化为只依赖必要的工具类。

4. **包名保持**: 为了最小化改动，所有文件保持原有的包名结构。

## 相关文档

- [Termux核心Linux功能实现代码文件.md](../Termux核心Linux功能实现代码文件.md) - 核心功能详细说明
- [Termux项目Java文件分类与重构状态.md](../Termux项目Java文件分类与重构状态.md) - 文件分类和重构状态
- [Termux核心模块提取方案.md](../Termux核心模块提取方案.md) - 模块提取方案
- [不能重构的Java文件记录.md](../不能重构的Java文件记录.md) - TermuxService 问题记录

## 版本历史

- **v1.0** (2026-03-09): 初始版本，从 app、terminal-emulator、termux-shared 模块提取核心文件

## 许可证

与 Termux 主项目相同，遵循 GPLv3 许可证。
