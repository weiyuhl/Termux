# TermuxInstaller.java 到 TermuxInstaller.kt 重构对比说明

## 文件位置
- **原始文件**: `app/src/main/java/com/termux/app/TermuxInstaller.java`
- **Kotlin 版本**: `app/src/main/java/com/termux/app/TermuxInstaller.kt`

## 主要改进和差异

### 1. 类结构变化

#### Java 版本
```java
final class TermuxInstaller {
    private static final String LOG_TAG = "TermuxInstaller";
    
    static void setupBootstrapIfNeeded(final Activity activity, final Runnable whenDone) {
        // ...
    }
}
```

#### Kotlin 版本
```kotlin
object TermuxInstaller {
    private const val LOG_TAG = "TermuxInstaller"
    
    @JvmStatic
    fun setupBootstrapIfNeeded(activity: Activity, whenDone: Runnable) {
        // ...
    }
}
```

**改进点**:
- 使用 `object` 单例对象替代 `final class`，更符合 Kotlin 习惯
- 使用 `const val` 替代 `static final`，编译时常量
- 添加 `@JvmStatic` 注解确保 Java 互操作性

### 2. 变量声明和类型推断

#### Java 版本
```java
String bootstrapErrorMessage;
Error filesDirectoryAccessibleError;
final ProgressDialog progress = ProgressDialog.show(...);
final byte[] buffer = new byte[8096];
final List<Pair<String, String>> symlinks = new ArrayList<>(50);
```

#### Kotlin 版本
```kotlin
val bootstrapErrorMessage: String
val filesDirectoryAccessibleError: Error?
val progress = ProgressDialog.show(...)
val buffer = ByteArray(8096)
val symlinks = mutableListOf<Pair<String, String>>()
```

**改进点**:
- 使用 `val` 替代 `final`，不可变变量
- 类型推断，减少冗余类型声明
- 使用 Kotlin 标准库函数 `mutableListOf()` 替代 `ArrayList`
- 使用 `ByteArray` 替代 `byte[]`
- 明确可空类型 `Error?`

### 3. 空安全检查

#### Java 版本
```java
boolean isFilesDirectoryAccessible = filesDirectoryAccessibleError == null;

if (error != null) {
    showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
    return;
}
```

#### Kotlin 版本
```kotlin
val isFilesDirectoryAccessible = filesDirectoryAccessibleError == null

if (error != null) {
    showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error))
    return@Thread
}
```

**改进点**:
- Kotlin 的空安全类型系统在编译时检查
- 使用 `return@Thread` 标签明确从 lambda 返回

### 4. 字符串模板

#### Java 版本
```java
Logger.logInfo(LOG_TAG, "The termux prefix directory \"" + TERMUX_PREFIX_DIR_PATH + "\" exists but is empty...");
Logger.logInfo(LOG_TAG, "Installing " + TermuxConstants.TERMUX_APP_NAME + " bootstrap packages.");
```

#### Kotlin 版本
```kotlin
Logger.logInfo(LOG_TAG, "The termux prefix directory \"$TERMUX_PREFIX_DIR_PATH\" exists but is empty...")
Logger.logInfo(LOG_TAG, "Installing ${TermuxConstants.TERMUX_APP_NAME} bootstrap packages.")
```

**改进点**:
- 使用字符串模板 `$variable` 和 `${expression}`
- 代码更简洁易读，避免字符串拼接

### 5. 集合操作

#### Java 版本
```java
final List<Pair<String, String>> symlinks = new ArrayList<>(50);
symlinks.add(Pair.create(oldPath, newPath));

if (symlinks.isEmpty())
    throw new RuntimeException("No SYMLINKS.txt encountered");
    
for (Pair<String, String> symlink : symlinks) {
    Os.symlink(symlink.first, symlink.second);
}
```

#### Kotlin 版本
```kotlin
val symlinks = mutableListOf<Pair<String, String>>()
symlinks.add(Pair(oldPath, newPath))

if (symlinks.isEmpty()) {
    throw RuntimeException("No SYMLINKS.txt encountered")
}

for ((oldPath, newPath) in symlinks) {
    Os.symlink(oldPath, newPath)
}
```

**改进点**:
- 使用 Kotlin 的 `Pair` 构造函数，更简洁
- 使用解构声明 `(oldPath, newPath)` 遍历，代码更清晰
- 强制使用花括号，提高代码一致性

### 6. 资源管理 (try-with-resources)

#### Java 版本
```java
try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
    ZipEntry zipEntry;
    while ((zipEntry = zipInput.getNextEntry()) != null) {
        // ...
    }
}

try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
    int readBytes;
    while ((readBytes = zipInput.read(buffer)) != -1)
        outStream.write(buffer, 0, readBytes);
}
```

#### Kotlin 版本
```kotlin
ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipInput ->
    var zipEntry = zipInput.nextEntry
    while (zipEntry != null) {
        // ...
        zipEntry = zipInput.nextEntry
    }
}

FileOutputStream(targetFile).use { outStream ->
    var readBytes = zipInput.read(buffer)
    while (readBytes != -1) {
        outStream.write(buffer, 0, readBytes)
        readBytes = zipInput.read(buffer)
    }
}
```

**改进点**:
- 使用 Kotlin 的 `.use {}` 扩展函数替代 try-with-resources
- 自动资源管理，更简洁
- Lambda 语法更现代

### 7. Lambda 表达式

#### Java 版本
```java
new Thread() {
    @Override
    public void run() {
        try {
            // ...
        } catch (final Exception e) {
            // ...
        } finally {
            activity.runOnUiThread(() -> {
                try {
                    progress.dismiss();
                } catch (RuntimeException e) {
                    // Activity already dismissed - ignore.
                }
            });
        }
    }
}.start();
```

#### Kotlin 版本
```kotlin
Thread {
    try {
        // ...
    } catch (e: Exception) {
        // ...
    } finally {
        activity.runOnUiThread {
            try {
                progress.dismiss()
            } catch (e: RuntimeException) {
                // Activity already dismissed - ignore.
            }
        }
    }
}.start()
```

**改进点**:
- 使用 Kotlin 的 lambda 语法，更简洁
- 不需要显式 `@Override` 和 `new Runnable()`
- 代码更易读

### 8. 对话框构建

#### Java 版本
```java
new AlertDialog.Builder(activity)
    .setTitle(R.string.bootstrap_error_title)
    .setMessage(R.string.bootstrap_error_body)
    .setNegativeButton(R.string.bootstrap_error_abort, (dialog, which) -> {
        dialog.dismiss();
        activity.finish();
    })
    .setPositiveButton(R.string.bootstrap_error_try_again, (dialog, which) -> {
        dialog.dismiss();
        FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
        TermuxInstaller.setupBootstrapIfNeeded(activity, whenDone);
    })
    .show();
```

#### Kotlin 版本
```kotlin
AlertDialog.Builder(activity)
    .setTitle(R.string.bootstrap_error_title)
    .setMessage(R.string.bootstrap_error_body)
    .setNegativeButton(R.string.bootstrap_error_abort) { dialog, _ ->
        dialog.dismiss()
        activity.finish()
    }
    .setPositiveButton(R.string.bootstrap_error_try_again) { dialog, _ ->
        dialog.dismiss()
        FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true)
        setupBootstrapIfNeeded(activity, whenDone)
    }
    .show()
```

**改进点**:
- 使用 Kotlin lambda 语法
- 使用 `_` 忽略未使用的参数
- 不需要显式类名 `TermuxInstaller.`（在 object 内部）

### 9. 数组和循环

#### Java 版本
```java
File[] dirs = context.getExternalFilesDirs(null);
if (dirs != null && dirs.length > 0) {
    for (int i = 0; i < dirs.length; i++) {
        File dir = dirs[i];
        if (dir == null) continue;
        String symlinkName = "external-" + i;
        // ...
    }
}
```

#### Kotlin 版本
```kotlin
val externalFilesDirs = context.getExternalFilesDirs(null)
if (externalFilesDirs != null && externalFilesDirs.isNotEmpty()) {
    for (i in externalFilesDirs.indices) {
        val dir = externalFilesDirs[i] ?: continue
        val symlinkName = "external-$i"
        // ...
    }
}
```

**改进点**:
- 使用 `isNotEmpty()` 替代 `length > 0`
- 使用 `indices` 属性获取索引范围
- 使用 Elvis 操作符 `?:` 简化空检查
- 字符串模板替代字符串拼接

### 10. 正则表达式

#### Java 版本
```java
activity.getFilesDir().getAbsolutePath().replaceAll("^/data/user/0/", "/data/data/")
```

#### Kotlin 版本
```kotlin
activity.filesDir.absolutePath.replace("^/data/user/0/".toRegex(), "/data/data/")
```

**改进点**:
- 使用 Kotlin 属性访问 `filesDir` 和 `absolutePath`
- 显式使用 `.toRegex()` 创建正则表达式
- 更清晰地表明这是正则替换

### 11. 注解和抑制警告

#### Java 版本
```java
//noinspection SdCardPath
if (PackageUtils.isAppInstalledOnExternalStorage(activity) && ...) {
    // ...
}

//noinspection OctalInteger
Os.chmod(targetFile.getAbsolutePath(), 0700);
```

#### Kotlin 版本
```kotlin
@Suppress("SdCardPath")
if (PackageUtils.isAppInstalledOnExternalStorage(activity) && ...) {
    // ...
}

@Suppress("OctalInteger")
Os.chmod(targetFile.absolutePath, 0700)
```

**改进点**:
- 使用 Kotlin 的 `@Suppress` 注解替代注释
- 更标准的警告抑制方式

### 12. Native 方法声明

#### Java 版本
```java
public static native byte[] getZip();
```

#### Kotlin 版本
```kotlin
@JvmStatic
external fun getZip(): ByteArray
```

**改进点**:
- 使用 `external` 关键字替代 `native`
- 使用 `ByteArray` 替代 `byte[]`
- 添加 `@JvmStatic` 确保 JNI 兼容性

## 代码统计对比

| 指标 | Java 版本 | Kotlin 版本 | 改进 |
|------|-----------|-------------|------|
| 总行数 | ~450 行 | ~420 行 | -6.7% |
| 代码密度 | 较低 | 较高 | 更简洁 |
| 类型安全 | 运行时 | 编译时 | 更安全 |
| 空安全 | 手动检查 | 类型系统 | 更可靠 |

## 兼容性保证

所有公共方法都添加了 `@JvmStatic` 注解，确保：
1. 从 Java 代码调用时保持相同的语法
2. JNI native 方法正常工作
3. 完全向后兼容

## 使用建议

### 迁移步骤
1. 保留原始 Java 文件作为备份
2. 测试 Kotlin 版本的所有功能
3. 确认 JNI 调用正常工作
4. 验证所有调用方（Java 和 Kotlin）都能正常使用
5. 确认构建和运行无误后，可以删除 Java 版本

### 注意事项
1. **JNI 兼容性**: native 方法 `getZip()` 需要确保 C 代码中的函数签名匹配
2. **单例模式**: 使用 `object` 替代 `final class`，确保单例行为
3. **空安全**: Kotlin 的空安全类型系统可能会在编译时发现潜在的空指针问题
4. **互操作性**: 所有公共 API 都添加了 `@JvmStatic`，确保 Java 代码可以无缝调用

## 测试清单

- [ ] Bootstrap 安装流程正常
- [ ] 错误对话框显示正确
- [ ] 存储符号链接创建成功
- [ ] JNI native 方法调用正常
- [ ] 从 Java 代码调用 Kotlin 方法正常
- [ ] 所有异常处理正确
- [ ] 进度对话框显示和关闭正常
- [ ] 文件权限设置正确
- [ ] 符号链接创建正确

## 总结

Kotlin 版本相比 Java 版本的主要优势：
1. **更简洁**: 减少样板代码，提高可读性
2. **更安全**: 编译时空安全检查，减少运行时错误
3. **更现代**: 使用现代语言特性（lambda、扩展函数、字符串模板等）
4. **完全兼容**: 保持与 Java 代码的互操作性
5. **更易维护**: 代码更清晰，逻辑更明确

建议在充分测试后采用 Kotlin 版本，可以提高代码质量和开发效率。
