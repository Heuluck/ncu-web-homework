# 部署脚本 (PowerShell)

Write-Host "=== 在线聊天系统部署脚本 ==="

Write-Host "1. 构建并启动服务..."
docker compose up -d --build

Write-Host "2. 等待 MySQL 就绪..."
Start-Sleep -Seconds 30

Write-Host "3. 检查服务状态..."
docker compose ps

Write-Host ""
Write-Host "=== 部署完成 ==="
Write-Host "访问地址: http://localhost:8080"
Write-Host "MySQL 地址: localhost:3306"
Write-Host ""
Write-Host "常用命令:"
Write-Host "  查看日志: docker compose logs -f"
Write-Host "  停止服务: docker compose down"
Write-Host "  重启服务: docker compose restart"
