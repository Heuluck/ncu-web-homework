# 在线聊天系统 - 本地启动 (PowerShell)

Write-Host "=== 在线聊天系统 - 本地启动 ==="

# 检查 Java
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "错误: 未找到 Java，请安装 JDK 17+"
    exit 1
}

# 检查 Maven
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "错误: 未找到 Maven，请安装 Maven"
    exit 1
}

# 数据库迁移
if (Get-Command mysql -ErrorAction SilentlyContinue) {
    Write-Host "1. 执行数据库迁移..."
    .\db\migrate.ps1
} else {
    Write-Host "1. 跳过数据库迁移（未找到 mysql 客户端）"
}

# 同步前端文件到 static/
Write-Host "2. 同步前端文件..."
Copy-Item -Path "frontend\*" -Destination "backend\src\main\resources\static\" -Recurse -Force -ErrorAction SilentlyContinue

# 编译
Write-Host "3. 编译项目..."
mvn -f backend/pom.xml package -DskipTests -q

# 启动
Write-Host "4. 启动应用..."
Write-Host "   访问地址: http://localhost:8080"
Write-Host "   按 Ctrl+C 停止"
Write-Host ""
java -jar backend/target/*.jar
