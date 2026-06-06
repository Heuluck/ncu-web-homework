# 数据库迁移脚本 (PowerShell)

# 确保 stdin/stdout 编码为 UTF-8（emoji 等 4 字节字符需要）
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$DB_HOST = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$DB_PORT = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$DB_USER = if ($env:DB_USER) { $env:DB_USER } else { "root" }
$DB_PASS = if ($env:DB_PASS) { $env:DB_PASS } else { "root" }
$DB_NAME = if ($env:DB_NAME) { $env:DB_NAME } else { "chat_system" }

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$MIGRATION_DIR = Join-Path $SCRIPT_DIR "migration"

$CHARSET_OPTS = "--default-character-set=utf8mb4"

function Invoke-MySQL {
    param([string]$Query, [string]$Database)
    $args = @($CHARSET_OPTS, "-h$DB_HOST", "-P$DB_PORT", "-u$DB_USER", "-p$DB_PASS")
    if ($Database) { $args += $Database }
    $args += "-e", $Query
    mysql @args
}

Write-Host "=== 数据库迁移 ==="

Write-Host "创建数据库 $DB_NAME ..."
Invoke-MySQL -Query "CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

Write-Host "执行迁移文件..."
$count = 0
Get-ChildItem -Path $MIGRATION_DIR -Filter "*.sql" | Sort-Object Name | ForEach-Object {
    Write-Host "   -> $($_.Name)"
    Get-Content $_.FullName -Raw -Encoding UTF8 | mysql $CHARSET_OPTS -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME
    $count++
}

Write-Host "=== 迁移完成，共执行 $count 个文件 ==="
