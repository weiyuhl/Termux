# Termux 模块化 + MVVM 重构方案

## 一、当前架构问题

### 现有结构
```
termux-app/
├── app/                    # 主应用（UI + 业务逻辑混合）
├── termux-core/           # 核心模块
│   └── bootstrap/         # Bootstrap 功能
├── termux-shared/         # 共享工具库
├── terminal-emulator/     # 终端模拟器
└── terminal-view/         # 终端视图
```

### 现有模块详细说明

#### termux-shared（共享工具库）
**职责**：提供跨模块的通用工具类和常量定义

**主要功能**：
- **文件操作**：文件读写、权限管理、路径处理（FileUtils）
- **Android 工具**：包名管理、权限检查、系统信息获取（AndroidUtils、PackageUtils）
- **日志系统**：统一的日志记录和输出（Logger）
- **错误处理**：错误码定义、错误信息格式化（Error、Errno）
- **Shell 管理**：Shell 命令执行、环境变量管理（ShellUtils、TermuxShellEnvironment）
- **设置管理**：SharedPreferences 封装、配置文件读取（TermuxAppSharedPreferences）
- **网络工具**：URL 处理、URI 解析、Socket 通信（UriUtils、LocalSocketManager）
- **Markdown 渲染**：Markdown 文本解析和显示（MarkdownUtils）
- **通知管理**：通知创建和显示（NotificationUtils、TermuxNotificationUtils）
- **主题管理**：主题切换、夜间模式（ThemeUtils、NightMode）
- **常量定义**：包名、路径、权限等常量（TermuxConstants）
- **数据处理**：数据转换、格式化（DataUtils）
- **交互工具**：对话框、Toast、分享（MessageDialogUtils、ShareUtils）

**特点**：
- 无业务逻辑，纯工具类
- 被所有其他模块依赖
- 提供 Termux 特有的工具方法

---

#### terminal-emulator（终端模拟器）
**职责**：提供终端仿真核心功能，处理终端协议和字符渲染

**主要功能**：
- **终端会话管理**：创建、管理、销毁终端会话（TerminalSession）
- **终端协议处理**：解析 VT100/ANSI 转义序列（TerminalEmulator）
- **字符缓冲区**：管理终端屏幕缓冲区、历史记录（TerminalBuffer）
- **输入输出处理**：处理键盘输入、鼠标事件、输出流（TerminalOutput）
- **字符编码**：UTF-8 编码处理、字符宽度计算（WcWidth）
- **颜色管理**：ANSI 颜色解析、256 色支持（TextStyle）
- **光标控制**：光标位置、样式、闪烁（TerminalEmulator）
- **滚动控制**：屏幕滚动、历史记录滚动（TerminalBuffer）
- **文本选择**：文本选择、复制功能（TerminalSession）
- **窗口大小调整**：动态调整终端窗口大小（TerminalSession）
- **进程管理**：与底层 Shell 进程通信（TerminalSession）

**特点**：
- 纯逻辑层，不涉及 UI
- 实现标准终端协议
- 与 Native 层（JNI）交互

---

#### terminal-view（终端视图）
**职责**：提供终端的 UI 展示和用户交互

**主要功能**：
- **终端渲染**：将终端缓冲区内容渲染到屏幕（TerminalView）
- **文本绘制**：字符绘制、颜色渲染、字体管理（TerminalRenderer）
- **触摸处理**：触摸事件处理、手势识别（TerminalView）
- **滚动处理**：滚动条显示、滚动事件处理（TerminalView）
- **文本选择 UI**：选择手柄、选择高亮显示（TerminalView）
- **额外按键**：额外按键栏显示和交互（ExtraKeysView）
- **自动补全**：命令自动补全 UI（TerminalView）
- **长按菜单**：长按显示上下文菜单（TerminalView）
- **缩放支持**：双指缩放字体大小（TerminalView）
- **性能优化**：脏区域刷新、硬件加速（TerminalRenderer）
- **可访问性**：屏幕阅读器支持（TerminalView）

**特点**：
- 纯 UI 层，继承自 View
- 依赖 terminal-emulator 提供的数据
- 处理所有用户交互

### 主要问题
1. **紧耦合**：Activity 直接操作 Service，业务逻辑分散在各处
2. **无架构模式**：没有 ViewModel，数据和 UI 混在一起
3. **难以测试**：业务逻辑在 Activity/Service 中，无法进行单元测试
4. **模块职责不清**：app 模块过于臃肿，承担了太多职责
5. **代码复用困难**：业务逻辑和 UI 绑定，无法在其他地方复用

---

## 二、目标架构：Clean Architecture + MVVM

### 架构分层

```
┌─────────────────────────────────────────────────────┐
│              Presentation Layer (表现层)             │
│        Activities, Fragments, ViewModels            │
│              负责：UI 展示和用户交互                  │
└─────────────────────────────────────────────────────┘
                        ↓ ↑
┌─────────────────────────────────────────────────────┐
│               Domain Layer (领域层)                  │
│          UseCases, Repository 接口, 领域模型         │
│         负责：纯业务逻辑，不依赖任何框架              │
└─────────────────────────────────────────────────────┘
                        ↓ ↑
┌─────────────────────────────────────────────────────┐
│                Data Layer (数据层)                   │
│       Repository 实现, DataSource, Mapper            │
│        负责：数据访问，与外部系统交互                 │
└─────────────────────────────────────────────────────┘
                        ↓ ↑
┌─────────────────────────────────────────────────────┐
│               Core Layer (核心层)                    │
│          Native 功能, 底层实现, 工具类               │
│         负责：底层功能实现，与系统交互                │
└─────────────────────────────────────────────────────┘
```

### 依赖规则
- **单向依赖**：外层依赖内层，内层不知道外层的存在
- **Domain 层**：纯 Kotlin，不依赖 Android 框架
- **Data 层**：实现 Domain 层定义的接口
- **Presentation 层**：只依赖 Domain 层，通过接口与 Data 层交互
