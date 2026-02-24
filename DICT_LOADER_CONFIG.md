# 字典加载器配置说明

本项目支持两种字典文件加载方式，通过配置文件 `application.yml` 切换。

## 加载器类型

### 1. GitLab 仓库加载（推荐）

从 GitLab 仓库直接加载 `MDict.d_schema.xml` 文件。

**优势**：
- 始终使用最新版本（每 20 秒自动刷新）
- 无需本地 Maven 仓库
- 自动同步更新
- 适合团队协作
- 支持热更新，无需重启服务

**配置示例**：

```yaml
dict:
  loader:
    type: gitlab  # 使用 GitLab 加载器
  
  # 定时刷新配置（仅 GitLab 加载器生效）
  refresh:
    enabled: true        # 是否启用定时刷新
    interval: 20000      # 刷新间隔（毫秒），默认 20 秒
  
  gitlab:
    url: http://gitlab.spdb.com
    project-id: 64142
    branch: master
    file-path: src/main/resources/dict/MDict.d_schema.xml
    
    # 认证方式 1: 使用 Token（推荐）
    token: WScw9XyM8hM1sYKLAz8o
    
    # 认证方式 2: 使用用户名密码（Token 为空时使用）
    username: c-wangsh8
    password: Liang*201314
```

**定时刷新机制**：

- 启动 20 秒后开始首次定时刷新
- 之后每 20 秒刷新一次
- 刷新时使用**双缓存机制**：
  - 旧缓存继续提供查询服务（不阻塞客户端）
  - 新数据加载并解析完成后，原子性切换缓存
- 刷新失败不影响当前服务（保留旧缓存）
- 连续失败 10 次自动暂停刷新

### 2. Maven 本地仓库加载

从本地 Maven 仓库的 JAR 包中加载字典文件。

**优势**：
- 离线可用
- 加载速度快
- 无需网络连接

**配置示例**：

```yaml
dict:
  loader:
    type: maven  # 使用 Maven 加载器
  
  group-id: com.spdb.ccbs
  artifact-id: dictionary
  version: 1.0.0-SNAPSHOT
  xml-path: dict/MDict.d_schema.xml
```

**注意**：使用 Maven 加载器时，需要通过 MCP 工具 `configureMavenRepo` 配置本地仓库路径。

## 定时刷新机制（GitLab 加载器）

### 工作原理

```
启动 → 初始加载 → 20秒后 → 定时刷新 → 每 20 秒循环
         ↓                    ↓
      缓存 A              缓存 A（继续服务）
                              ↓
                          加载新数据到临时缓存 B
                              ↓
                          原子性切换：缓存 A → 缓存 B
                              ↓
                          缓存 B（继续服务）
```

### 缓冲保护机制

1. **并发控制**：使用 `AtomicBoolean` 防止并发加载
   - 加载中时，跳过新的刷新请求
   - 确保同一时间只有一个加载任务
   
2. **双缓存策略**：
   - 使用 `volatile` 关键字确保缓存可见性
   - 新数据加载到临时 Map
   - 解析完成后原子性切换缓存引用
   - 客户端查询始终访问稳定的缓存
   
3. **故障保护**：
   - 刷新失败保留旧缓存
   - 连续失败 5 次发出警告
   - 连续失败 10 次自动暂停刷新
   
4. **性能优化**：
   - 查询操作直接访问 Map（O(1) 时间复杂度）
   - 加载和查询完全解耦
   - 刷新不阻塞客户端请求

### 刷新配置

```yaml
dict:
  refresh:
    enabled: true        # 是否启用定时刷新
    interval: 20000      # 刷新间隔（毫秒）
```

**调整间隔**：

- 频繁更新场景：10000（10 秒）
- 标准场景：20000（20 秒，默认）
- 稳定场景：60000（60 秒）

**禁用刷新**：

```yaml
dict:
  refresh:
    enabled: false
```

### 刷新日志

**成功**：

```
开始定时刷新字典缓存...
从 GitLab 加载字典文件...
成功加载字典文件, 大小: 245678 bytes
字典缓存刷新成功, 耗时: 1234 ms, 字段数: 1523 -> 1525
```

**失败**：

```
字典缓存刷新失败 (连续失败 3 次): GitLab 连接超时
```

**连续失败警告**：

```
⚠️  字典缓存连续刷新失败 5 次，请检查 GitLab 连接
```

**暂停刷新**：

```
❌ 字典缓存连续刷新失败 10 次，暂停刷新，请检查配置
```

## 配置切换

在 `application.yml` 中修改 `dict.loader.type` 即可切换：

```yaml
dict:
  loader:
    type: gitlab  # 或 maven
```

重启服务后生效。

## 完整配置示例

### application.yml（GitLab 模式）

```yaml
server:
  port: 8080

spring:
  application:
    name: dict-mcp-server

dict:
  # 加载器类型: gitlab 或 maven
  loader:
    type: gitlab
  
  # GitLab 配置
  gitlab:
    url: http://gitlab.spdb.com
    project-id: 64142
    branch: master
    file-path: src/main/resources/dict/MDict.d_schema.xml
    token: WScw9XyM8hM1sYKLAz8o
    username: c-wangsh8
    password: Liang*201314
  
  # Maven 配置（loader.type=maven 时使用）
  group-id: com.spdb.ccbs
  artifact-id: dictionary
  version: 1.0.0-SNAPSHOT
  xml-path: dict/MDict.d_schema.xml
```

### application.yml（Maven 模式）

```yaml
server:
  port: 8080

spring:
  application:
    name: dict-mcp-server

dict:
  # 加载器类型: gitlab 或 maven
  loader:
    type: maven
  
  # Maven 配置
  group-id: com.spdb.ccbs
  artifact-id: dictionary
  version: 1.0.0-SNAPSHOT
  xml-path: dict/MDict.d_schema.xml
  
  # GitLab 配置（loader.type=gitlab 时使用）
  gitlab:
    url: http://gitlab.spdb.com
    project-id: 64142
    branch: master
    file-path: src/main/resources/dict/MDict.d_schema.xml
    token: WScw9XyM8hM1sYKLAz8o
```

## 启动日志

### GitLab 模式

```
========================================
Dict MCP Server 已启动
========================================

字典加载器: GitLab 仓库

正在从 GitLab 加载字典文件...
✓ 字典文件加载成功
  - 已缓存字段数: 1523

========================================
```

### Maven 模式

```
========================================
Dict MCP Server 已启动
========================================

字典加载器: Maven 本地仓库

提示: 请通过 MCP 工具 configureMavenRepo 配置 Maven 仓库路径
使用步骤:
1. 调用 configureMavenRepo 工具，action='detect' 检测路径
2. 根据返回的提示，调用 action='use' 使用检测到的路径
3. 或调用 action='custom' 使用自定义路径

配置完成后，即可使用 getDictDefByLongNameList 查询字典
========================================
```

## 故障排查

### GitLab 加载失败

**错误**：`GitLab 连接测试失败`

**检查项**：

1. GitLab URL 是否正确
2. Project ID 是否正确
3. Token 或用户名密码是否正确
4. 网络是否可以访问 GitLab
5. Token 权限是否包含 `read_repository`

### Maven 加载失败

**错误**：`Maven 仓库路径未设置`

**解决方法**：

调用 MCP 工具 `configureMavenRepo` 配置路径。

## 安全建议

**生产环境**：

1. 不要在配置文件中明文存储密码和 Token
2. 使用环境变量或 Spring Cloud Config 管理敏感信息
3. 使用 Token 而非用户名密码
4. 定期轮换 Token

**配置环境变量示例**：

```yaml
dict:
  gitlab:
    token: ${GITLAB_TOKEN}
    username: ${GITLAB_USERNAME}
    password: ${GITLAB_PASSWORD}
```

启动时设置环境变量：

```bash
export GITLAB_TOKEN=WScw9XyM8hM1sYKLAz8o
java -jar dict-mcp-server.jar
```
