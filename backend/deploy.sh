#!/bin/bash
set -e

echo "=== 在线聊天系统部署脚本 ==="

echo "1. 构建并启动服务..."
docker-compose up -d --build

echo "2. 等待 MySQL 就绪..."
sleep 30

echo "3. 检查服务状态..."
docker-compose ps

echo ""
echo "=== 部署完成 ==="
echo "访问地址: http://localhost:8080"
echo "MySQL 地址: localhost:3306"
echo ""
echo "常用命令:"
echo "  查看日志: docker-compose logs -f"
echo "  停止服务: docker-compose down"
echo "  重启服务: docker-compose restart"
