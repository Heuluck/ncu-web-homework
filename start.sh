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

# 数据库迁移
if command -v mysql &> /dev/null; then
    echo "1. 执行数据库迁移..."
    ./db/migrate.sh
else
    echo "1. 跳过数据库迁移（未找到 mysql 客户端）"
fi

# 同步前端文件到 static/
echo "2. 同步前端文件..."
cp -r frontend/* backend/src/main/resources/static/ 2>/dev/null || true

# 编译
echo "3. 编译项目..."
mvn -f backend/pom.xml package -DskipTests -q

# 启动
echo "4. 启动应用..."
echo "   访问地址: http://localhost:8080"
echo "   按 Ctrl+C 停止"
echo ""
java -jar backend/target/*.jar
