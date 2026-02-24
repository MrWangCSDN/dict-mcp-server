# 内网部署指南

## 版本信息

- **版本号**: 0224-1.0
- **打包文件**: `dict-mcp-server-0224-1.0.zip`
- **打包大小**: 185 KB

## 部署步骤

### 1. 解压源码包

```bash
unzip dict-mcp-server-0224-1.0.zip -d dict-mcp-server
cd dict-mcp-server
```

### 2. 确认环境

**必需环境**：

- Java 17 或更高版本
- Maven 3.6+ 或 Gradle（用于构建）
- 网络访问内网 GitLab（如果使用 GitLab 加载器）

**验证 Java 版本**：

```bash
java -version
# 应显示 17 或更高版本
```

**验证 Maven**：

```bash
mvn -version
```

### 3. 配置字典加载方式

编辑 `src/main/resources/application.yml`：

#### 选项 1：使用内网 GitLab（推荐）

```yaml
dict:
  loader:
    type: gitlab
  
  refresh:
    enabled: true
    interval: 20000  # 20 秒刷新一次
  
  gitlab:
    url: http://内网gitlab地址
    project-id: 项目ID
    branch: master
    file-path: src/main/resources/dict/MDict.d_schema.xml
    token: 你的token
```

#### 选项 2：使用 Maven 本地仓库

```yaml
dict:
  loader:
    type: maven
  
  group-id: com.spdb.ccbs
  artifact-id: dictionary
  version: 1.0.0-SNAPSHOT
  xml-path: dict/MDict.d_schema.xml
```

### 4. 构建项目

#### 使用 Maven 构建

```bash
mvn clean package -DskipTests
```

构建完成后，JAR 包位于：

```
target/dict-mcp-server-0224-1.0.jar
```

### 5. 运行服务

#### 前台运行

```bash
java -jar target/dict-mcp-server-0224-1.0.jar
```

#### 后台运行

```bash
nohup java -jar target/dict-mcp-server-0224-1.0.jar > dict-mcp-server.log 2>&1 &
```

#### 使用环境变量（推荐）

```bash
export GITLAB_TOKEN=你的token
java -jar target/dict-mcp-server-0224-1.0.jar
```

对应配置：

```yaml
dict:
  gitlab:
    token: ${GITLAB_TOKEN}
```

### 6. 验证服务

**检查服务状态**：

```bash
curl http://localhost:8080/actuator/health
```

预期返回：

```json
{"status":"UP"}
```

**查看服务日志**：

```bash
tail -f dict-mcp-server.log
```

预期看到：

```
Dict MCP Server 已启动
字典加载器: GitLab Repository
正在从 GitLab 加载字典文件...
✓ 字典文件加载成功
  - 已缓存字段数: 1523
```

## 内网特殊配置

### Maven 依赖下载

如果内网无法访问公网 Maven 仓库：

#### 方案 1：配置内网 Maven 镜像

编辑 `~/.m2/settings.xml`：

```xml
<mirrors>
  <mirror>
    <id>internal-maven</id>
    <url>http://内网maven仓库地址/repository/maven-public/</url>
    <mirrorOf>*</mirrorOf>
  </mirror>
</mirrors>
```

#### 方案 2：离线构建

在有网络的机器上：

```bash
# 下载所有依赖到本地
mvn dependency:go-offline

# 打包项目和依赖
mvn package -DskipTests
```

将整个项目和 `~/.m2/repository/` 打包转移到内网。

### GitLab 内网地址配置

```yaml
dict:
  gitlab:
    url: http://内网gitlab.公司域名
    project-id: 64142
    token: 内网GitLab的token
```

### 防火墙配置

确保以下端口可访问：

- **8080**：MCP 服务端口
- **GitLab 端口**：通常是 80 或 443

## 目录结构

```
dict-mcp-server-0224-1.0/
├── pom.xml                          # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/                    # Java 源码
│   │   └── resources/
│   │       └── application.yml      # 配置文件
│   └── test/                        # 测试代码
├── .cursor/                         # Cursor 技能
├── .trae/                           # Trae 技能
├── .gitignore
├── README.md
├── QUICKSTART.md                    # 快速开始
├── USAGE.md                         # 使用说明
├── DICT_LOADER_CONFIG.md            # 加载器配置
├── CACHE_MECHANISM.md               # 缓存机制
├── README_DICT_LOADER.md            # 加载器指南
└── 复合类型元数据.md                 # 元数据说明
```

## 常见问题

### Q1: 内网无法访问 Maven 中央仓库

**A**: 配置内网 Maven 镜像或使用离线构建。

### Q2: GitLab 连接失败

**A**: 检查：
1. GitLab URL 是否正确
2. Project ID 是否正确
3. Token 是否有效且有 `read_repository` 权限
4. 网络是否可以访问内网 GitLab

### Q3: 服务启动但字典加载失败

**A**: 查看日志：

```bash
tail -100 dict-mcp-server.log | grep -i error
```

常见原因：
- GitLab 文件路径不正确
- Token 权限不足
- 网络连接问题

### Q4: 如何切换加载方式

**A**: 修改 `application.yml` 中的 `dict.loader.type`：

```yaml
dict:
  loader:
    type: gitlab  # 或 maven
```

重启服务生效。

## 监控与维护

### 查看缓存状态

通过 MCP 工具查询或直接访问：

```bash
curl http://localhost:8080/actuator/info
```

### 手动刷新缓存

重启服务或等待定时任务（20 秒）。

### 查看日志

```bash
# 实时查看
tail -f dict-mcp-server.log

# 查看错误
grep ERROR dict-mcp-server.log

# 查看刷新日志
grep "刷新字典缓存" dict-mcp-server.log
```

## 性能调优

### 调整刷新间隔

```yaml
dict:
  refresh:
    interval: 30000  # 改为 30 秒
```

### 调整 JVM 参数

```bash
java -Xms512m -Xmx1024m -jar target/dict-mcp-server-0224-1.0.jar
```

## 升级说明

1. 停止服务
2. 备份当前版本
3. 解压新版本源码包
4. 复制配置文件（`application.yml`）
5. 重新构建并启动

## 技术支持

- GitHub 仓库：https://github.com/MrWangCSDN/dict-mcp-server
- 文档：查看项目根目录的 `*.md` 文件
