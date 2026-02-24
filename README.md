# Dict MCP Server

字典元数据查询 MCP 服务 - 遵循 Model Context Protocol (MCP) 协议规范

## 概述

这是一个基于 Spring AI MCP Server WebFlux 实现的字典元数据查询服务，通过 SSE (Server-Sent Events) 协议提供 MCP 工具调用能力。

### 技术栈

- **协议**: MCP (Model Context Protocol)
- **传输**: SSE (Server-Sent Events) over HTTP
- **框架**: Spring Boot 3.2.0 + Spring AI 1.1.0
- **响应式**: Spring WebFlux

## MCP 协议说明

### 服务端点

```
HTTP: http://localhost:8080/mcp
协议: SSE (Server-Sent Events)
```

### 连接方式

MCP 客户端通过 SSE 连接到服务端点，服务器会推送可用工具列表和执行结果。

```bash
# 使用 curl 测试 SSE 连接
curl -N -H "Accept: text/event-stream" http://localhost:8080/mcp
```

## 可用工具

### 1. getDictDefByLongNameList

批量查询字段元数据定义

**功能**: 根据字段中文名称列表查询字段元数据，未贯标的字段返回 null

**输入参数**:
```json
{
  "longNameList": ["字段中文名1", "字段中文名2", "字段中文名3"]
}
```

**返回结果**:
```json
{
  "success": true,
  "message": "查询成功" 或 "以下字段需要贯标处理: xxx, xxx",
  "total": 3,
  "found": 2,
  "data": {
    "字段中文名1": {
      "id": "fieldId1",
      "longname": "字段中文名1",
      "type": "MBaseType.U_ZI_FU_100",
      "dbname": "field_id1",
      "ref": "MDict.ComplexType.fieldId1"
    },
    "字段中文名2": {
      "id": "fieldId2",
      "longname": "字段中文名2",
      "type": "MBaseType.U_ZI_FU_50",
      "dbname": "field_id2",
      "ref": "MDict.ComplexType.fieldId2"
    },
    "字段中文名3": null
  },
  "unstandardizedFields": ["字段中文名3"]
}
```

### 2. reloadDict

重新加载字典文件 (开发调试用)

**功能**: 重新从 Maven 仓库加载最新的字典文件，无需重启服务

**输入参数**: 无

**返回结果**:
```json
{
  "success": true,
  "message": "字典文件重新加载成功"
}
```

### 3. getCacheStats

获取缓存统计信息

**功能**: 查看当前缓存的字段总数和所有字段名称

**输入参数**: 无

**返回结果**:
```json
{
  "totalFields": 150,
  "cacheKeys": ["字段1", "字段2", "..."],
  "message": "缓存统计查询成功"
}
```

## 配置说明

### application.yml 配置

```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE       # 使用 Streamable HTTP 协议
        name: dict-mcp-server      # 服务名称
        version: 1.0.0             # 服务版本

dict:
  maven-repo: ${user.home}/.m2/repository           # Maven 仓库路径
  group-id: com/spdb/ccbs/dictionary                # 字典 JAR 的 groupId
  artifact-id: dictionary                           # 字典 JAR 的 artifactId
  version: 1.0.0-SNAPSHOT                           # 字典 JAR 版本
  xml-path: dict/MDict.d_schema.xml                 # JAR 内 XML 文件路径
```

### 字典文件格式

服务从 Maven 仓库读取字典 JAR 包 (`dictionary-1.0.0-SNAPSHOT.jar`)，并解析其中的 XML 文件：

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<schema id="MDict">
    <complexType id="A">
       <element id="afMdCntDsc" 
                dbname="afmd_cntdsc" 
                longname="变更后内容描述" 
                type="MBaseType.U_WEN_BEN_1000" 
                required="false" 
                desc="EDD-000015" />
    </complexType>
</schema>
```

**字段映射规则**:
- `longname` → Map 的 key
- 生成的 `ref` = `{schema.id}.{complexType.id}.{element.id}`
- 示例: `MDict.A.afMdCntDsc`

## 启动服务

### 前置条件

1. JDK 17+
2. Maven 3.6+
3. Maven 本地仓库中存在字典 JAR 包

### 运行

```bash
# 编译
mvn clean package

# 启动
java -jar target/dict-mcp-server-1.0.0.jar

# 或使用 Maven 运行
mvn spring-boot:run
```

### 验证

```bash
# 检查服务是否启动
curl http://localhost:8080/actuator/health

# 连接 MCP Streamable HTTP 端点
curl -N http://localhost:8080/mcp
```

## 集成 Claude Desktop

在 Claude Desktop 的 MCP 配置文件中添加此服务：

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

重启 Claude Desktop 后，即可使用字典查询工具。

## 使用示例

在 Claude Desktop 中，可以直接使用自然语言查询字段元数据：

```
用户: 帮我查询"变更后内容描述"、"客户编号"、"交易金额"这几个字段的元数据信息

Claude 会自动调用 getDictDefByLongNameList 工具，返回字段定义或提示未贯标字段
```

## 开发说明

### 添加新的 MCP 工具

使用 Function Bean 方式定义新工具：

```java
@Bean
@Description("工具描述")
public Function<YourRequest, YourResponse> yourToolName() {
    return request -> {
        // 实现逻辑
        return response;
    };
}
```

### 更新字典文件

1. 更新 Maven 仓库中的字典 JAR 包
2. 调用 `reloadDict` 工具重新加载（或重启服务）

### 日志级别

```yaml
logging:
  level:
    com.spdb.mcp: DEBUG                # 查看详细日志
    org.springframework.ai: DEBUG      # 查看 Spring AI MCP 日志
```

## 故障排查

### 1. 服务无法启动

检查 Maven 仓库中是否存在字典 JAR 包：
```bash
ls ~/.m2/repository/com/spdb/ccbs/dictionary/1.0.0-SNAPSHOT/
```

### 2. 字段查询返回 null

- 检查字典 XML 文件中是否包含该字段
- 确认 `longname` 属性值是否完全匹配（区分大小写）
- 调用 `getCacheStats` 查看当前缓存的字段列表

### 3. SSE 连接失败

- 确认服务已启动
- 检查防火墙设置
- 验证端口 8080 未被占用

## License

此项目遵循公司内部许可协议。
