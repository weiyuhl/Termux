# Termux 构建依赖备份

本文件夹包含 Termux 项目构建过程中下载的依赖文件备份。

## 备份时间
2026年3月8日

## 依赖列表

### termux-am-library v2.0.0
- **来源**: https://jitpack.io/com/termux/termux-am-library/v2.0.0/
- **用途**: Termux Activity Manager 库，用于 termux-shared 模块
- **文件**:
  - `termux-am-library-v2.0.0.aar` (16,687 字节) - Android 库文件
  - `termux-am-library-v2.0.0.module` (1,768 字节) - Gradle 模块元数据
  - `termux-am-library-v2.0.0.pom` (1,220 字节) - Maven POM 文件

## 使用说明

如果将来构建时无法从网络下载这些依赖，可以：

1. 将 `termux-am-library` 文件夹复制到：
   ```
   %USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.termux\
   ```

2. 或者配置本地 Maven 仓库使用这些文件

## 原始缓存位置
```
C:\Users\USER228466\.gradle\caches\modules-2\files-2.1\com.termux\termux-am-library\v2.0.0\
```
