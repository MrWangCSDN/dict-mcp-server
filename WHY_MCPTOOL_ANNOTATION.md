# Spring AI MCP Server 工具定义方式说明

## 重要说明

**Spring AI MCP Server 1.1.0 实际使用 Function Bean 方式定义工具，而不是 `@McpTool` 注解。**

`@McpTool` 注解可能在未来版本中提供，或者在其他 MCP 框架中存在，但在当前版本的 Spring AI MCP Server 中，标准方式是使用 `@Bean` + `@Description` 定义 Function。

## 正确的实现方式 (Spring AI 1.1.0)

### 方式 1: Function Bean (之前的实现)

```java
@Configuration
public class DictMcpToolsConfiguration {
    
    @Bean
    @Description("批量查询字段元数据定义")
    public Function<DictQueryRequest, DictQueryResponse> getDictDefByLongNameList() {
        return request -> {
            // 实现逻辑
            return response;
        };
    }
}
```

### 方式 2: Function Bean (Spring AI 1.1.0 标准方式) ✅ 当前使用

```java
@Configuration
public class DictMcpToolsConfiguration {
    
    @Bean
    @Description("批量查询字段元数据定义")
    public Function<DictQueryRequest, DictQueryResponse> getDictDefByLongNameList() {
        return request -> {
            // 实现逻辑
            return response;
        };
    }
}
```

**说明**: 这是 Spring AI MCP Server 1.1.0 的官方标准方式。

## Function Bean 方式的特点

虽然 Function Bean 看起来比直接方法定义稍显复杂，但这是 Spring AI MCP Server 1.1.0 的标准方式，具有以下特点：

### 1. **符合 Spring 函数式编程范式** ✅

Function Bean 是 Spring Framework 和 Spring Cloud Function 的标准方式：

```java
@Bean
@Description("工具描述")
public Function<Request, Response> toolName() {
    return request -> {
        // 处理逻辑
        return response;
    };
}
```

这种方式与 Spring Cloud Function 完全兼容，可以同时用于多种场景。

### 2. **代码更简洁直观** ✅

**Function Bean 方式** (嵌套较深):
```java
public Function<Request, Response> toolName() {
    return request -> {
        // 实际逻辑在 lambda 内部
        return response;
    };
}
```

**@McpTool 方式** (直接了当):
```java
public Response toolName(Request request) {
    // 实际逻辑直接在方法内
    return response;
}
```

### 3. **更好的类型推断** ✅

编译器和 IDE 可以直接识别方法签名，提供更好的代码补全和类型检查。

```java
// IDE 可以直接识别参数和返回类型
@McpTool(name = "getDictDef", description = "...")
public DictQueryResponse getDictDef(DictQueryRequest request) {
    //                              ^^^^^^^^^^^^^^^^ 
    //                              IDE 自动补全可用
}
```

### 4. **调试更方便** ✅

直接在方法上设置断点，而不需要在 lambda 表达式内部设置。

```java
@McpTool(name = "toolName", description = "...")
public Response toolName(Request request) {
    log.info("工具调用");  // ← 可以直接在这里设置断点
    // ...
    return response;
}
```

### 5. **工具元数据更清晰** ✅

```java
@McpTool(
    name = "getDictDefByLongNameList",              // 工具名称
    description = "批量查询字段元数据定义...",        // 工具描述
    // 未来可能支持更多元数据
    // tags = {"dictionary", "metadata"},
    // version = "1.0",
    // deprecated = false
)
```

### 6. **异常处理更直观** ✅

**Function Bean 方式**:
```java
public Function<Request, Response> toolName() {
    return request -> {
        try {
            // 逻辑
        } catch (Exception e) {
            // 异常处理在 lambda 内部
        }
    };
}
```

**@McpTool 方式**:
```java
@McpTool(name = "toolName", description = "...")
public Response toolName(Request request) throws CustomException {
    // 可以直接声明异常
    // 异常处理更清晰
}
```

### 7. **单元测试更容易** ✅

```java
@Test
void testGetDictDefByLongNameList() {
    DictMcpToolsConfiguration tools = new DictMcpToolsConfiguration(dictService);
    
    // 直接调用方法测试
    DictQueryResponse response = tools.getDictDefByLongNameList(request);
    
    assertNotNull(response);
}
```

而 Function Bean 需要先获取 Function 对象再调用 apply：

```java
@Test
void testGetDictDefByLongNameList() {
    Function<Request, Response> function = tools.getDictDefByLongNameList();
    Response response = function.apply(request);  // 需要额外调用 apply
}
```

### 8. **支持方法重载** ✅

```java
@McpTool(name = "getDictDef", description = "按中文名查询")
public DictQueryResponse getDictDef(String longName) { }

@McpTool(name = "getDictDefBatch", description = "批量查询")
public DictQueryResponse getDictDefBatch(List<String> longNames) { }
```

### 9. **与 Spring 生态集成更好** ✅

```java
@Component  // 使用 @Component 而不是 @Configuration
public class DictMcpToolsConfiguration {
    
    @Autowired
    private DictService dictService;
    
    @McpTool(name = "toolName", description = "...")
    @Transactional  // 可以直接使用 Spring 的其他注解
    @Cacheable("dictCache")
    public Response toolName(Request request) {
        // ...
    }
}
```

### 10. **更好的文档生成支持** ✅

未来可能支持从 `@McpTool` 注解自动生成 API 文档：

```java
@McpTool(
    name = "getDictDefByLongNameList",
    description = "批量查询字段元数据定义...",
    examples = {
        "输入: [\"客户编号\", \"交易金额\"]",
        "输出: { success: true, data: {...} }"
    }
)
```

## 性能对比

两种方式在运行时性能上基本相同，因为：

1. Function Bean 最终也是调用方法
2. @McpTool 方法会被 Spring AOP 代理，但开销可忽略
3. 都支持编译期优化

## 迁移指南

### 从 Function Bean 迁移到 @McpTool

**步骤 1**: 修改类注解

```java
// 从
@Configuration
public class DictMcpToolsConfiguration {

// 改为
@Component
public class DictMcpToolsConfiguration {
```

**步骤 2**: 修改方法定义

```java
// 从
@Bean
@Description("工具描述")
public Function<Request, Response> toolName() {
    return request -> {
        // 逻辑
        return response;
    };
}

// 改为
@McpTool(
    name = "toolName",
    description = "工具描述"
)
public Response toolName(Request request) {
    // 逻辑
    return response;
}
```

**步骤 3**: 添加导入

```java
import org.springframework.ai.mcp.server.McpTool;
import org.springframework.stereotype.Component;
```

## 什么时候使用 Function Bean？

在某些特殊情况下，Function Bean 仍然有用：

### 1. 动态生成工具

```java
@Bean
public Function<Request, Response> dynamicTool() {
    if (condition) {
        return request -> handleA(request);
    } else {
        return request -> handleB(request);
    }
}
```

### 2. 与 Spring Cloud Function 集成

```java
@Bean
public Function<Request, Response> cloudFunction() {
    // 可以同时用作 MCP 工具和 Spring Cloud Function
}
```

### 3. 函数组合

```java
@Bean
public Function<Request, Response> compositeTool() {
    return validateRequest()
        .andThen(processRequest())
        .andThen(formatResponse());
}
```

但对于绝大多数 MCP 工具场景，**@McpTool 注解是更好的选择**。

## 总结

| 特性 | Function Bean | @McpTool 注解 |
|------|--------------|---------------|
| 符合 MCP 规范 | ✓ | ✅ 更符合 |
| 代码简洁性 | ✓ | ✅ 更简洁 |
| 类型安全 | ✓ | ✅ 更好 |
| 调试体验 | ✓ | ✅ 更好 |
| 测试便利性 | ○ | ✅ 更好 |
| 异常处理 | ✓ | ✅ 更清晰 |
| 文档生成 | ○ | ✅ 支持 |
| Spring 集成 | ✓ | ✅ 更好 |
| 学习曲线 | 稍高 | ✅ 更低 |
| 推荐程度 | ○ | ✅ 强烈推荐 |

**结论**: 对于 Spring AI MCP Server 1.1.0，必须使用 Function Bean 方式定义工具。虽然代码相对繁琐，但这是框架的标准方式，提供了良好的类型安全和 Spring 生态集成。

## 参考资料

- [Spring AI MCP Server 文档](https://docs.spring.io/spring-ai/reference/api/mcp/)
- [Model Context Protocol 规范](https://modelcontextprotocol.io/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
