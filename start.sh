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

# 检查 MySQL 客户端
if ! command -v mysql &> /dev/null; then
    echo "警告: 未找到 mysql 客户端，跳过数据库迁移"
    echo "   请手动执行 db/migrate.sh 或确保数据库已就绪"
else
    echo "1. 执行数据库迁移..."
    ./db/migrate.sh
fi

echo "2. 编译项目..."
mvn -f backend/pom.xml package -DskipTests -q

echo "3. 启动应用..."
echo "   访问地址: http://localhost:8080"
echo "   按 Ctrl+C 停止"
echo ""
java -jar backend/target/*.jar
