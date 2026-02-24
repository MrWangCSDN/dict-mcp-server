package com.spdb.mcp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Maven 配置管理器
 * 负责读写工作目录下的 .mvn/maven.config 文件
 * 
 * 注意: 配置文件保存在调用方工程的 .mvn 目录下，而不是 dict-mcp-server 项目下
 */
@Service
public class MavenConfigManager {

    private static final Logger log = LoggerFactory.getLogger(MavenConfigManager.class);
    private static final String CONFIG_FILE_NAME = ".mvn/maven.config";

    /**
     * 获取 Maven 仓库路径
     * 优先级: {workingDir}/.cursor/maven.config > Maven settings.xml > 默认路径
     * 
     * @param workingDir 工作目录（调用方项目根目录），如果为 null 则使用当前目录
     */
    public String getMavenRepoPath(String workingDir, MavenSettingsReader settingsReader) {
        // 1. 优先从指定工作目录的 .cursor/maven.config 读取
        String configPath = readFromConfigFile(workingDir);
        if (configPath != null && !configPath.isEmpty()) {
            String configFileLocation = getConfigFilePath(workingDir);
            log.info("从 {} 读取 Maven 仓库路径: {}", configFileLocation, configPath);
            return configPath;
        }

        // 2. 其次从 Maven settings.xml 读取
        String settingsPath = settingsReader.getLocalRepositoryPath();
        if (settingsPath != null && !settingsPath.isEmpty()) {
            log.info("从 Maven settings.xml 读取仓库路径: {}", settingsPath);
            return settingsPath;
        }

        // 3. 最后使用默认路径
        String userHome = System.getProperty("user.home");
        String defaultPath = Paths.get(userHome, ".m2", "repository").toString();
        log.info("使用默认 Maven 仓库路径: {}", defaultPath);
        return defaultPath;
    }

    /**
     * 保存 Maven 仓库路径到指定工作目录的 .cursor/maven.config
     * 
     * @param workingDir 工作目录（调用方项目根目录），如果为 null 则使用当前目录
     * @param mavenRepoPath Maven 仓库路径
     */
    public void saveMavenRepoPath(String workingDir, String mavenRepoPath) {
        try {
            String configFilePath = getConfigFilePath(workingDir);
            Path configFile = Paths.get(configFilePath);
            
            // 确保 .cursor 目录存在
            Path cursorDir = configFile.getParent();
            if (cursorDir != null && !Files.exists(cursorDir)) {
                Files.createDirectories(cursorDir);
                log.info("创建目录: {}", cursorDir.toAbsolutePath());
            }

            // 写入配置文件
            Files.writeString(configFile, mavenRepoPath, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("Maven 仓库路径已保存到 {}: {}", configFile.toAbsolutePath(), mavenRepoPath);
            
        } catch (IOException e) {
            log.error("保存 Maven 配置失败", e);
            throw new RuntimeException("保存 Maven 配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从指定工作目录的 .cursor/maven.config 读取路径
     */
    private String readFromConfigFile(String workingDir) {
        try {
            String configFilePath = getConfigFilePath(workingDir);
            Path configFile = Paths.get(configFilePath);
            
            if (!Files.exists(configFile)) {
                log.debug("{} 文件不存在", configFilePath);
                return null;
            }

            String path = Files.readString(configFile).trim();
            
            if (path.isEmpty()) {
                log.debug("{} 文件为空", configFilePath);
                return null;
            }

            log.debug("从 {} 读取到路径: {}", configFilePath, path);
            return path;
            
        } catch (IOException e) {
            log.warn("读取 .cursor/maven.config 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查指定工作目录的 .cursor/maven.config 是否存在
     */
    public boolean hasConfigFile(String workingDir) {
        String configFilePath = getConfigFilePath(workingDir);
        return Files.exists(Paths.get(configFilePath));
    }

    /**
     * 获取配置文件的完整路径
     * 
     * @param workingDir 工作目录，如果为 null 则使用当前目录
     */
    private String getConfigFilePath(String workingDir) {
        if (workingDir == null || workingDir.isEmpty()) {
            // 如果未指定工作目录，使用当前目录
            return CONFIG_FILE_NAME;
        } else {
            // 使用指定的工作目录
            return Paths.get(workingDir, CONFIG_FILE_NAME).toString();
        }
    }
}

