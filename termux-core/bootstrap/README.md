# Bootstrap 模块

Termux Bootstrap 功能的 Native 实现。

## 功能

提供 Termux Linux 环境初始化所需的 Bootstrap 数据和 Native 接口。

## 模块信息

- **命名空间**: `com.termux.core.bootstrap`
- **类型**: Android Library (AAR)
- **Native 库**: `libtermux-bootstrap.so`

## 文件结构

```
bootstrap/
├── build.gradle
├── proguard-rules.pro
├── README.md
└── src/main/
    ├── AndroidManifest.xml
    └── cpp/
        ├── Android.mk              # NDK 构建配置
        ├── termux-bootstrap.c      # C 代码
        ├── termux-bootstrap-zip.S  # 汇编代码（嵌入 zip 数据）
        ├── bootstrap-aarch64.zip   # ARM64 Bootstrap
        ├── bootstrap-arm.zip       # ARM Bootstrap
        ├── bootstrap-i686.zip      # x86 Bootstrap
        └── bootstrap-x86_64.zip    # x86_64 Bootstrap
```

## Native 代码

### termux-bootstrap.c

提供 JNI 接口，用于获取嵌入的 Bootstrap zip 数据。

**JNI 方法**:
```c
JNIEXPORT jbyteArray JNICALL
Java_com_termux_app_TermuxInstaller_getZip(JNIEnv *env, jclass clazz);
```

### termux-bootstrap-zip.S

汇编代码，使用 `.incbin` 指令将 Bootstrap zip 文件嵌入到共享库中。

```asm
.global zip_start
.global zip_end

zip_start:
    .incbin "bootstrap-aarch64.zip"  // 根据架构选择
zip_end:
```

## 支持的架构

| 架构 | ABI | Bootstrap 文件 | 库大小 |
|------|-----|----------------|--------|
| ARM 64位 | arm64-v8a | bootstrap-aarch64.zip | ~29 MB |
| ARM 32位 | armeabi-v7a | bootstrap-arm.zip | ~26 MB |
| Intel 32位 | x86 | bootstrap-i686.zip | ~28 MB |
| Intel 64位 | x86_64 | bootstrap-x86_64.zip | ~29 MB |

## 使用方法

### 1. 添加依赖

在 `build.gradle` 中：

```groovy
dependencies {
    implementation project(":termux-core:bootstrap")
}
```

### 2. 加载 Native 库

在 Java/Kotlin 代码中：

```kotlin
class TermuxInstaller {
    companion object {
        fun loadZipBytes(): ByteArray {
            System.loadLibrary("termux-bootstrap")
            return getZip()
        }
        
        @JvmStatic
        private external fun getZip(): ByteArray
    }
}
```

### 3. 使用 Bootstrap 数据

```kotlin
val zipBytes = TermuxInstaller.loadZipBytes()
// 解压 zip 数据到 $PREFIX 目录
```

## 构建

### 构建 Debug 版本

```bash
./gradlew :termux-core:bootstrap:assembleDebug
```

### 构建 Release 版本

```bash
./gradlew :termux-core:bootstrap:assembleRelease
```

### 清理

```bash
./gradlew :termux-core:bootstrap:clean
```

## 输出

构建后会生成：

```
bootstrap/build/outputs/aar/
└── bootstrap-debug.aar  (或 bootstrap-release.aar)
```

AAR 文件包含：
- `classes.jar` - Java/Kotlin 类（如果有）
- `jni/` - Native 库（所有架构）
  - `arm64-v8a/libtermux-bootstrap.so`
  - `armeabi-v7a/libtermux-bootstrap.so`
  - `x86/libtermux-bootstrap.so`
  - `x86_64/libtermux-bootstrap.so`
- `AndroidManifest.xml`

## 依赖

```groovy
dependencies {
    implementation "androidx.annotation:annotation:1.9.0"
    implementation "androidx.core:core:1.13.1"
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    
    // Termux 模块
    implementation project(":terminal-emulator")
    implementation project(":terminal-view")
    implementation project(":termux-shared")
    
    // 第三方库
    implementation "com.google.guava:guava:24.1-jre"
    implementation "commons-io:commons-io:2.5"
    implementation "com.termux:termux-am-library:v2.0.0"
}
```

## Bootstrap 文件来源

Bootstrap zip 文件来自 Termux 官方：
- **仓库**: https://github.com/termux/termux-packages
- **版本**: 2026.02.12-r1+apt.android-7
- **下载**: https://github.com/termux/termux-packages/releases

## 更新 Bootstrap

如果需要更新 Bootstrap 文件：

1. 从官方下载新版本的 Bootstrap zip 文件
2. 替换 `src/main/cpp/bootstrap-*.zip` 文件
3. 重新构建模块

## 技术细节

### 为什么使用汇编嵌入数据？

使用汇编的 `.incbin` 指令可以直接将二进制文件嵌入到共享库中，优势：
- ✅ 数据直接在内存中，访问速度快
- ✅ 不需要从文件系统读取
- ✅ 数据随库一起打包，不会丢失

### 内存占用

Bootstrap 数据会占用约 26-29 MB 的内存（根据架构），但这是必要的，因为：
- 只在安装时加载一次
- 安装完成后可以释放
- 相比从网络下载，速度更快

## 故障排除

### 问题 1: 找不到 libtermux-bootstrap.so

**原因**: Native 库没有正确打包到 APK

**解决**:
1. 检查 `build.gradle` 中的 `externalNativeBuild` 配置
2. 清理并重新构建：`./gradlew clean assembleDebug`
3. 检查 APK 中是否包含库：解压 APK 查看 `lib/` 目录

### 问题 2: UnsatisfiedLinkError

**原因**: JNI 方法签名不匹配

**解决**:
1. 确保 Java/Kotlin 中的 native 方法声明正确
2. 确保 C 代码中的 JNI 函数名正确
3. 使用 `javah` 生成正确的头文件

### 问题 3: Bootstrap 数据损坏

**原因**: zip 文件在嵌入过程中损坏

**解决**:
1. 验证原始 zip 文件的完整性
2. 检查 `.incbin` 路径是否正确
3. 重新下载 Bootstrap 文件

## 相关文档

- [Termux Core README](../README.md)
- [Termux核心模块独立成功报告.md](../../Termux核心模块独立成功报告.md)

---

**版本**: v1.0.0  
**最后更新**: 2026年3月9日
