package com.spdb.mcp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 字典缓存定时刷新器（仅 GitLab 加载器启用）
 */
@Component
@ConditionalOnProperty(name = "dict.loader.type", havingValue = "gitlab")
public class DictCacheRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(DictCacheRefreshScheduler.class);

    @Autowired
    private DictService dictService;

    @Value("${dict.refresh.interval:20000}")
    private long refreshInterval;

    @Value("${dict.refresh.enabled:true}")
    private boolean refreshEnabled;

    /**
     * 加载中标志，防止并发加载
     */
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    /**
     * 上次成功加载时间
     */
    private final AtomicLong lastSuccessTime = new AtomicLong(0);

    /**
     * 连续失败次数
     */
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    /**
     * 定时刷新字典缓存（每 20 秒执行一次）
     */
    @Scheduled(fixedDelayString = "${dict.refresh.interval:20000}", initialDelay = 20000)
    public void refreshDictCache() {
        if (!refreshEnabled) {
            log.debug("字典刷新已禁用");
            return;
        }

        // 防止并发加载
        if (!isLoading.compareAndSet(false, true)) {
            log.debug("字典正在加载中，跳过本次刷新");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            log.info("开始定时刷新字典缓存...");

            // 获取刷新前的缓存大小
            int oldCacheSize = dictService.getCacheStats().get("totalFields") != null ? 
                    (int) dictService.getCacheStats().get("totalFields") : 0;

            // 重新加载字典（使用新的临时缓存，避免影响当前查询）
            dictService.reload();

            // 获取刷新后的缓存大小
            int newCacheSize = dictService.getCacheStats().get("totalFields") != null ? 
                    (int) dictService.getCacheStats().get("totalFields") : 0;

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info("字典缓存刷新成功, 耗时: {} ms, 字段数: {} -> {}", 
                    elapsedTime, oldCacheSize, newCacheSize);

            // 更新成功时间，重置失败计数
            lastSuccessTime.set(System.currentTimeMillis());
            consecutiveFailures.set(0);

        } catch (Exception e) {
            long failures = consecutiveFailures.incrementAndGet();
            log.error("字典缓存刷新失败 (连续失败 {} 次): {}", failures, e.getMessage());

            // 连续失败超过 5 次，发出警告
            if (failures >= 5) {
                log.error("⚠️  字典缓存连续刷新失败 {} 次，请检查 GitLab 连接", failures);
            }

            // 连续失败超过 10 次，暂停刷新
            if (failures >= 10) {
                log.error("❌ 字典缓存连续刷新失败 {} 次，暂停刷新，请检查配置", failures);
                refreshEnabled = false;
            }

        } finally {
            isLoading.set(false);
        }
    }

    /**
     * 获取刷新状态
     */
    public RefreshStatus getRefreshStatus() {
        return RefreshStatus.builder()
                .enabled(refreshEnabled)
                .loading(isLoading.get())
                .lastSuccessTime(lastSuccessTime.get())
                .consecutiveFailures(consecutiveFailures.get())
                .refreshInterval(refreshInterval)
                .build();
    }

    /**
     * 手动触发刷新
     */
    public void manualRefresh() {
        log.info("手动触发字典缓存刷新");
        refreshDictCache();
    }

    /**
     * 重新启用定时刷新
     */
    public void enableRefresh() {
        log.info("重新启用字典定时刷新");
        refreshEnabled = true;
        consecutiveFailures.set(0);
    }

    /**
     * 刷新状态类
     */
    public static class RefreshStatus {
        private boolean enabled;
        private boolean loading;
        private long lastSuccessTime;
        private long consecutiveFailures;
        private long refreshInterval;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private RefreshStatus status = new RefreshStatus();

            public Builder enabled(boolean enabled) {
                status.enabled = enabled;
                return this;
            }

            public Builder loading(boolean loading) {
                status.loading = loading;
                return this;
            }

            public Builder lastSuccessTime(long lastSuccessTime) {
                status.lastSuccessTime = lastSuccessTime;
                return this;
            }

            public Builder consecutiveFailures(long consecutiveFailures) {
                status.consecutiveFailures = consecutiveFailures;
                return this;
            }

            public Builder refreshInterval(long refreshInterval) {
                status.refreshInterval = refreshInterval;
                return this;
            }

            public RefreshStatus build() {
                return status;
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isLoading() {
            return loading;
        }

        public long getLastSuccessTime() {
            return lastSuccessTime;
        }

        public long getConsecutiveFailures() {
            return consecutiveFailures;
        }

        public long getRefreshInterval() {
            return refreshInterval;
        }
    }
}
