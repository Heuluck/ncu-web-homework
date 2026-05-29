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

Write-Host "1. 编译项目..."
mvn -f backend/pom.xml package -DskipTests -q

Write-Host "2. 启动应用..."
Write-Host "   访问地址: http://localhost:8080"
Write-Host "   按 Ctrl+C 停止"
Write-Host ""
java -jar backend/target/*.jar
