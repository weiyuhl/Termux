# 不能重构的 Java 文件记录

本文档记录在 Termux 项目中尝试重构为 Kotlin 后会导致功能异常的 Java 文件。

## 1. TermuxService.java

### 文件路径
```
app/src/main/java/com/termux/app/TermuxService.java
```

### 问题描述
将此文件重构为 Kotlin 后，会导致**终端文字消失，无法显示任何文本内容**。

### 尝试记录
- **首次尝试时间**: 2026年3月9日
- **重构结果**: 编译成功，但运行时终端无法显示文字
- **二次尝试时间**: 2026年3月9日
- **修复尝试**: 将 Kotlin 属性改回方法调用以完全匹配 Java 版本
- **最终结果**: 仍然无法正常显示终端文字

### 原因分析
该文件是 Termux 的核心服务类，负责：
- 管理所有终端会话 (TermuxSession)
- 管理后台任务 (AppShell/TermuxTask)
- 处理前台服务通知
- 管理 WakeLock 和 WifiLock
- 处理终端会话的创建、销毁和客户端绑定

由于涉及复杂的生命周期管理和客户端绑定逻辑，Kotlin 转换可能在某些细微之处与 Java 版本存在行为差异，导致终端显示功能失效。

### 决策
**保持 Java 版本，不进行 Kotlin 重构。**

### 相关文件
- `app/src/main/java/com/termux/app/TermuxActivity.java` - 与此服务交互的主 Activity
- `app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java` - 终端会话客户端
- `app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionServiceClient.java` - 服务端客户端

---

## 统计信息

- **不能重构的文件数量**: 1
- **最后更新时间**: 2026年3月9日
