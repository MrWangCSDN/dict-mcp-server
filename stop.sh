#!/bin/bash

# Dict MCP Server 停止脚本

echo "=========================================="
echo "Dict MCP Server 停止脚本"
echo "=========================================="
echo ""

# 查找 Java 进程
PID=$(ps aux | grep "dict-mcp-server-0224-3.0.jar" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "未找到运行中的服务"
    exit 0
fi

echo "找到服务进程: $PID"
echo "正在停止服务..."

kill $PID

# 等待进程结束
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✓ 服务已停止"
        exit 0
    fi
    sleep 1
done

# 强制终止
echo "服务未响应，强制终止..."
kill -9 $PID

if ! ps -p $PID > /dev/null 2>&1; then
    echo "✓ 服务已强制停止"
else
    echo "✗ 停止失败"
    exit 1
fi

echo "=========================================="
