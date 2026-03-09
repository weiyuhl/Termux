# Termux 核心模块提取方案

## 一、方案概述

### 1.1 目标
将已重构的 26 个核心 Kotlin 文件和 1 个必须保留的 Java 文件（TermuxService.java）提取为独立的核心模块 `termux-core`，实现：
- 核心 Linux 功能的模块化封装
- 清晰的依赖边界
- 便于维护和测试
- 可被其他模块复用

### 1.2 核心模块职责
- **Bootstrap 安装**: 部署 Linux 用户空间环境
- **进程创建**: 通过 JNI 调用 Native 代码创建 Linux 进程
- **命令执行**: 管理后台任务（AppShell）和交互式会话（TermuxSession）
- **环境配置**: 设置 Shell 环境变量
- **AM Socket Server**: 提供高性能的 Activity Manager 命令服务
- **服务管理**: TermuxService 核心服务
- **常量定义**: 核心路径和配置

---

## 二、文件分布分析

### 2.1 当前文件分布

#### App 模块（3 个文件）
```
app/src/main/java/com/termux/app/
├── TermuxInstaller.kt          ✅ 核心：Bootstrap 安装
├── RunCommandService.kt        ✅ 核心：命令执行服务
└── TermuxService.java          ❌ 核心：主服务（保持 Java）
```

#### Terminal Emulator 模块（1 个文件）
```
terminal-emulator/src/main/java/com/termux/terminal/
└── JNI.kt                      ✅ 核心：JNI 接口
```

#### Termux Shared 模块（22 个文件）
```
termux-shared/src/main/java/com/termux/shared/
├── termux/
│   ├── TermuxBootstrap.kt                                          ✅
│   ├── TermuxConstants.kt                                          ✅
│   └── shell/
│       ├── TermuxShellManager.kt                                   ✅
│       ├── TermuxShellUtils.kt                                     ✅
│       ├── am/
│       │   └── TermuxAmSocketServer.kt                             ✅
│       └── command/
│           ├── environment/
│           │   ├── TermuxShellEnvironment.kt                       ✅
│           │   ├── TermuxAppShellEnvironment.kt                    ✅
│           │   ├── TermuxAPIShellEnvironment.kt                    ✅
│           │   └── TermuxShellCommandShellEnvironment.kt           ✅
│           └── runner/
│               └── terminal/
│                   └── TermuxSession.kt                            ✅
└── shell/
    ├── ShellUtils.kt                                               ✅
    ├── ArgumentTokenizer.kt                                        ✅
    ├── StreamGobbler.kt                                            ✅
    ├── am/
    │   └── AmSocketServer.kt                                       ✅
    └── command/
        ├── ExecutionCommand.kt                                     ✅
        ├── ShellCommandConstants.kt                                ✅
        ├── environment/
        │   ├── UnixShellEnvironment.kt                             ✅
        │   ├── ShellEnvironmentUtils.kt                            ✅
        │   ├── AndroidShellEnvironment.kt                          ✅
        │   └── ShellCommandShellEnvironment.kt                     ✅
        ├── result/
        │   └── ResultData.kt                                       ✅
        └── runner/
            └── app/
                └── AppShell.kt                                     ✅
```

#### Native 代码（2 个文件）
```
app/src/main/cpp/
├── termux-bootstrap.c          🔧 Native：Bootstrap 数据提供
└── termux-bootstrap-zip.S      🔧 汇编：Bootstrap 数据嵌入
```


---

## 三、依赖关系分析

### 3.1 核心文件的外部依赖

#### 依赖 termux-shared 的其他类
核心文件依赖以下 termux-shared 中的非核心类：

1. **日志工具**
   - `com.termux.shared.logger.Logger`

2. **文件操作**
   - `com.termux.shared.file.FileUtils`
   - `com.termux.shared.file.filesystem.FileTypes`

3. **错误处理**
   - `com.termux.shared.errors.Error`
   - `com.termux.shared.errors.Errno`

4. **Android 工具**
   - `com.termux.shared.android.PackageUtils`
   - `com.termux.shared.android.SELinuxUtils`

5. **数据工具**
   - `com.termux.shared.data.DataUtils`

6. **网络 Socket**
   - `com.termux.shared.net.socket.local.*` (LocalClientSocket, LocalServerSocket 等)

7. **设置和配置**
   - `com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences`
   - `com.termux.shared.termux.settings.properties.TermuxAppSharedProperties`
   - `com.termux.shared.termux.settings.properties.TermuxPropertyConstants`

8. **Termux 工具**
   - `com.termux.shared.termux.TermuxUtils`
   - `com.termux.shared.termux.crash.TermuxCrashUtils`
   - `com.termux.shared.termux.plugins.TermuxPluginUtils`

9. **Shell 接口**
   - `com.termux.shared.shell.command.environment.IShellEnvironment`

#### 依赖 terminal-emulator
- `com.termux.terminal.TerminalSession`
- `com.termux.terminal.TerminalSessionClient`

#### 依赖 Android SDK
- `android.content.Context`
- `android.os.Build`
- `android.system.OsConstants`
- 等等

#### 依赖第三方库
- `com.google.common.base.Joiner` (Guava)
- `org.apache.commons.io.filefilter.TrueFileFilter`

### 3.2 TermuxService.java 的特殊依赖
TermuxService 作为核心服务，依赖大量 UI 和应用层组件：
- `TermuxActivity` (UI)
- `TerminalView` (UI)
- `NotificationUtils` (通知)
- `WakeLock` (电源管理)
- 等等

---

## 四、模块提取方案

### 4.1 方案选择

#### 方案 A：完全独立的核心模块（推荐）⭐
**结构**:
```
termux-core/
├── src/main/java/com/termux/core/
│   ├── bootstrap/          # Bootstrap 安装
│   ├── shell/              # Shell 命令执行
│   ├── environment/        # 环境配置
│   ├── am/                 # AM Socket Server
│   ├── service/            # TermuxService
│   └── constants/          # 常量定义
└── src/main/cpp/           # Native 代码
```

**优点**:
- 清晰的模块边界
- 独立的命名空间
- 便于版本管理
- 可独立测试

**缺点**:
- 需要重构包名
- 需要更新所有引用
- 工作量较大


#### 方案 B：保持包名的核心模块（实用）⭐⭐⭐
**结构**:
```
termux-core/
├── src/main/java/
│   └── com/termux/          # 保持原包名结构
│       ├── app/
│       │   ├── TermuxInstaller.kt
│       │   ├── RunCommandService.kt
│       │   └── TermuxService.java
│       ├── terminal/
│       │   └── JNI.kt
│       └── shared/
│           ├── termux/...
│           └── shell/...
└── src/main/cpp/            # Native 代码
```

**优点**:
- 保持原有包名，无需修改引用
- 工作量小
- 风险低
- 易于回滚

**缺点**:
- 包名不够清晰
- 模块边界不够明显

#### 方案 C：混合方案
将核心功能保持在 termux-shared，但创建一个 termux-core 模块作为聚合层。

**不推荐**：增加复杂度，没有实质性好处。

### 4.2 推荐方案：方案 B（保持包名）

考虑到：
1. 已有 26 个文件已重构完成
2. 保持包名可以最小化改动
3. 降低引入 bug 的风险
4. 便于逐步迁移

**推荐使用方案 B**。

---

## 五、实施步骤

### 5.1 准备阶段

#### Step 1: 创建 termux-core 模块
```bash
mkdir -p termux-core/src/main/java/com/termux
mkdir -p termux-core/src/main/cpp
```

#### Step 2: 创建 build.gradle
```groovy
plugins {
    id "com.android.library"
    id "kotlin-android"
}

android {
    namespace "com.termux.core"
    compileSdkVersion project.properties.compileSdkVersion.toInteger()
    ndkVersion project.properties.ndkVersion
    
    defaultConfig {
        minSdkVersion project.properties.minSdkVersion.toInteger()
        targetSdkVersion project.properties.targetSdkVersion.toInteger()
    }
    
    externalNativeBuild {
        ndkBuild {
            path "src/main/cpp/Android.mk"
        }
    }
}

dependencies {
    // Android 基础库
    implementation "androidx.annotation:annotation:1.9.0"
    implementation "androidx.core:core:1.13.1"
    
    // Kotlin
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    
    // 第三方库
    implementation "com.google.guava:guava:24.1-jre"
    implementation "commons-io:commons-io:2.5"
    
    // 依赖其他模块
    implementation project(":terminal-emulator")
    
    // 从 termux-shared 提取需要的工具类（临时方案）
    // 或者依赖整个 termux-shared
    implementation project(":termux-shared")
}
```

#### Step 3: 更新 settings.gradle
```groovy
include ':app', ':termux-core', ':termux-shared', ':terminal-emulator', ':terminal-view'
```

### 5.2 文件迁移阶段

#### Step 4: 迁移核心文件

**从 app 模块迁移（3 个）**:
```bash
# Bootstrap 安装
cp app/src/main/java/com/termux/app/TermuxInstaller.kt \
   termux-core/src/main/java/com/termux/app/

# 命令执行服务
cp app/src/main/java/com/termux/app/RunCommandService.kt \
   termux-core/src/main/java/com/termux/app/

# 核心服务（保持 Java）
cp app/src/main/java/com/termux/app/TermuxService.java \
   termux-core/src/main/java/com/termux/app/
```

**从 terminal-emulator 迁移（1 个）**:
```bash
cp terminal-emulator/src/main/java/com/termux/terminal/JNI.kt \
   termux-core/src/main/java/com/termux/terminal/
```

**从 termux-shared 迁移（22 个）**:
```bash
# 创建目录结构
mkdir -p termux-core/src/main/java/com/termux/shared/{termux,shell}

# 迁移所有核心文件（详细列表见附录）
# ...
```

**迁移 Native 代码**:
```bash
cp app/src/main/cpp/termux-bootstrap.c termux-core/src/main/cpp/
cp app/src/main/cpp/termux-bootstrap-zip.S termux-core/src/main/cpp/
cp app/src/main/cpp/Android.mk termux-core/src/main/cpp/

# 复制 Bootstrap zip 文件
cp app/src/main/cpp/bootstrap-*.zip termux-core/src/main/cpp/
```


### 5.3 依赖处理阶段

#### Step 5: 处理 termux-shared 依赖

**选项 A：完全依赖 termux-shared（简单）**
```groovy
// termux-core/build.gradle
dependencies {
    implementation project(":termux-shared")
}
```
- 优点：简单快速
- 缺点：引入不必要的依赖

**选项 B：提取必要的工具类（推荐）**
将核心文件依赖的工具类也迁移到 termux-core：
- Logger
- FileUtils（部分）
- Error/Errno
- 基础 Android 工具

**选项 C：创建接口层**
在 termux-core 中定义接口，由 termux-shared 实现。

**推荐：先使用选项 A，后续优化为选项 B**。

#### Step 6: 更新模块依赖

**app/build.gradle**:
```groovy
dependencies {
    implementation project(":termux-core")      // 新增
    implementation project(":terminal-view")
    implementation project(":termux-shared")
    // ...
}
```

**termux-core/build.gradle**:
```groovy
dependencies {
    implementation project(":terminal-emulator")
    implementation project(":termux-shared")    // 临时依赖
    // ...
}
```

### 5.4 测试验证阶段

#### Step 7: 编译测试
```bash
./gradlew :termux-core:assembleDebug
./gradlew :app:assembleDebug
```

#### Step 8: 功能测试
- 测试 Bootstrap 安装
- 测试终端会话创建
- 测试后台命令执行
- 测试 AM Socket Server
- 测试 TermuxService 启动

#### Step 9: 清理原模块
确认测试通过后，删除原模块中的已迁移文件：
```bash
# 从 app 删除
rm app/src/main/java/com/termux/app/TermuxInstaller.kt
rm app/src/main/java/com/termux/app/RunCommandService.kt
rm app/src/main/java/com/termux/app/TermuxService.java

# 从 terminal-emulator 删除
rm terminal-emulator/src/main/java/com/termux/terminal/JNI.kt

# 从 termux-shared 删除（22 个文件）
# ...
```

---

## 六、目录结构设计

### 6.1 termux-core 完整目录结构

```
termux-core/
├── build.gradle
├── proguard-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── cpp/
    │   │   ├── Android.mk
    │   │   ├── termux-bootstrap.c
    │   │   ├── termux-bootstrap-zip.S
    │   │   ├── bootstrap-aarch64.zip
    │   │   ├── bootstrap-arm.zip
    │   │   ├── bootstrap-i686.zip
    │   │   └── bootstrap-x86_64.zip
    │   └── java/com/termux/
    │       ├── app/
    │       │   ├── TermuxInstaller.kt          # Bootstrap 安装
    │       │   ├── RunCommandService.kt        # 命令执行服务
    │       │   └── TermuxService.java          # 核心服务
    │       ├── terminal/
    │       │   └── JNI.kt                      # JNI 接口
    │       └── shared/
    │           ├── termux/
    │           │   ├── TermuxBootstrap.kt
    │           │   ├── TermuxConstants.kt
    │           │   └── shell/
    │           │       ├── TermuxShellManager.kt
    │           │       ├── TermuxShellUtils.kt
    │           │       ├── am/
    │           │       │   └── TermuxAmSocketServer.kt
    │           │       └── command/
    │           │           ├── environment/
    │           │           │   ├── TermuxShellEnvironment.kt
    │           │           │   ├── TermuxAppShellEnvironment.kt
    │           │           │   ├── TermuxAPIShellEnvironment.kt
    │           │           │   └── TermuxShellCommandShellEnvironment.kt
    │           │           └── runner/
    │           │               └── terminal/
    │           │                   └── TermuxSession.kt
    │           └── shell/
    │               ├── ShellUtils.kt
    │               ├── ArgumentTokenizer.kt
    │               ├── StreamGobbler.kt
    │               ├── am/
    │               │   └── AmSocketServer.kt
    │               └── command/
    │                   ├── ExecutionCommand.kt
    │                   ├── ShellCommandConstants.kt
    │                   ├── environment/
    │                   │   ├── UnixShellEnvironment.kt
    │                   │   ├── ShellEnvironmentUtils.kt
    │                   │   ├── AndroidShellEnvironment.kt
    │                   │   └── ShellCommandShellEnvironment.kt
    │                   ├── result/
    │                   │   └── ResultData.kt
    │                   └── runner/
    │                       └── app/
    │                           └── AppShell.kt
    └── test/
        └── java/com/termux/core/
            └── (单元测试)
```


---

## 七、风险评估与应对

### 7.1 主要风险

#### 风险 1: TermuxService 迁移问题
**描述**: TermuxService.java 依赖大量 UI 组件，迁移可能导致编译错误。

**应对**:
- 保持 TermuxService 在 app 模块（备选方案）
- 或者将 UI 相关依赖通过接口注入
- 先迁移其他 26 个文件，TermuxService 最后处理

#### 风险 2: 循环依赖
**描述**: termux-core 依赖 termux-shared，但 termux-shared 可能也需要 termux-core 的类。

**应对**:
- 仔细分析依赖关系
- 使用接口解耦
- 必要时调整模块边界

#### 风险 3: Native 代码编译问题
**描述**: Native 代码迁移后可能无法正确编译或链接。

**应对**:
- 保持 Android.mk 配置一致
- 确保 Bootstrap zip 文件路径正确
- 测试所有架构的编译

#### 风险 4: 运行时错误
**描述**: 迁移后可能出现类找不到、资源找不到等运行时错误。

**应对**:
- 充分测试所有核心功能
- 保留原文件作为备份
- 使用 Git 版本控制，便于回滚

### 7.2 回滚方案

如果迁移失败，可以：
1. 使用 Git 回滚到迁移前的提交
2. 删除 termux-core 模块
3. 恢复原模块中的文件
4. 更新 settings.gradle 和依赖配置

---

## 八、优化建议

### 8.1 短期优化（迁移完成后）

1. **添加单元测试**
   - 为核心功能添加单元测试
   - 提高代码质量和可维护性

2. **文档完善**
   - 编写 termux-core 模块的 README
   - 说明模块职责和使用方法

3. **代码审查**
   - 检查是否有遗漏的依赖
   - 优化代码结构

### 8.2 长期优化

1. **减少对 termux-shared 的依赖**
   - 将必要的工具类迁移到 termux-core
   - 或者创建 termux-common 模块存放共享工具

2. **接口化设计**
   - 定义清晰的接口
   - 降低模块间耦合

3. **性能优化**
   - 优化 Bootstrap 安装速度
   - 优化命令执行性能

4. **考虑 TermuxService 的处理**
   - 评估是否需要迁移到 termux-core
   - 或者拆分为核心逻辑和 UI 逻辑

---

## 九、时间估算

### 9.1 工作量评估

| 阶段 | 任务 | 预计时间 |
|------|------|----------|
| 准备 | 创建模块、配置 build.gradle | 1 小时 |
| 迁移 | 迁移 27 个文件 + Native 代码 | 2-3 小时 |
| 依赖 | 处理依赖关系、更新引用 | 2-3 小时 |
| 测试 | 编译测试、功能测试 | 2-3 小时 |
| 清理 | 删除原文件、代码审查 | 1 小时 |
| 文档 | 编写文档、提交代码 | 1 小时 |
| **总计** | | **9-13 小时** |

### 9.2 分阶段实施

**第一阶段（核心验证）**: 2-3 小时
- 创建 termux-core 模块
- 迁移 5-10 个简单文件
- 验证编译和基本功能

**第二阶段（完整迁移）**: 4-6 小时
- 迁移所有 27 个文件
- 处理所有依赖
- 完整功能测试

**第三阶段（优化完善）**: 3-4 小时
- 清理原文件
- 优化依赖关系
- 编写文档

---

## 十、决策建议

### 10.1 是否应该提取核心模块？

**建议：是的，应该提取**

**理由**:
1. ✅ 已有 26 个文件重构完成，是提取的好时机
2. ✅ 核心功能边界清晰，职责明确
3. ✅ 有利于代码维护和测试
4. ✅ 为未来的模块化开发打基础
5. ✅ 降低 app 模块的复杂度

**但需要注意**:
1. ⚠️ TermuxService 的处理需要特别小心
2. ⚠️ 需要充分测试，确保不影响现有功能
3. ⚠️ 需要处理好依赖关系

### 10.2 推荐实施方案

**推荐：方案 B（保持包名） + 分阶段实施**

1. **第一步**: 创建 termux-core 模块，迁移除 TermuxService 外的 26 个文件
2. **第二步**: 测试验证，确保功能正常
3. **第三步**: 评估 TermuxService 是否迁移
4. **第四步**: 优化依赖关系，减少对 termux-shared 的依赖

---

## 十一、附录

### 11.1 完整文件迁移清单

#### 从 app 迁移（3 个）
- [ ] `app/src/main/java/com/termux/app/TermuxInstaller.kt`
- [ ] `app/src/main/java/com/termux/app/RunCommandService.kt`
- [ ] `app/src/main/java/com/termux/app/TermuxService.java`

#### 从 terminal-emulator 迁移（1 个）
- [ ] `terminal-emulator/src/main/java/com/termux/terminal/JNI.kt`

#### 从 termux-shared 迁移（22 个）
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/TermuxBootstrap.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/shell/TermuxShellManager.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/shell/TermuxShellUtils.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/shell/am/TermuxAmSocketServer.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxShellEnvironment.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxAppShellEnvironment.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxAPIShellEnvironment.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxShellCommandShellEnvironment.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/termux/shell/command/runner/terminal/TermuxSession.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/ShellUtils.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/ArgumentTokenizer.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/StreamGobbler.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/am/AmSocketServer.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/command/ExecutionCommand.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/command/ShellCommandConstants.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/command/environment/UnixShellEnvironment.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/command/environment/ShellEnvironmentUtils.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/command/environment/AndroidShellEnvironment.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/command/environment/ShellCommandShellEnvironment.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/command/result/ResultData.kt`
- [ ] `termux-shared/src/main/java/com/termux/shared/shell/command/runner/app/AppShell.kt`

#### Native 代码（2 个 + Bootstrap 文件）
- [ ] `app/src/main/cpp/termux-bootstrap.c`
- [ ] `app/src/main/cpp/termux-bootstrap-zip.S`
- [ ] `app/src/main/cpp/Android.mk`
- [ ] `app/src/main/cpp/bootstrap-aarch64.zip`
- [ ] `app/src/main/cpp/bootstrap-arm.zip`
- [ ] `app/src/main/cpp/bootstrap-i686.zip`
- [ ] `app/src/main/cpp/bootstrap-x86_64.zip`

### 11.2 相关文档
- `Termux核心Linux功能实现代码文件.md` - 核心功能详细说明
- `Termux项目Java文件分类与重构状态.md` - 文件分类和重构状态
- `不能重构的Java文件记录.md` - TermuxService 问题记录

---

**方案版本**: v1.0  
**创建日期**: 2026年3月9日  
**作者**: Kiro AI Assistant  
**状态**: 待审核

