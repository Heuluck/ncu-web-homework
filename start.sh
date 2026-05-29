#!/bin/bash
set -e

echo "=== 在线聊天系统 - 本地启动 ==="

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "错误: 未找到 Java，请安装 JDK 17+"
    exit 1
fi

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到 Maven，请安装 Maven"
    exit 1
fi

echo "1. 编译项目..."
mvn -f backend/pom.xml package -DskipTests -q

echo "2. 启动应用..."
echo "   访问地址: http://localhost:8080"
echo "   按 Ctrl+C 停止"
echo ""
java -jar backend/target/*.jar
