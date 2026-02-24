# Dict MCP Server - 快速开始

## 5 分钟快速上手

### 步骤 1: 确认前置条件

确保你的环境满足以下要求：

```bash
# 检查 Java 版本 (需要 17+)
java -version

# 检查 Maven 版本 (需要 3.6+)
mvn -version

# 检查字典 JAR 包是否存在
ls ~/.m2/repository/com/spdb/ccbs/dictionary/1.0.0-SNAPSHOT/
```

### 步骤 2: 启动服务

```bash
cd /path/to/dict-mcp-server

# 方式 1: 使用 Maven 启动 (推荐开发环境)
mvn spring-boot:run

# 方式 2: 打包后启动 (推荐生产环境)
mvn clean package
java -jar target/dict-mcp-server-1.0.0.jar
```

看到以下日志表示启动成功：

```
Started DictMcpServerApplication in X.XXX seconds
字典文件加载成功, 共 XXX 个字段
```

### 步骤 3: 验证服务

在新的终端窗口执行：

```bash
# 运行测试脚本
./test-mcp-service.sh

# 或手动测试
curl http://localhost:8080/actuator/health
```

### 步骤 4: 配置 Claude Desktop

1. 找到 Claude Desktop 的配置文件：
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

2. 添加 MCP 服务器配置：

```json
{
  "mcpServers": {
    "dict-mcp-server": {
      "url": "http://localhost:8080/mcp",
      "transport": "streamable-http"
    }
  }
}
```

3. 重启 Claude Desktop

### 步骤 5: 测试使用

在 Claude Desktop 中输入：

```
帮我查询"变更后内容描述"、"客户编号"这两个字段的元数据信息
```

Claude 会自动调用 `getDictDefByLongNameList` 工具并返回结果。

## MCP 协议架构

```
┌─────────────────┐    Streamable HTTP    ┌──────────────────┐
│                 │◄─────────────────────►│                  │
│ Claude Desktop  │  http://localhost:8080/mcp  Dict MCP Server │
│   (MCP Client)  │                          │   (MCP Server)   │
│                 │                          │                  │
└─────────────────┘                          └──────────────────┘
                                                      │
                                                      │ 读取
                                                      ▼
                                             ┌─────────────────┐
                                             │ Maven Repository│
                                             │  dictionary.jar │
                                             │ MDict.d_schema  │
                                             └─────────────────┘
```

## 可用工具说明

### 1. 批量查询字段元数据

**使用场景**: 查询一个或多个字段的详细定义

**示例提示词**:
- "查询'客户编号'字段的元数据"
- "帮我看看'交易金额'和'交易日期'这两个字段的定义"
- "批量查询以下字段：客户名称、账号、余额"

**返回信息**:
- `id`: 字段英文名
- `longname`: 字段中文名
- `type`: 字段类型
- `dbname`: 数据库字段名
- `ref`: 字典引用路径 (如 `MDict.A.fieldId`)

**未贯标提示**: 如果字段未贯标，会明确提示 "以下字段需要贯标处理: xxx"

### 2. 重新加载字典

**使用场景**: 字典文件更新后，无需重启服务即可加载最新数据

**示例提示词**:
- "重新加载字典文件"
- "刷新字典缓存"

### 3. 查看缓存统计

**使用场景**: 了解当前缓存的字段总数和字段列表

**示例提示词**:
- "查看字典缓存统计"
- "有多少个字段在缓存中？"
- "列出所有可查询的字段"

## 常见问题

### Q1: 服务启动失败，提示找不到字典文件

**解决方案**:

1. 检查 `application.yml` 配置是否正确
2. 确认 Maven 仓库路径和字典 JAR 包路径
3. 查看启动日志，找到具体错误信息

### Q2: Claude Desktop 无法连接 MCP 服务

**解决方案**:

1. 确认服务已启动: `curl http://localhost:8080/actuator/health`
2. 检查配置文件路径和格式是否正确
3. 重启 Claude Desktop
4. 查看 Claude Desktop 的错误日志

### Q3: 查询字段返回 null

**原因**: 该字段未在字典文件中定义（未贯标）

**解决方案**:

1. 确认字段中文名是否完全匹配（区分大小写）
2. 使用 `getCacheStats` 工具查看所有可查询的字段
3. 联系数据贯标团队添加该字段定义

### Q4: 如何更新字典文件？

**步骤**:

1. 更新 Maven 仓库中的字典 JAR 包
2. 在 Claude 中输入 "重新加载字典文件"
3. 或者重启 MCP 服务

## 性能优化

### 缓存机制

服务启动时会一次性加载所有字段到内存缓存，后续查询直接从内存读取，速度极快。

### 快照版本

服务会自动查找最新的快照版本（根据文件修改时间），确保使用最新的字典定义。

### 日志级别

生产环境建议调整日志级别：

```yaml
logging:
  level:
    com.spdb.mcp: INFO              # 生产环境使用 INFO
    org.springframework.ai: WARN    # 减少框架日志
```

## 进阶使用

### 自定义 Maven 仓库路径

修改 `application.yml`:

```yaml
dict:
  maven-repo: /custom/path/to/.m2/repository
```

### 修改服务端口

```yaml
server:
  port: 9090  # 自定义端口
```

然后在 Claude Desktop 配置中使用新端口：

```json
{
  "url": "http://localhost:9090/mcp"
}
```

### 启用 Actuator 端点 (监控)

添加依赖并配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

访问: `http://localhost:8080/actuator/metrics`

## 下一步

- 查看 [README.md](README.md) 了解详细文档
- 查看 [技术架构](ARCHITECTURE.md) 了解实现细节（如果需要）
- 运行 `./test-mcp-service.sh` 进行完整测试

## 获取帮助

如有问题，请联系开发团队或查看日志：

```bash
# 查看服务日志
tail -f logs/dict-mcp-server.log

# 增加日志详细程度
# 修改 application.yml 中的日志级别为 DEBUG
```
