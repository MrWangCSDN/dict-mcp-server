# Dict MCP Server 使用指南

## 快速开始

### 1. 配置 Maven 仓库

确保字典 JAR 包已安装到 Maven 本地仓库:

```bash
# 检查 JAR 包是否存在
ls ~/.m2/repository/com/spdb/ccbs/dictionary/dictionary/1.0.0-SNAPSHOT/

# 应该看到类似的文件:
# dictionary-1.0.0-20260207.123456-1.jar
# dictionary-1.0.0-20260207.145622-2.jar
```

### 2. 修改配置 (可选)

编辑 `src/main/resources/application.yml`:

```yaml
dict:
  maven-repo: ${user.home}/.m2/repository  # Maven 仓库路径
  group-id: com/spdb/ccbs/dictionary       # 根据实际情况修改
  artifact-id: dictionary
  version: 1.0.0-SNAPSHOT
  xml-path: dict/MDict.d_schema.xml
```

### 3. 启动服务

```bash
# 方式 1: 使用 Maven
mvn spring-boot:run

# 方式 2: 打包后运行
mvn clean package
java -jar target/dict-mcp-server-1.0.0.jar
```

### 4. 验证服务

查看日志确认服务启动成功:

```
开始加载字典文件...
Maven 仓库: /Users/xxx/.m2/repository
找到字典 JAR 包: xxx/dictionary-1.0.0-20260207.145622-2.jar
成功解析字典文件, 共 1250 个字段
```

## MCP 工具使用

### getDictDefByLongNameList - 批量查询字段

**功能**: 根据字段中文名称批量查询字段元数据

**输入参数**:
```json
{
  "longNameList": ["客户ID", "国家", "性别", "未知字段"]
}
```

**返回结果**:
```json
{
  "客户ID": {
    "id": "custId",
    "longname": "客户ID",
    "type": "MBaseType.U_KE_HU_BIAN_HAO",
    "dbname": "cust_id",
    "ref": "MDict.A.custId"
  },
  "国家": {
    "id": "cst",
    "longname": "国家",
    "type": "MBaseType.U_GUO_JIA",
    "dbname": "cst",
    "ref": "MDict.C.cst"
  },
  "性别": {
    "id": "xb",
    "longname": "性别",
    "type": "MBaseType.U_XING_BIE",
    "dbname": "xb",
    "ref": "MDict.X.xb"
  },
  "未知字段": null
}
```

**使用场景**: 在 flowtran 交易创建时,批量查询所有字段的元数据

### reloadDict - 重新加载字典

**功能**: 重新从 Maven 仓库加载最新的字典文件

**使用场景**: 
- 字典 JAR 包更新后
- 开发调试时
- 缓存数据需要刷新时

**调用**:
```json
{}
```

**返回**:
```json
{
  "success": true,
  "message": "字典文件重新加载成功"
}
```

### getCacheStats - 获取缓存统计

**功能**: 查看当前缓存的字段数量和所有字段中文名

**调用**:
```json
{}
```

**返回**:
```json
{
  "totalFields": 1250,
  "cacheKeys": ["客户ID", "国家", "性别", "账号", ...]
}
```

## 常见问题

### Q: 如何更新字典文件?

**A**: 
1. 更新 Maven 仓库中的 dictionary JAR 包
2. 调用 `reloadDict` 工具重新加载
3. 或重启服务

### Q: 查询返回 null 是什么原因?

**A**: 
- 字段中文名在字典 XML 中不存在
- longname 拼写与 XML 中不完全一致
- XML 文件解析失败

### Q: 如何确认字典文件加载成功?

**A**: 
1. 查看启动日志,应该有"成功解析字典文件"的提示
2. 调用 `getCacheStats` 查看缓存的字段数量
3. 测试查询已知字段,看是否返回正确结果

### Q: 快照版本如何选择?

**A**: 服务自动选择最新的快照版本(按文件修改时间倒序)

### Q: 支持多个字典文件吗?

**A**: 当前版本仅支持单个 XML 文件。如需支持多个,需要扩展代码。

## 开发和测试

### 运行测试

```bash
mvn test
```

### 调试模式

修改 `application.yml`:

```yaml
logging:
  level:
    com.spdb.mcp: TRACE  # 查看详细日志
```

### 本地测试

创建测试 XML 文件在 `src/test/resources/test-schema.xml`,运行测试验证解析逻辑。
