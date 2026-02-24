package com.spdb.mcp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动提示器 - 根据加载器类型显示配置提示
 */
@Component
public class InteractiveInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InteractiveInitializer.class);

    @Value("${dict.loader.type:maven}")
    private String loaderType;

    @Autowired(required = false)
    private DictLoader dictLoader;

    @Autowired
    private DictService dictService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========================================");
        System.out.println("Dict MCP Server 已启动");
        System.out.println("========================================");
        System.out.println();

        if ("gitlab".equalsIgnoreCase(loaderType)) {
            System.out.println("字典加载器: GitLab 仓库");
            System.out.println();
            
            if (dictLoader != null) {
                // 尝试初始化加载
                try {
                    System.out.println("正在从 GitLab 加载字典文件...");
                    dictService.init();
                    System.out.println("✓ 字典文件加载成功");
                    System.out.println("  - 已缓存字段数: " + dictService.getCacheStats().get("totalFields"));
                } catch (Exception e) {
                    System.out.println("✗ 字典文件加载失败: " + e.getMessage());
                    System.out.println();
                    System.out.println("请检查配置:");
                    System.out.println("  - dict.gitlab.url");
                    System.out.println("  - dict.gitlab.project-id");
                    System.out.println("  - dict.gitlab.token 或 username+password");
                }
            }
            
        } else {
            System.out.println("字典加载器: Maven 本地仓库");
            System.out.println();
            System.out.println("提示: 请通过 MCP 工具 configureMavenRepo 配置 Maven 仓库路径");
            System.out.println("使用步骤:");
            System.out.println("1. 调用 configureMavenRepo 工具，action='detect' 检测路径");
            System.out.println("2. 根据返回的提示，调用 action='use' 使用检测到的路径");
            System.out.println("3. 或调用 action='custom' 使用自定义路径");
            System.out.println();
            System.out.println("配置完成后，即可使用 getDictDefByLongNameList 查询字典");
        }

        System.out.println("========================================\n");
        
        log.info("服务已启动，加载器类型: {}", loaderType);
    }
}

