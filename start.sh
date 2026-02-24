#!/bin/bash

# Dict MCP Server 启动脚本

echo "=========================================="
echo "Dict MCP Server 启动脚本"
echo "版本: 0224-3.0"
echo "=========================================="
echo ""

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到 Java 环境"
    echo "请安装 Java 17 或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "错误: Java 版本过低 (当前: $JAVA_VERSION, 需要: 17+)"
    exit 1
fi

echo "✓ Java 环境检查通过"
echo ""

# 检查 JAR 包是否存在
JAR_FILE="target/dict-mcp-server-0224-3.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "未找到 JAR 包，开始构建..."
    if ! command -v mvn &> /dev/null; then
        echo "错误: 未找到 Maven"
        echo "请先安装 Maven 或手动构建项目"
        exit 1
    fi
    
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo "构建失败"
        exit 1
    fi
fi

echo "✓ JAR 包检查通过"
echo ""

# 启动服务
echo "启动 Dict MCP Server..."
echo "日志文件: dict-mcp-server.log"
echo ""

nohup java -Xms512m -Xmx1024m -jar "$JAR_FILE" > dict-mcp-server.log 2>&1 &

PID=$!
echo "服务已启动, PID: $PID"
echo ""

# 等待服务启动
echo "等待服务启动..."
sleep 5

# 检查进程是否存在
if ps -p $PID > /dev/null; then
    echo "✓ 服务启动成功"
    echo ""
    echo "访问地址: http://localhost:8080"
    echo "健康检查: http://localhost:8080/actuator/health"
    echo "MCP 端点: http://localhost:8080/mcp"
    echo ""
    echo "查看日志: tail -f dict-mcp-server.log"
    echo "停止服务: kill $PID"
else
    echo "✗ 服务启动失败，请查看日志:"
    tail -50 dict-mcp-server.log
    exit 1
fi

echo "=========================================="
