#!/bin/bash
set -e

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-root}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== 数据库迁移脚本 ==="

echo "1. 初始化数据库..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" < "$SCRIPT_DIR/init.sql"

echo "2. 执行迁移..."
for sql_file in "$SCRIPT_DIR/migration"/*.sql; do
    if [ -f "$sql_file" ]; then
        echo "   执行: $(basename "$sql_file")"
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" < "$sql_file"
    fi
done

echo "=== 迁移完成 ==="
