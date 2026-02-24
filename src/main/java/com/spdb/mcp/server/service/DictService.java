package com.spdb.mcp.server.service;

import com.spdb.mcp.server.model.FieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 字典服务 - 负责加载和查询字典元数据
 */
@Service
public class DictService {

    private static final Logger log = LoggerFactory.getLogger(DictService.class);

    private final DictSchemaParser schemaParser;
    private final DictLoader dictLoader;

    @Value("${dict.loader.type:maven}")
    private String loaderType;

    public DictService(DictSchemaParser schemaParser, DictLoader dictLoader) {
        this.schemaParser = schemaParser;
        this.dictLoader = dictLoader;
    }

    private String mavenRepo;

    /**
     * 字段元数据缓存 Map<longname, FieldDefinition>
     * 使用 volatile 确保线程安全的可见性
     */
    private volatile Map<String, FieldDefinition> fieldCache = new HashMap<>();
    
    /**
     * 加载中标志，防止并发加载
     */
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    /**
     * 设置 Maven 仓库路径
     */
    public void setMavenRepo(String mavenRepo) {
        this.mavenRepo = mavenRepo;
    }

    /**
     * 初始化 - 加载字典文件（首次启动）
     */
    public void init() {
        // 首次加载不使用并发控制
        loadDictInternal(false);
    }
    
    /**
     * 内部加载方法（支持并发控制）
     * 
     * @param useConcurrencyControl 是否使用并发控制（定时刷新时为 true）
     */
    private void loadDictInternal(boolean useConcurrencyControl) {
        // 如果需要并发控制，检查加载标志
        if (useConcurrencyControl) {
            if (!isLoading.compareAndSet(false, true)) {
                log.debug("字典正在加载中，跳过本次加载");
                return;
            }
        }

        try {
            log.info("开始加载字典文件...");
            log.info("加载器类型: {}", dictLoader.getLoaderType());

            // 对于 Maven 加载器，需要先设置仓库路径
            if (dictLoader instanceof MavenDictLoader) {
                if (mavenRepo == null || mavenRepo.isEmpty()) {
                    log.error("Maven 仓库路径未设置，无法加载字典文件");
                    throw new IllegalStateException("Maven 仓库路径未设置");
                }
                ((MavenDictLoader) dictLoader).setMavenRepo(mavenRepo);
                log.info("Maven 仓库: {}", mavenRepo);
            }

            // 使用加载器加载字典文件
            Map<String, FieldDefinition> newCache;
            try (InputStream xmlStream = dictLoader.loadDictFile()) {
                if (xmlStream == null) {
                    log.error("加载字典文件失败：返回 null");
                    return;
                }

                // 解析 XML 到新的临时缓存（不影响当前查询）
                newCache = schemaParser.parseSchema(xmlStream);
            }

            // 原子性切换缓存（确保查询不受影响）
            int oldSize = fieldCache.size();
            fieldCache = newCache;
            int newSize = fieldCache.size();

            log.info("字典文件加载成功, 字段数: {} -> {}", oldSize, newSize);

        } catch (Exception e) {
            log.error("加载字典服务失败", e);
            if (!useConcurrencyControl) {
                // 首次加载失败，抛出异常
                throw new RuntimeException("初始化字典服务失败", e);
            }
            // 定时刷新失败，保留旧缓存继续服务
        } finally {
            if (useConcurrencyControl) {
                isLoading.set(false);
            }
        }
    }

    /**
     * 批量查询字段定义
     *
     * @param longNameList 字段中文名称列表
     * @param mavenRepoPath 可选的 Maven 仓库路径（仅 Maven 加载器使用）
     * @return Map<longname, FieldDefinition> (找不到的返回 null)
     */
    public Map<String, Object> getDictDefByLongNameList(List<String> longNameList, String mavenRepoPath) {
        // 如果使用 Maven 加载器，处理 mavenRepoPath 参数
        if (dictLoader instanceof MavenDictLoader) {
            if (mavenRepoPath != null && !mavenRepoPath.isEmpty()) {
                if (!mavenRepoPath.equals(this.mavenRepo)) {
                    log.info("检测到新的 Maven 仓库路径，重新加载字典: {}", mavenRepoPath);
                    this.mavenRepo = mavenRepoPath;
                    init();
                }
            } else {
                // 如果没有提供路径且当前路径也未设置，抛出异常
                if (this.mavenRepo == null || this.mavenRepo.isEmpty()) {
                    log.error("Maven 仓库路径未设置，无法查询字典");
                    throw new IllegalStateException("Maven 仓库路径未设置，请先调用 configureMavenRepo 配置路径");
                }
            }
        } else if (dictLoader instanceof GitLabDictLoader) {
            // GitLab 加载器：确保已初始化
            if (fieldCache.isEmpty()) {
                log.info("字典缓存为空，执行初始化加载");
                init();
            }
        }

        Map<String, Object> result = new HashMap<>();

        log.debug("批量查询字段, 数量: {}", longNameList.size());

        for (String longName : longNameList) {
            FieldDefinition field = fieldCache.get(longName);

            if (field != null) {
                log.debug("找到字段: {} -> {}", longName, field.getId());
                result.put(longName, field);
            } else {
                log.warn("字段未贯标: {}", longName);
                result.put(longName, null);
            }
        }

        log.info("批量查询完成, 查询 {} 个, 找到 {} 个, 未贯标 {} 个",
                longNameList.size(),
                result.values().stream().filter(Objects::nonNull).count(),
                result.values().stream().filter(Objects::isNull).count());

        return result;
    }


    /**
     * 重新加载字典文件（用于定时刷新和手动刷新）
     */
    public void reload() {
        log.info("重新加载字典文件...");
        loadDictInternal(true);  // 使用并发控制
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
                "totalFields", fieldCache.size(),
                "cacheKeys", fieldCache.keySet()
        );
    }

    /**
     * 获取当前配置的 Maven 仓库路径
     */
    public String getCurrentMavenRepo() {
        if (dictLoader instanceof MavenDictLoader) {
            return ((MavenDictLoader) dictLoader).getCurrentMavenRepo();
        }
        return mavenRepo;
    }
    
    /**
     * 获取加载器信息
     */
    public Map<String, Object> getLoaderInfo() {
        return Map.of(
                "loaderType", dictLoader.getLoaderType(),
                "connectionStatus", dictLoader.testConnection() ? "connected" : "disconnected",
                "cacheSize", fieldCache.size()
        );
    }
}
