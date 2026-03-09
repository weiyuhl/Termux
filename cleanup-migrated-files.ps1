# 清理已迁移到 termux-core 的文件

Write-Host "开始清理已迁移的文件..." -ForegroundColor Green

# 从 terminal-emulator 删除 (1 个文件)
$files_terminal_emulator = @(
    "terminal-emulator/src/main/java/com/termux/terminal/JNI.kt"
)

# 从 termux-shared 删除 (22 个文件)
$files_termux_shared = @(
    "termux-shared/src/main/java/com/termux/shared/termux/TermuxBootstrap.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/TermuxShellManager.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/TermuxShellUtils.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/am/TermuxAmSocketServer.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxShellEnvironment.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxAppShellEnvironment.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxAPIShellEnvironment.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment/TermuxShellCommandShellEnvironment.kt",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command/runner/terminal/TermuxSession.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/ShellUtils.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/ArgumentTokenizer.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/StreamGobbler.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/am/AmSocketServer.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/command/ExecutionCommand.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/command/ShellCommandConstants.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/command/environment/UnixShellEnvironment.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/command/environment/ShellEnvironmentUtils.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/command/environment/AndroidShellEnvironment.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/command/environment/ShellCommandShellEnvironment.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/command/result/ResultData.kt",
    "termux-shared/src/main/java/com/termux/shared/shell/command/runner/app/AppShell.kt"
)

# 从 app 删除 (1 个文件 - TermuxInstaller.kt)
$files_app = @(
    "app/src/main/java/com/termux/app/TermuxInstaller.kt"
)

# 删除函数
function Remove-FileIfExists {
    param($filePath)
    if (Test-Path $filePath) {
        Remove-Item $filePath -Force
        Write-Host "✓ 已删除: $filePath" -ForegroundColor Yellow
        return $true
    } else {
        Write-Host "✗ 文件不存在: $filePath" -ForegroundColor Red
        return $false
    }
}

# 统计
$total = 0
$deleted = 0

# 删除 terminal-emulator 文件
Write-Host "`n=== 清理 terminal-emulator 模块 ===" -ForegroundColor Cyan
foreach ($file in $files_terminal_emulator) {
    $total++
    if (Remove-FileIfExists $file) { $deleted++ }
}

# 删除 termux-shared 文件
Write-Host "`n=== 清理 termux-shared 模块 ===" -ForegroundColor Cyan
foreach ($file in $files_termux_shared) {
    $total++
    if (Remove-FileIfExists $file) { $deleted++ }
}

# 删除 app 文件
Write-Host "`n=== 清理 app 模块 ===" -ForegroundColor Cyan
foreach ($file in $files_app) {
    $total++
    if (Remove-FileIfExists $file) { $deleted++ }
}

# 清理空目录
Write-Host "`n=== 清理空目录 ===" -ForegroundColor Cyan
$empty_dirs = @(
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command/runner/terminal",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command/runner",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command/environment",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/command",
    "termux-shared/src/main/java/com/termux/shared/termux/shell/am",
    "termux-shared/src/main/java/com/termux/shared/shell/command/runner/app",
    "termux-shared/src/main/java/com/termux/shared/shell/command/runner",
    "termux-shared/src/main/java/com/termux/shared/shell/command/result",
    "termux-shared/src/main/java/com/termux/shared/shell/command/environment",
    "termux-shared/src/main/java/com/termux/shared/shell/command",
    "termux-shared/src/main/java/com/termux/shared/shell/am"
)

foreach ($dir in $empty_dirs) {
    if (Test-Path $dir) {
        $items = Get-ChildItem $dir -Force
        if ($items.Count -eq 0) {
            Remove-Item $dir -Force
            Write-Host "✓ 已删除空目录: $dir" -ForegroundColor Yellow
        }
    }
}

# 总结
Write-Host "`n=== 清理完成 ===" -ForegroundColor Green
Write-Host "总文件数: $total" -ForegroundColor White
Write-Host "已删除: $deleted" -ForegroundColor Green
Write-Host "未找到: $($total - $deleted)" -ForegroundColor Red

if ($deleted -eq $total) {
    Write-Host "`n✓ 所有文件已成功删除！" -ForegroundColor Green
} else {
    Write-Host "`n⚠ 部分文件未找到，请检查。" -ForegroundColor Yellow
}
