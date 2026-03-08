# 本地 Maven 仓库

本文件夹作为项目的本地 Maven 仓库，存储构建所需的依赖文件。

## 目录结构

```
本地Maven仓库/
└── com/
    └── termux/
        └── termux-am-library/
            └── v2.0.0/
                ├── termux-am-library-v2.0.0.aar
                └── termux-am-library-v2.0.0.pom
```

## 配置说明

在根目录的 `build.gradle` 文件中已配置使用本地仓库：

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        // 本地 Maven 仓库（优先使用）
        maven { url uri("$rootDir/本地Maven仓库") }
        // 备用：在线仓库
        maven { url "https://jitpack.io" }
    }
}
```

## 依赖信息

### termux-am-library v2.0.0
- **Group ID**: com.termux
- **Artifact ID**: termux-am-library
- **Version**: v2.0.0
- **原始来源**: https://jitpack.io/com/termux/termux-am-library/v2.0.0/
- **GitHub**: https://github.com/termux/termux-am-library
- **使用位置**: termux-shared/build.gradle

## 优势

1. **离线构建**: 无需网络连接即可构建项目
2. **构建速度**: 避免网络下载，加快构建速度
3. **稳定性**: 不受外部仓库服务状态影响
4. **版本控制**: 可以将依赖纳入版本控制

## 添加新依赖

如果需要添加新的本地依赖，请按照 Maven 标准目录结构：

```
本地Maven仓库/
└── {groupId 用 / 分隔}/
    └── {artifactId}/
        └── {version}/
            ├── {artifactId}-{version}.{extension}
            └── {artifactId}-{version}.pom
```

例如：
- Group ID: `com.example`
- Artifact ID: `my-library`
- Version: `1.0.0`

目录结构应为：
```
本地Maven仓库/com/example/my-library/1.0.0/
├── my-library-1.0.0.aar
└── my-library-1.0.0.pom
```
