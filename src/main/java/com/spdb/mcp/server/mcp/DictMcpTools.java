package com.spdb.mcp.server.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.spdb.mcp.server.service.DictService;
import com.spdb.mcp.server.service.MavenConfigManager;
import com.spdb.mcp.server.service.MavenSettingsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dict MCP 工具实现
 * 使用 @McpTool 注解定义工具
 */
@Component
public class DictMcpTools {

    private static final Logger log = LoggerFactory.getLogger(DictMcpTools.class);

    private final DictService dictService;
    private final MavenSettingsReader mavenSettingsReader;
    private final MavenConfigManager configManager;

    public DictMcpTools(DictService dictService, MavenSettingsReader mavenSettingsReader, MavenConfigManager configManager) {
        this.dictService = dictService;
        this.mavenSettingsReader = mavenSettingsReader;
        this.configManager = configManager;
        log.info("DictMcpTools 已初始化");
    }

    /**
     * 配置 Maven 仓库路径
     * 支持保存到调用方工程的 .mvn/maven.config 文件
     */
    @McpTool(
        name = "configureMavenRepo",
        description = "配置 Maven 仓库路径并保存到 .mvn/maven.config。action参数: 'get'获取当前路径建议,'save'保存用户输入的路径。mavenRepoPath参数: 当action为'save'时必填,为用户输入的路径。workingDirectory: 可选,指定配置文件保存的工程根目录,如果不提供则保存到当前目录的 .mvn/maven.config。"
    )
    public MavenRepoConfigResponse configureMavenRepo(
            @McpToolParam(description = "操作类型: get获取路径建议, save保存路径", required = true) String action,
            @McpToolParam(description = "Maven 仓库路径（save 时必填）", required = false) String mavenRepoPath,
            @McpToolParam(description = "工作目录（flowtran 项目根目录），配置文件将保存到此目录的 .cursor/ 下", required = false) String workingDirectory) {
        
        log.info("MCP Tool [configureMavenRepo] - action: {}, path: {}, workingDir: {}", action, mavenRepoPath, workingDirectory);
        
        MavenRepoConfigResponse response = new MavenRepoConfigResponse();
        
        try {
            if ("get".equalsIgnoreCase(action)) {
                // 获取建议的路径（优先级: .mvn/maven.config > settings.xml > 默认）
                String suggestedPath = configManager.getMavenRepoPath(workingDirectory, mavenSettingsReader);
                
                response.setSuccess(true);
                response.setSuggestedPath(suggestedPath);
                response.setHasConfigFile(configManager.hasConfigFile(workingDirectory));
                response.setWorkingDirectory(workingDirectory != null ? workingDirectory : System.getProperty("user.dir"));
                
                if (configManager.hasConfigFile(workingDirectory)) {
                    String configLocation = workingDirectory != null ? workingDirectory + "/.mvn/maven.config" : ".mvn/maven.config";
                    response.setMessage("当前配置的路径: " + suggestedPath + " (来自 " + configLocation + ")");
                } else {
                    response.setMessage("建议的路径: " + suggestedPath + " (未配置,可输入自定义路径)");
                }
                
                log.info("返回建议路径: {}, 工作目录: {}", suggestedPath, response.getWorkingDirectory());
                
            } else if ("save".equalsIgnoreCase(action)) {
                // 保存用户输入的路径
                if (mavenRepoPath == null || mavenRepoPath.isEmpty()) {
                    response.setSuccess(false);
                    response.setMessage("保存失败: Maven 仓库路径不能为空");
                    return response;
                }
                
                // 验证路径是否存在
                if (!Files.exists(Paths.get(mavenRepoPath)) || !Files.isDirectory(Paths.get(mavenRepoPath))) {
                    response.setSuccess(false);
                    response.setMessage("保存失败: Maven 仓库路径不存在 - " + mavenRepoPath);
                    log.error("Maven 仓库路径不存在: {}", mavenRepoPath);
                    return response;
                }
                
                // 保存到指定工作目录的 .mvn/maven.config
                configManager.saveMavenRepoPath(workingDirectory, mavenRepoPath);
                
                // 设置并初始化字典服务
                dictService.setMavenRepo(mavenRepoPath);
                dictService.init();
                
                String savedLocation = workingDirectory != null ? workingDirectory + "/.mvn/maven.config" : ".mvn/maven.config";
                
                response.setSuccess(true);
                response.setConfiguredPath(mavenRepoPath);
                response.setSavedToConfig(true);
                response.setWorkingDirectory(workingDirectory != null ? workingDirectory : System.getProperty("user.dir"));
                response.setMessage("Maven 仓库路径配置成功并已保存到 " + savedLocation + ": " + mavenRepoPath);
                
                log.info("Maven 仓库路径已配置并保存到 {}: {}", savedLocation, mavenRepoPath);
                
            } else {
                response.setSuccess(false);
                response.setMessage("无效的 action 参数，支持的值: get, save");
                log.error("无效的 action: {}", action);
            }
        } catch (Exception e) {
            log.error("配置 Maven 仓库路径失败", e);
            response.setSuccess(false);
            response.setMessage("配置失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 批量查询字段元数据定义
     */
    @McpTool(
        name = "getDictDefByLongNameList",
        description = "批量查询字段元数据定义。输入字段中文名称列表和可选的Maven仓库路径。返回字段定义映射。未贯标的字段value为null。"
    )
    public DictQueryResponse getDictDefByLongNameList(
            @McpToolParam(description = "字段中文名称列表", required = true) List<String> longNameList,
            @McpToolParam(description = "可选的Maven仓库路径", required = false) String mavenRepoPath) {
        
        log.info("MCP Tool [getDictDefByLongNameList] - 字段数量: {}, Maven路径: {}", 
                longNameList.size(), mavenRepoPath != null ? mavenRepoPath : "使用已配置的路径");

        Map<String, Object> resultMap = dictService.getDictDefByLongNameList(longNameList, mavenRepoPath);

        List<String> unstandardizedFields = resultMap.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DictQueryResponse response = new DictQueryResponse();
        response.setSuccess(true);
        response.setTotal(longNameList.size());
        response.setFound(resultMap.size() - unstandardizedFields.size());
        response.setData(resultMap);
        
        if (!unstandardizedFields.isEmpty()) {
            String message = String.format("以下字段需要贯标处理: %s", String.join(", ", unstandardizedFields));
            response.setMessage(message);
            response.setUnstandardizedFields(unstandardizedFields);
        } else {
            response.setMessage("查询成功");
        }

        return response;
    }

    /**
     * 重新加载字典文件
     */
    @McpTool(
        name = "reloadDict",
        description = "重新加载字典文件。用于开发调试,当字典文件更新后可以重新加载而无需重启服务。"
    )
    public StatusResponse reloadDict() {
        log.info("MCP Tool [reloadDict] - 重新加载字典文件");
        try {
            dictService.reload();
            return new StatusResponse(true, "字典文件重新加载成功");
        } catch (Exception e) {
            log.error("重新加载字典失败", e);
            return new StatusResponse(false, "重新加载失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存统计信息
     */
    @McpTool(
        name = "getCacheStats",
        description = "获取字典缓存统计信息。返回当前缓存的字段总数和所有字段的中文名称列表。"
    )
    public CacheStatsResponse getCacheStats() {
        log.info("MCP Tool [getCacheStats] - 获取缓存统计");
        Map<String, Object> stats = dictService.getCacheStats();
        
        CacheStatsResponse response = new CacheStatsResponse();
        response.setTotalFields((Integer) stats.get("totalFields"));
        response.setCacheKeys(stats.get("cacheKeys"));
        response.setMessage("缓存统计查询成功");
        
        return response;
    }

    // ==================== 响应对象 ====================

    public static class MavenRepoConfigResponse {
        @JsonProperty("success")
        @JsonPropertyDescription("配置是否成功")
        private boolean success;

        @JsonProperty("message")
        @JsonPropertyDescription("响应消息")
        private String message;

        @JsonProperty("suggestedPath")
        @JsonPropertyDescription("建议的路径（get 时返回）")
        private String suggestedPath;

        @JsonProperty("configuredPath")
        @JsonPropertyDescription("已配置的路径（save 时返回）")
        private String configuredPath;

        @JsonProperty("hasConfigFile")
        @JsonPropertyDescription("是否已有 .mvn/maven.config 配置文件")
        private boolean hasConfigFile;

        @JsonProperty("savedToConfig")
        @JsonPropertyDescription("是否已保存到配置文件")
        private boolean savedToConfig;

        @JsonProperty("workingDirectory")
        @JsonPropertyDescription("工作目录（配置文件所在的项目根目录）")
        private String workingDirectory;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSuggestedPath() { return suggestedPath; }
        public void setSuggestedPath(String suggestedPath) { this.suggestedPath = suggestedPath; }
        public String getConfiguredPath() { return configuredPath; }
        public void setConfiguredPath(String configuredPath) { this.configuredPath = configuredPath; }
        public boolean isHasConfigFile() { return hasConfigFile; }
        public void setHasConfigFile(boolean hasConfigFile) { this.hasConfigFile = hasConfigFile; }
        public boolean isSavedToConfig() { return savedToConfig; }
        public void setSavedToConfig(boolean savedToConfig) { this.savedToConfig = savedToConfig; }
        public String getWorkingDirectory() { return workingDirectory; }
        public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }
    }

    public static class DictQueryResponse {
        @JsonProperty("success")
        private boolean success;
        @JsonProperty("message")
        private String message;
        @JsonProperty("total")
        private int total;
        @JsonProperty("found")
        private int found;
        @JsonProperty("data")
        private Map<String, Object> data;
        @JsonProperty("unstandardizedFields")
        private List<String> unstandardizedFields;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getFound() { return found; }
        public void setFound(int found) { this.found = found; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        public List<String> getUnstandardizedFields() { return unstandardizedFields; }
        public void setUnstandardizedFields(List<String> unstandardizedFields) { this.unstandardizedFields = unstandardizedFields; }
    }

    public static class StatusResponse {
        @JsonProperty("success")
        private boolean success;
        @JsonProperty("message")
        private String message;

        public StatusResponse() {}
        public StatusResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class CacheStatsResponse {
        @JsonProperty("totalFields")
        private Integer totalFields;
        @JsonProperty("cacheKeys")
        private Object cacheKeys;
        @JsonProperty("message")
        private String message;

        public Integer getTotalFields() { return totalFields; }
        public void setTotalFields(Integer totalFields) { this.totalFields = totalFields; }
        public Object getCacheKeys() { return cacheKeys; }
        public void setCacheKeys(Object cacheKeys) { this.cacheKeys = cacheKeys; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
