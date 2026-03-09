# Termux 核心模块提取遇到的问题

## 问题描述

在尝试将核心文件从 app、terminal-emulator 和 termux-shared 模块提取到独立的 termux-core 模块时，遇到了严重的循环依赖问题。

## 问题根源

### 当前依赖关系

```
termux-core
 ├── terminal-emulator
 ├── terminal-view
 └── termux-shared  ❌ 问题：termux-core 依赖 termux-shared

termux-shared
 ├── terminal-view
 └── termux-core    ❌ 问题：termux-shared 也依赖 termux-core
```

这形成了循环依赖：
- termux-core 需要 termux-shared 的工具类（Logger, FileUtils, Error 等）
- termux-shared 需要 termux-core 的核心类（TermuxConstants, ExecutionCommand, ResultData 等）

### 具体表现

1. **JNI.kt 的问题**
   - JNI.kt 最初被迁移到 termux-core
   - 但 terminal-emulator 的 TerminalSession.java 需要使用 JNI 类
   - 如果让 terminal-emulator 依赖 termux-core，会形成循环：
     - termux-core → terminal-emulator (依赖)
     - terminal-emulator → termux-core (需要 JNI)

2. **termux-shared 的问题**
   - termux-shared 中的很多类依赖已迁移的核心类：
     - TermuxFileUtils 需要 TermuxConstants, ExecutionCommand, AppShell
     - TermuxPluginUtils 需要 ExecutionCommand, ResultData, ShellUtils
     - PhantomProcessUtils 需要 ExecutionCommand, AppShell, AndroidShellEnvironment
     - ResultSender 需要 ResultData, ShellCommandConstants
     - TermuxCrashUtils 需要 TermuxConstants
     - TermuxUtils 需要 TermuxConstants, ExecutionCommand, AppShell
     - 等等...

## 编译错误统计

删除原模块文件后编译，出现 100+ 个错误：
- termux-shared 找不到 TermuxConstants (30+ 处)
- termux-shared 找不到 ExecutionCommand (20+ 处)
- termux-shared 找不到 ResultData (10+ 处)
- termux-shared 找不到 AppShell (5+ 处)
- termux-shared 找不到 ShellUtils, AndroidShellEnvironment 等

## 解决方案探讨

### 方案 1：保持 JNI.kt 在 terminal-emulator ✅ (已实施)

**做法**：
- 将 JNI.kt 还原到 terminal-emulator 模块
- 从 termux-core 删除 JNI.kt

**理由**：
- JNI.kt 加载的是 "termux" 库，这个库在 terminal-emulator 中编译
- JNI.kt 是终端模拟器的核心接口，应该属于 terminal-emulator
- 避免 terminal-emulator 和 termux-core 之间的循环依赖

**结果**：
- ✅ 解决了 terminal-emulator 的编译问题
- ❌ 但 termux-shared 的问题依然存在

### 方案 2：不迁移 termux-shared 中的文件 ❌ (不可行)

**做法**：
- 只迁移 app 模块的文件（TermuxInstaller.kt）
- 保持 termux-shared 中的 22 个文件不动

**问题**：
- 违背了提取核心模块的初衷
- 核心功能仍然分散在多个模块中
- 没有实现模块化的目标

### 方案 3：重新设计模块依赖关系 ⭐ (推荐)

**核心思想**：打破循环依赖

#### 3.1 创建 termux-common 模块

```
termux-common (基础工具类)
 ├── Logger
 ├── FileUtils
 ├── Error/Errno
 ├── AndroidUtils
 └── 其他通用工具

termux-core (核心功能)
 ├── termux-common
 ├── terminal-emulator
 └── terminal-view
 包含：
 - TermuxConstants
 - ExecutionCommand
 - ResultData
 - AppShell
 - TermuxSession
 - Shell 环境配置
 - 等等

termux-shared (共享功能)
 ├── termux-common
 └── termux-core
 包含：
 - TermuxFileUtils
 - TermuxPluginUtils
 - TermuxCrashUtils
 - 等等

app
 ├── termux-core
 ├── termux-shared
 ├── terminal-view
 └── terminal-emulator
```

**优点**：
- 清晰的依赖层次：common → core → shared → app
- 没有循环依赖
- 核心功能独立
- 工具类可复用

**缺点**：
- 需要创建新模块 termux-common
- 需要大量重构 termux-shared
- 工作量较大

#### 3.2 termux-core 不依赖 termux-shared

**做法**：
- 将 termux-core 需要的工具类也迁移到 termux-core
- 或者复制一份到 termux-core
- termux-core 完全独立，不依赖 termux-shared

**优点**：
- termux-core 真正独立
- 没有循环依赖

**缺点**：
- 代码重复
- 维护成本高
- 违背 DRY 原则

#### 3.3 保持当前结构，只提取 app 模块的文件

**做法**：
- 只将 app/TermuxInstaller.kt 迁移到 termux-core
- termux-shared 中的文件保持不动
- termux-core 依赖 termux-shared

**优点**：
- 改动最小
- 风险最低
- 不会出现循环依赖

**缺点**：
- 核心功能仍然分散
- 没有完全实现模块化目标

### 方案 4：将 termux-shared 拆分 ⭐⭐ (可行)

**做法**：
1. 将 termux-shared 拆分为两部分：
   - termux-shared-base: 基础工具类（不依赖核心类）
   - termux-shared-ext: 扩展功能（依赖核心类）

2. 依赖关系：
```
termux-shared-base (基础工具)
 ├── Logger
 ├── FileUtils
 ├── Error
 └── AndroidUtils

termux-core (核心功能)
 ├── termux-shared-base
 ├── terminal-emulator
 └── terminal-view

termux-shared-ext (扩展功能)
 ├── termux-shared-base
 └── termux-core

app
 ├── termux-core
 ├── termux-shared-ext
 └── ...
```

**优点**：
- 打破循环依赖
- 核心功能独立
- 保持代码复用

**缺点**：
- 需要拆分 termux-shared
- 需要更新所有引用
- 工作量中等

## 当前状态

### 已完成
- ✅ 创建了 termux-core 模块
- ✅ 迁移了 25 个核心文件到 termux-core
- ✅ 修改了 R 类引用
- ✅ 创建了资源文件
- ✅ termux-core 模块单独编译成功
- ✅ 将 JNI.kt 还原到 terminal-emulator

### 当前问题
- ❌ 删除原模块文件后出现循环依赖
- ❌ termux-shared 找不到已迁移的核心类
- ❌ 整个项目无法编译

### 文件状态
- 原模块文件已删除（24 个文件）
- termux-core 中有完整的文件副本
- JNI.kt 已还原到 terminal-emulator
- 可以通过 git 回滚

## 建议的下一步行动

### 短期方案（快速修复）

1. **回滚删除操作**
   ```bash
   git restore app/src/main/java/com/termux/app/TermuxInstaller.kt
   git restore termux-shared/src/main/java/com/termux/shared/
   ```

2. **保留 termux-core 模块**
   - 作为未来的目标架构
   - 暂时不删除原模块的文件

3. **更新依赖关系**
   - 移除 termux-shared 对 termux-core 的依赖
   - 保持 termux-core 依赖 termux-shared

4. **文档化问题**
   - 记录循环依赖问题
   - 说明需要重构的原因

### 长期方案（彻底解决）

选择方案 4：拆分 termux-shared

1. **第一阶段**：分析依赖
   - 分析 termux-shared 中哪些类是基础工具
   - 分析哪些类依赖核心功能

2. **第二阶段**：创建 termux-shared-base
   - 迁移基础工具类
   - 确保不依赖核心类

3. **第三阶段**：重构 termux-core
   - 依赖 termux-shared-base
   - 不依赖 termux-shared-ext

4. **第四阶段**：创建 termux-shared-ext
   - 迁移扩展功能
   - 依赖 termux-core

5. **第五阶段**：更新 app 模块
   - 更新依赖配置
   - 测试所有功能

## 时间估算

### 短期方案
- 回滚和修复：30 分钟
- 文档化：30 分钟
- 总计：1 小时

### 长期方案
- 依赖分析：2-3 小时
- 创建 termux-shared-base：3-4 小时
- 重构 termux-core：2-3 小时
- 创建 termux-shared-ext：2-3 小时
- 测试和修复：3-4 小时
- 总计：12-17 小时

## 结论

当前的模块提取遇到了架构层面的问题，不能简单地通过移动文件来解决。需要重新设计模块依赖关系，打破循环依赖。

建议：
1. 短期：回滚删除操作，保留 termux-core 作为目标架构
2. 长期：实施方案 4，拆分 termux-shared 模块

---

**报告时间**: 2026年3月9日  
**状态**: ❌ 遇到循环依赖问题，需要重新设计  
**建议**: 回滚并重新规划架构
