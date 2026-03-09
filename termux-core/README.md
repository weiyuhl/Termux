# Termux Core

Termux 核心模块集合，包含各种独立的功能模块。

## 模块结构

```
termux-core/
├── bootstrap/              # Bootstrap 模块（Linux 环境初始化）
│   ├── build.gradle
│   └── src/
│       └── main/
│           └── cpp/        # Native 代码
│               ├── termux-bootstrap.c
│               ├── termux-bootstrap-zip.S
│               └── bootstrap-*.zip (4 个架构)
├── build.gradle            # 主模块配置（容器）
└── settings.gradle         # 子模块配置
```

## 子模块说明

### bootstrap

**功能**: 提供 Termux Bootstrap 功能的 Native 实现

**职责**:
- 编译 `libtermux-bootstrap.so` Native 库
- 嵌入 Bootstrap zip 数据（4 个架构）
- 提供 JNI 接口供 Java/Kotlin 代码调用

**支持的架构**:
- ARM64 (aarch64)
- ARM (armeabi-v7a)
- x86
- x86_64

**依赖**:
```groovy
implementation project(":termux-core:bootstrap")
```

**使用**:
```kotlin
// 在 TermuxInstaller.kt 中
System.loadLibrary("termux-bootstrap")
val zipBytes = getZip()  // 调用 Native 方法
```

---

## 未来计划的子模块

### vim (计划中)

**功能**: Vim 编辑器

**结构**:
```
termux-core/vim/
├── build.gradle
└── src/main/cpp/
    └── vim 源代码
```

### python (计划中)

**功能**: Python 解释器

**结构**:
```
termux-core/python/
├── build.gradle
└── src/main/cpp/
    └── python 源代码
```

### nodejs (计划中)

**功能**: Node.js 运行时

### git (计划中)

**功能**: Git 版本控制工具

### 其他工具

可以根据需要添加更多独立的工具模块。

---

## 添加新子模块

### 步骤 1: 创建子模块目录

```bash
mkdir termux-core/your-module
```

### 步骤 2: 创建 build.gradle

```groovy
plugins {
    id "com.android.library"
    id "kotlin-android"
}

android {
    namespace "com.termux.core.yourmodule"
    
    compileSdkVersion project.properties.compileSdkVersion.toInteger()
    ndkVersion = System.getenv("JITPACK_NDK_VERSION") ?: project.properties.ndkVersion
    
    defaultConfig {
        minSdkVersion project.properties.minSdkVersion.toInteger()
        targetSdkVersion project.properties.targetSdkVersion.toInteger()
    }
    
    // 如果有 Native 代码
    externalNativeBuild {
        ndkBuild {
            path "src/main/cpp/Android.mk"
        }
    }
}

dependencies {
    // 添加依赖
}
```

### 步骤 3: 更新 termux-core/settings.gradle

```groovy
include ':bootstrap'
include ':your-module'  // 添加新模块
```

### 步骤 4: 更新根项目 settings.gradle

```groovy
include ':termux-core:your-module'
```

### 步骤 5: 在需要的地方添加依赖

```groovy
dependencies {
    implementation project(":termux-core:your-module")
}
```

---

## 模块设计原则

### 1. 独立性

每个子模块应该是独立的，可以单独编译和测试。

### 2. 职责单一

每个子模块只负责一个特定的功能或工具。

### 3. 最小依赖

尽量减少子模块之间的依赖，避免循环依赖。

### 4. Native 优先

对于需要编译的工具（如 vim, python），使用 Native 代码实现。

### 5. 标准化

所有子模块使用统一的命名空间：`com.termux.core.<module-name>`

---

## 构建

### 构建所有模块

```bash
./gradlew :termux-core:bootstrap:assembleDebug
```

### 构建特定子模块

```bash
./gradlew :termux-core:bootstrap:assembleDebug
./gradlew :termux-core:vim:assembleDebug
```

### 清理

```bash
./gradlew :termux-core:clean
```

---

## 依赖关系

```
app
 └── termux-core:bootstrap (获取 Native 库)

termux-core (容器模块)
 ├── bootstrap (子模块)
 ├── vim (子模块，未来)
 └── python (子模块，未来)
```

---

## 优势

### 1. 模块化清晰

每个工具都是独立的模块，便于管理和维护。

### 2. 按需依赖

应用可以选择性地依赖需要的模块，减少 APK 大小。

### 3. 独立开发

不同的工具可以由不同的开发者独立开发。

### 4. 版本管理

每个模块可以有自己的版本号。

### 5. 易于扩展

添加新工具只需要创建新的子模块。

---

## 相关文档

- [Bootstrap 模块说明](./bootstrap/README.md)
- [Termux核心模块独立成功报告.md](../Termux核心模块独立成功报告.md)
- [Maven依赖说明.md](../Maven依赖说明.md)

---

**版本**: v1.0.0  
**最后更新**: 2026年3月9日
