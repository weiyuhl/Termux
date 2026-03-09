## 一、当前架构

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

**主要功能包结构**：
- **activities/**：通用 Activity（ReportActivity、TextIOActivity）
- **activity/**：Activity 工具类和错误码（ActivityUtils、ActivityErrno）
- **android/**：Android 系统工具
  - AndroidUtils：系统信息获取
  - PackageUtils：包管理
  - PermissionUtils：权限检查
  - PhantomProcessUtils：幽灵进程管理
  - ProcessUtils：进程工具
  - SELinuxUtils：SELinux 相关
  - SettingsProviderUtils：系统设置访问
  - UserUtils：用户信息
- **crash/**：崩溃处理（CrashHandler）
- **data/**：数据处理（DataUtils、IntentUtils）
- **errors/**：错误处理（Error、Errno、FunctionErrno）
- **file/**：文件操作（FileUtils、FileUtilsErrno、filesystem/、tests/）
- **interact/**：用户交互（MessageDialogUtils、ShareUtils）
- **jni/**：JNI 相关模型
- **logger/**：日志系统（Logger）
- **markdown/**：Markdown 渲染（MarkdownUtils）
- **models/**：数据模型（ReportInfo、TextIOInfo）
- **net/**：网络工具
  - socket/：Socket 通信
  - uri/：URI 处理
  - url/：URL 工具
- **notification/**：通知管理（NotificationUtils）
- **reflection/**：反射工具（ReflectionUtils）
- **settings/**：设置管理
  - preferences/：SharedPreferences 封装
  - properties/：配置文件读取
- **shell/**：Shell 管理
  - ArgumentTokenizer：参数解析
  - ShellUtils：Shell 工具
  - StreamGobbler：流处理
  - am/：Activity Manager 相关
  - command/：命令执行
- **termux/**：Termux 特有功能
  - TermuxBootstrap：Bootstrap 管理
  - TermuxConstants：常量定义
  - TermuxUtils：Termux 工具
  - crash/：Termux 崩溃处理
  - data/：Termux 数据处理
  - extrakeys/：额外按键配置
  - file/：Termux 文件管理
  - interact/：Termux 交互
  - models/：Termux 数据模型
  - notification/：Termux 通知
  - plugins/：插件支持
  - settings/：Termux 设置
  - shell/：Termux Shell
  - terminal/：终端相关
  - theme/：主题管理
- **theme/**：主题工具（ThemeUtils、NightMode）
- **view/**：视图工具（ViewUtils、KeyboardUtils）

**Native 代码**：
- local-socket.cpp：本地 Socket 通信实现

**资源文件**：
- 布局文件：报告界面、文本输入输出界面、对话框、Markdown 渲染
- 菜单文件：报告菜单、文本输入输出菜单
- 原始资源：apt_info_script.sh、bell.ogg（提示音）

**外部依赖**：
- androidx 库：appcompat、core、material
- Markwon：Markdown 渲染库
- Guava：Google 核心库
- commons-io：Apache 文件操作库
- termux-am-library：Termux Activity Manager 库
- lsposed.hiddenapibypass：隐藏 API 访问

**依赖关系**：
- 依赖 terminal-view 模块

**特点**：
- 无业务逻辑，纯工具类
- 被所有其他模块依赖
- 提供 Termux 特有的工具方法
- 包含 Native 代码实现本地 Socket 通信

---

#### terminal-emulator（终端模拟器）
**职责**：提供终端仿真核心功能，处理终端协议和字符渲染

**主要类文件**：
- **TerminalSession**：终端会话管理，创建、管理、销毁会话
- **TerminalEmulator**：终端协议处理，解析 VT100/ANSI 转义序列
- **TerminalBuffer**：字符缓冲区，管理屏幕缓冲区和历史记录
- **TerminalRow**：终端行数据结构
- **TerminalOutput**：输入输出处理，处理键盘输入和输出流
- **TerminalSessionClient**：会话客户端接口
- **KeyHandler**：键盘事件处理
- **TextStyle**：文本样式，颜色管理和 ANSI 颜色解析
- **TerminalColors**：终端颜色定义
- **TerminalColorScheme**：颜色方案管理
- **WcWidth**：字符宽度计算，UTF-8 编码处理
- **ByteQueue**：字节队列，用于数据缓冲
- **Logger**：日志记录
- **JNI**：JNI 接口定义

**Native 代码**：
- termux.c：Native 层实现，与底层 Shell 进程通信

**依赖关系**：
- 无外部模块依赖，仅依赖 Android 基础库

**特点**：
- 纯逻辑层，不涉及 UI
- 实现标准终端协议
- 与 Native 层（JNI）交互
- 独立模块，可被其他项目复用

---

#### terminal-view（终端视图）
**职责**：提供终端的 UI 展示和用户交互

**主要类文件**：
- **TerminalView**：终端视图主类，负责渲染、触摸处理、滚动、文本选择等
- **TerminalRenderer**：终端渲染器，字符绘制、颜色渲染、字体管理
- **TerminalViewClient**：视图客户端接口
- **GestureAndScaleRecognizer**：手势和缩放识别器
- **support/**：兼容性支持类
  - PopupWindowCompatGingerbread：弹出窗口兼容实现
- **textselection/**：文本选择相关
  - CursorController：光标控制器接口
  - TextSelectionCursorController：文本选择光标控制器
  - TextSelectionHandleView：文本选择手柄视图

**资源文件**：
- 文本选择手柄图标（左右手柄）

**依赖关系**：
- 依赖 terminal-emulator 模块（api 依赖）

**特点**：
- 纯 UI 层，继承自 View
- 依赖 terminal-emulator 提供的数据
- 处理所有用户交互

---

#### app（主应用模块）
**职责**：提供 Termux 应用的 UI 界面和业务逻辑整合

**主要组件**：
- **TermuxActivity**：主界面 Activity，管理终端视图和用户交互
- **TermuxService**：后台服务，管理终端会话生命周期
- **RunCommandService**：执行命令服务，处理外部命令执行请求
- **TermuxApplication**：应用程序入口，初始化全局配置
- **TermuxInstaller**：Bootstrap 安装器，负责首次安装和更新
- **TermuxOpenReceiver**：广播接收器，处理应用启动事件
- **SystemEventReceiver**：系统事件接收器，监听系统广播

**子包结构**：
- **activities/**：设置界面（SettingsActivity）、帮助界面（HelpActivity）
- **api/file/**：文件接收功能（FileReceiverActivity）
- **fragments/settings/**：各种设置 Fragment（Termux、API、Float、Tasker、Widget）
- **terminal/**：终端相关客户端实现
  - TermuxTerminalSessionActivityClient：Activity 端会话客户端
  - TermuxTerminalSessionServiceClient：Service 端会话客户端
  - TermuxTerminalViewClient：终端视图客户端
  - TermuxSessionsListViewController：会话列表控制器
  - TermuxActivityRootView：根视图，处理软键盘
- **terminal/io/**：输入输出相关
  - TermuxTerminalExtraKeys：额外按键实现
  - KeyboardShortcut：键盘快捷键
  - TerminalToolbarViewPager：工具栏 ViewPager
  - FullScreenWorkAround：全屏模式修复
- **models/**：数据模型（UserAction）
- **filepicker/**：文档提供器（TermuxDocumentsProvider）

**外部依赖**：
- androidx 库：core、drawerlayout、preference、viewpager
- Material Design 组件
- Markwon：Markdown 渲染库
- Guava：Google 核心库

**依赖关系**：
- 依赖 termux-core:bootstrap 模块
- 依赖 terminal-view 模块
- 依赖 termux-shared 模块

**特点**：
- 混合了 UI 和业务逻辑
- 直接依赖所有其他模块
- 包含 Kotlin 和 Java 混合代码
- 应用入口，整合所有功能

---

#### termux-core/bootstrap（Bootstrap 模块）
**职责**：提供 Termux 运行环境的 Bootstrap 文件打包和安装

**主要内容**：
- **Native 代码**：termux-bootstrap.c 和 termux-bootstrap-zip.S
- **Bootstrap 压缩包**：包含多架构支持
  - bootstrap-aarch64.zip（ARM64）
  - bootstrap-arm.zip（ARM32）
  - bootstrap-i686.zip（x86 32位）
  - bootstrap-x86_64.zip（x86 64位）

**外部依赖**：
- androidx 库：core、drawerlayout、material
- Guava、commons-io
- termux-am-library

**依赖关系**：
- 依赖 terminal-emulator 模块
- 依赖 terminal-view 模块
- 依赖 termux-shared 模块

**特点**：
- 纯 Native 实现
- 包含预编译的 Linux 工具链
- 支持多种 CPU 架构
- 负责 Termux 运行环境的初始化

---

### 模块依赖关系图

```
app
├── termux-core:bootstrap
│   ├── terminal-emulator
│   ├── terminal-view
│   │   └── terminal-emulator
│   └── termux-shared
│       └── terminal-view
├── terminal-view
│   └── terminal-emulator
└── termux-shared
    └── terminal-view
```

**依赖层级**（从底层到上层）：
1. terminal-emulator（最底层，无依赖）
2. terminal-view（依赖 terminal-emulator）
3. termux-shared（依赖 terminal-view）
4. termux-core:bootstrap（依赖 terminal-emulator、terminal-view、termux-shared）
5. app（依赖所有模块）

**关键特点**：
- terminal-emulator 是最底层的独立模块
- terminal-view 只依赖 terminal-emulator
- termux-shared 依赖 terminal-view，提供通用工具
- app 和 bootstrap 模块依赖所有其他模块
