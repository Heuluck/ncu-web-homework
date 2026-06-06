#!/bin/bash
set -e

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS-root}
DB_NAME=${DB_NAME:-chat_system}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIGRATION_DIR="$SCRIPT_DIR/migration"

CHARSET_OPTS="--default-character-set=utf8mb4"

mysql_cmd() {
    if [ -z "$DB_PASS" ]; then
        mysql $CHARSET_OPTS -u"$DB_USER" "$@"
    else
        mysql $CHARSET_OPTS -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" "$@"
    fi
}

echo "=== 数据库迁移 ==="

# 创建数据库
echo "创建数据库 $DB_NAME ..."
mysql_cmd -e "CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 按文件名排序执行所有 SQL
echo "执行迁移文件..."
count=0
for sql_file in "$MIGRATION_DIR"/*.sql; do
    [ -f "$sql_file" ] || continue
    echo "   -> $(basename "$sql_file")"
    mysql_cmd "$DB_NAME" < "$sql_file"
    count=$((count + 1))
done

echo "=== 迁移完成，共执行 $count 个文件 ==="
