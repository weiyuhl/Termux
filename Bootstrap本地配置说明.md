# Bootstrap 本地配置说明

## 配置完成时间
2026年3月8日 23:02

## 配置目标
将 Termux 项目的 bootstrap 文件配置为优先使用本地文件，避免每次构建时从网络下载。

---

## 修改内容

### 1. 修改 app/build.gradle 中的 downloadBootstrap 函数

**位置**: `app/build.gradle` 第 167-213 行

**修改内容**:
- 添加了从本地 `Bootstrap文件/` 目录复制文件的逻辑
- 保留了网络下载作为备用方案
- 添加了详细的日志输出

**工作流程**:
```
1. 检查 app/src/main/cpp/bootstrap-{arch}.zip 是否存在
   ├─ 存在 → 验证 SHA-256 校验和
   │   ├─ 正确 → 跳过，使用现有文件
   │   └─ 错误 → 删除文件，继续下一步
   └─ 不存在 → 继续下一步

2. 检查 Bootstrap文件/bootstrap-{arch}.zip 是否存在
   ├─ 存在 → 复制到 app/src/main/cpp/
   │   ├─ 验证复制后的 SHA-256 校验和
   │   │   ├─ 正确 → 完成
   │   │   └─ 错误 → 抛出异常
   │   └─ 完成
   └─ 不存在 → 继续下一步

3. 从网络下载（备用方案）
   └─ 从 GitHub Releases 下载
       ├─ 验证 SHA-256 校验和
       │   ├─ 正确 → 完成
       │   └─ 错误 → 抛出异常
       └─ 完成
```

### 2. 修改 clean 任务

**位置**: `app/build.gradle` 第 215-222 行

**修改内容**:
- 注释掉了删除 bootstrap 文件的逻辑
- 添加了提示信息

**原因**: 
- 避免每次 clean 后都需要重新复制文件
- 保留本地文件，加快构建速度
- 如需重新复制，可手动删除

---

## 文件位置

### 源文件位置
```
Bootstrap文件/
├── bootstrap-aarch64.zip  (30,542,758 字节)
├── bootstrap-arm.zip      (27,460,370 字节)
├── bootstrap-i686.zip     (29,722,143 字节)
└── bootstrap-x86_64.zip   (30,393,585 字节)
```

### 目标位置
```
app/src/main/cpp/
├── bootstrap-aarch64.zip
├── bootstrap-arm.zip
├── bootstrap-i686.zip
└── bootstrap-x86_64.zip
```

---

## 使用方法

### 正常构建
```bash
# 直接构建，会自动从本地复制 bootstrap 文件
./gradlew assembleRelease
```

### 手动复制 bootstrap 文件
```bash
# 如果需要手动触发复制
./gradlew :app:downloadBootstraps
```

### 清理构建
```bash
# clean 不会删除 bootstrap 文件
./gradlew clean
```

### 强制重新复制
```bash
# 1. 手动删除目标文件
Remove-Item "app/src/main/cpp/bootstrap-*.zip" -Force

# 2. 重新复制
./gradlew :app:downloadBootstraps
```

---

## 验证测试

### 测试 1: 从本地复制
```bash
# 删除目标文件
Remove-Item "app/src/main/cpp/bootstrap-*.zip" -Force

# 运行复制任务
./gradlew :app:downloadBootstraps
```

**预期输出**:
```
> Task :app:downloadBootstraps
Copying bootstrap from local directory: D:\termux\Bootstrap文件\bootstrap-aarch64.zip
Bootstrap file copied successfully with correct checksum
Copying bootstrap from local directory: D:\termux\Bootstrap文件\bootstrap-arm.zip
Bootstrap file copied successfully with correct checksum
Copying bootstrap from local directory: D:\termux\Bootstrap文件\bootstrap-i686.zip
Bootstrap file copied successfully with correct checksum
Copying bootstrap from local directory: D:\termux\Bootstrap文件\bootstrap-x86_64.zip
Bootstrap file copied successfully with correct checksum
```

### 测试 2: 使用现有文件
```bash
# 再次运行（文件已存在）
./gradlew :app:downloadBootstraps
```

**预期输出**:
```
> Task :app:downloadBootstraps
Bootstrap file already exists with correct checksum: src/main/cpp/bootstrap-aarch64.zip
Bootstrap file already exists with correct checksum: src/main/cpp/bootstrap-arm.zip
Bootstrap file already exists with correct checksum: src/main/cpp/bootstrap-i686.zip
Bootstrap file already exists with correct checksum: src/main/cpp/bootstrap-x86_64.zip
```

### 测试 3: clean 保留文件
```bash
# 运行 clean
./gradlew clean

# 检查文件是否还在
Get-ChildItem "app/src/main/cpp/bootstrap-*.zip"
```

**预期结果**: 文件仍然存在

---

## SHA-256 校验和

配置中使用的校验和（apt-android-7 变体）:

```
aarch64: ea2aeba8819e517db711f8c32369e89e7c52cee73e07930ff91185e1ab93f4f3
arm:     a38f4d3b2f735f83be2bf54eff463e86dc32a3e2f9f861c1557c4378d249c018
i686:    f5bc0b025b9f3b420b5fcaeefc064f888f5f22a0d6fd7090f4aac0c33eb3555b
x86_64:  b7fd0f2e3a4de534be3144f9f91acc768630fc463eaf134ab2e64c545e834f7a
```

---

## 优势

### 1. 离线构建
- ✅ 无需网络连接即可构建
- ✅ 不受 GitHub 服务状态影响
- ✅ 避免网络超时问题

### 2. 构建速度
- ✅ 本地复制比网络下载快得多
- ✅ 减少构建时间
- ✅ 提高开发效率

### 3. 稳定性
- ✅ 不受网络波动影响
- ✅ 构建结果可预测
- ✅ 减少构建失败率

### 4. 版本控制
- ✅ 可以将 bootstrap 文件纳入版本控制
- ✅ 确保团队使用相同版本
- ✅ 便于回溯和调试

---

## 注意事项

### 1. 文件完整性
- 确保 `Bootstrap文件/` 目录中的文件完整且未损坏
- 定期验证文件的 SHA-256 校验和
- 不要手动修改这些文件

### 2. 版本匹配
- Bootstrap 版本必须与 `app/build.gradle` 中配置的版本一致
- 更新 Termux 版本时，可能需要更新 bootstrap 文件
- 检查 `packageVariant` 配置是否正确

### 3. 磁盘空间
- 4 个 bootstrap 文件总大小约 114 MB
- 复制后 `app/src/main/cpp/` 目录也会占用 114 MB
- 确保有足够的磁盘空间

### 4. 备用方案
- 如果本地文件损坏或丢失，会自动从网络下载
- 确保网络可用作为备用
- 或者重新从 GitHub Releases 下载文件

---

## 更新 Bootstrap

如果需要更新到新版本的 bootstrap:

### 步骤 1: 下载新版本
访问 https://github.com/termux/termux-packages/releases

找到对应版本的 bootstrap 文件并下载。

### 步骤 2: 替换文件
```bash
# 将新文件放入 Bootstrap文件/ 目录
Copy-Item "下载路径/bootstrap-*.zip" "Bootstrap文件/" -Force
```

### 步骤 3: 更新配置
编辑 `app/build.gradle`，更新版本号和校验和：

```groovy
task downloadBootstraps() {
    doLast {
        def packageVariant = project.ext.packageVariant
        if (packageVariant == "apt-android-7") {
            def version = "新版本号"
            downloadBootstrap("aarch64", "新的校验和", version)
            downloadBootstrap("arm", "新的校验和", version)
            downloadBootstrap("i686", "新的校验和", version)
            downloadBootstrap("x86_64", "新的校验和", version)
        }
    }
}
```

### 步骤 4: 重新复制
```bash
# 删除旧文件
Remove-Item "app/src/main/cpp/bootstrap-*.zip" -Force

# 复制新文件
./gradlew :app:downloadBootstraps
```

### 步骤 5: 测试构建
```bash
./gradlew assembleRelease
```

---

## 故障排除

### 问题 1: 校验和不匹配
**错误信息**: `Wrong checksum for local file: expected: xxx, actual: yyy`

**解决方法**:
1. 重新下载 bootstrap 文件
2. 验证下载的文件完整性
3. 检查 `app/build.gradle` 中的校验和是否正确

### 问题 2: 文件不存在
**错误信息**: `Local bootstrap file not found, downloading from ...`

**解决方法**:
1. 检查 `Bootstrap文件/` 目录是否存在
2. 检查文件名是否正确
3. 确保文件没有被误删

### 问题 3: 复制失败
**错误信息**: 文件复制过程中出错

**解决方法**:
1. 检查磁盘空间是否充足
2. 检查文件权限
3. 尝试手动复制文件

### 问题 4: 构建失败
**错误信息**: `Could not find incbin file 'bootstrap-aarch64.zip'`

**解决方法**:
1. 运行 `./gradlew :app:downloadBootstraps`
2. 检查 `app/src/main/cpp/` 目录中是否有文件
3. 查看构建日志确认问题

---

## 相关文档

- `Bootstrap文件/README.md` - Bootstrap 文件详细说明
- `依赖配置说明.md` - 项目依赖配置总览
- `依赖远程地址访问记录.md` - 远程地址访问记录

---

## 配置状态

✅ **配置完成**

- [x] 修改 downloadBootstrap 函数
- [x] 修改 clean 任务
- [x] 测试本地复制功能
- [x] 测试文件保留功能
- [x] 验证构建成功
- [x] 创建配置文档

**当前状态**: 项目已完全配置为使用本地 bootstrap 文件，可以离线构建。
