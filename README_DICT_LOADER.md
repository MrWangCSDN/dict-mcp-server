# 字典加载器使用说明

## 概述

本服务支持两种字典文件加载方式：

1. **GitLab 仓库加载**：从 GitLab 仓库实时加载最新的 `MDict.d_schema.xml` 文件
2. **Maven 本地仓库加载**：从本地 Maven 仓库的 JAR 包中加载字典文件

## 快速开始

### 使用 GitLab 加载器（推荐）

**1. 修改 `application.yml`**

```yaml
dict:
  loader:
    type: gitlab
  gitlab:
    url: http://gitlab.spdb.com
    project-id: 64142
    branch: master
    file-path: src/main/resources/dict/MDict.d_schema.xml
    token: YOUR_GITLAB_TOKEN
```

**2. 启动服务**

```bash
mvn spring-boot:run
```

**3. 验证**

服务启动后会自动从 GitLab 加载字典文件，控制台会显示：

```
✓ 字典文件加载成功
  - 已缓存字段数: 1523
```

### 使用 Maven 加载器

**1. 修改 `application.yml`**

```yaml
dict:
  loader:
    type: maven
  group-id: com.spdb.ccbs
  artifact-id: dictionary
  version: 1.0.0-SNAPSHOT
  xml-path: dict/MDict.d_schema.xml
```

**2. 启动服务**

```bash
mvn spring-boot:run
```

**3. 配置 Maven 仓库路径**

通过 MCP 工具 `configureMavenRepo` 配置本地仓库路径。

## 配置参数说明

### GitLab 加载器配置

| 参数 | 必填 | 默认值 | 说明 |
|------|-----|--------|------|
| `dict.loader.type` | 是 | - | 填 `gitlab` |
| `dict.gitlab.url` | 是 | `http://gitlab.spdb.com` | GitLab 地址 |
| `dict.gitlab.project-id` | 是 | `64142` | 项目 ID |
| `dict.gitlab.branch` | 否 | `master` | 分支名 |
| `dict.gitlab.file-path` | 是 | `src/main/resources/dict/MDict.d_schema.xml` | 文件路径 |
| `dict.gitlab.token` | 推荐 | - | GitLab Token（优先） |
| `dict.gitlab.username` | 否 | - | GitLab 用户名 |
| `dict.gitlab.password` | 否 | - | GitLab 密码 |

**认证优先级**：Token > 用户名密码

### Maven 加载器配置

| 参数 | 必填 | 默认值 | 说明 |
|------|-----|--------|------|
| `dict.loader.type` | 是 | - | 填 `maven` |
| `dict.group-id` | 是 | - | Maven groupId |
| `dict.artifact-id` | 是 | - | Maven artifactId |
| `dict.version` | 是 | - | Maven version |
| `dict.xml-path` | 是 | - | JAR 包内 XML 文件路径 |

## 架构说明

### 类图

```
DictLoader (接口)
├── GitLabDictLoader (实现)
│   └── 从 GitLab 仓库加载
└── MavenDictLoader (实现)
    └── 从本地 Maven 仓库加载

DictService
└── 依赖 DictLoader（策略模式）
```

### 加载流程

```
启动 → 读取配置 → 选择加载器 → 加载字典文件 → 解析 XML → 缓存字段
```

## 使用示例

### 示例 1：GitLab 加载器

**配置文件**：

```yaml
dict:
  loader:
    type: gitlab
  gitlab:
    url: http://gitlab.spdb.com
    project-id: 64142
    branch: master
    file-path: src/main/resources/dict/MDict.d_schema.xml
    token: ${GITLAB_TOKEN}  # 从环境变量读取
```

**启动命令**：

```bash
export GITLAB_TOKEN=WScw9XyM8hM1sYKLAz8o
mvn spring-boot:run
```

**日志输出**：

```
Dict MCP Server 已启动
字典加载器: GitLab 仓库
正在从 GitLab 加载字典文件...
使用 Token 连接 GitLab: http://gitlab.spdb.com
GitLab 连接成功, 项目 ID: 64142
项目 ID: 64142, 分支: master, 文件路径: src/main/resources/dict/MDict.d_schema.xml
成功加载字典文件, 大小: 245678 bytes
✓ 字典文件加载成功
  - 已缓存字段数: 1523
```

### 示例 2：Maven 加载器

**配置文件**：

```yaml
dict:
  loader:
    type: maven
  group-id: com.spdb.ccbs
  artifact-id: dictionary
  version: 1.0.0-SNAPSHOT
  xml-path: dict/MDict.d_schema.xml
```

**启动并配置**：

```bash
mvn spring-boot:run

# 然后通过 MCP 工具配置 Maven 仓库路径
# configureMavenRepo(action="custom", mavenRepoPath="/path/to/maven/repo")
```

## 切换加载器

修改 `application.yml` 中的 `dict.loader.type`，重启服务即可：

```yaml
# 切换到 GitLab 加载器
dict:
  loader:
    type: gitlab

# 或切换到 Maven 加载器
dict:
  loader:
    type: maven
```

## 故障排查

### GitLab 加载器常见问题

**问题 1：连接失败**

```
错误: 初始化 GitLab API 失败
```

**解决方法**：
- 检查 GitLab URL 是否正确
- 检查网络是否可以访问 GitLab
- 检查 Token 或用户名密码是否正确

**问题 2：文件不存在**

```
错误: GitLab 文件不存在: src/main/resources/dict/MDict.d_schema.xml
```

**解决方法**：
- 检查 project-id 是否正确
- 检查 branch 是否正确
- 检查 file-path 是否正确
- 确认 Token 有 `read_repository` 权限

### Maven 加载器常见问题

**问题：Maven 仓库路径未设置**

```
错误: Maven 仓库路径未设置
```

**解决方法**：
调用 MCP 工具 `configureMavenRepo` 配置路径。

## API 说明

### 新增方法

**`getLoaderInfo()`**

返回加载器信息：

```json
{
  "loaderType": "GitLab Repository",
  "connectionStatus": "connected",
  "cacheSize": 1523
}
```

**`testConnection()`**

测试加载器连接状态：

```java
boolean isConnected = dictService.testConnection();
```

## 安全建议

1. **不要在代码中硬编码密码和 Token**
2. **使用环境变量**：
   ```yaml
   dict:
     gitlab:
       token: ${GITLAB_TOKEN}
       username: ${GITLAB_USERNAME}
       password: ${GITLAB_PASSWORD}
   ```
3. **生产环境使用 Token 而非密码**
4. **定期轮换 Token**
5. **限制 Token 权限**：仅 `read_repository`

## 性能对比

| 加载器 | 首次加载时间 | 后续查询 | 离线可用 |
|-------|------------|---------|---------|
| GitLab | 2-5 秒（网络依赖） | 毫秒级（缓存） | 否 |
| Maven | 1-2 秒 | 毫秒级（缓存） | 是 |

**建议**：
- 开发环境：使用 GitLab（始终最新）
- 生产环境：根据网络情况选择（稳定网络用 GitLab，离线环境用 Maven）
