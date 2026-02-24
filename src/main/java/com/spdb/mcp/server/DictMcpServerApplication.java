package com.spdb.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Dict MCP Server 应用主类
 * 
 * Spring AI MCP Server WebFlux 会自动配置 MCP 端点
 * 需要确保：
 * 1. 有 Function<?, ?> 类型的 Bean（使用 @Bean + @Description）
 * 2. 配置了 spring.ai.mcp.server 相关属性
 */
@SpringBootApplication
@EnableScheduling
public class DictMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DictMcpServerApplication.class, args);
    }
}
