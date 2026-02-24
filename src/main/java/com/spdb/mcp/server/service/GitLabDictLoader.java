package com.spdb.mcp.server.service;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.RepositoryFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * GitLab 仓库字典文件加载器
 */
@Service
@ConditionalOnProperty(name = "dict.loader.type", havingValue = "gitlab", matchIfMissing = false)
public class GitLabDictLoader implements DictLoader {

    private static final Logger log = LoggerFactory.getLogger(GitLabDictLoader.class);

    @Value("${dict.gitlab.url:http://gitlab.spdb.com}")
    private String gitlabUrl;

    @Value("${dict.gitlab.project-id:64142}")
    private Long projectId;

    @Value("${dict.gitlab.branch:master}")
    private String branch;

    @Value("${dict.gitlab.file-path:src/main/resources/dict/MDict.d_schema.xml}")
    private String filePath;

    @Value("${dict.gitlab.token:}")
    private String token;

    @Value("${dict.gitlab.username:}")
    private String username;

    @Value("${dict.gitlab.password:}")
    private String password;

    private GitLabApi gitLabApi;

    /**
     * 初始化 GitLab API 客户端
     */
    private void initGitLabApi() {
        if (gitLabApi != null) {
            return;
        }

        try {
            if (token != null && !token.isEmpty()) {
                // 优先使用 Token
                gitLabApi = new GitLabApi(gitlabUrl, token);
                log.info("使用 Token 连接 GitLab: {}", gitlabUrl);
            } else if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                // 使用用户名密码
                gitLabApi = GitLabApi.oauth2Login(gitlabUrl, username, password);
                log.info("使用用户名密码连接 GitLab: {}", gitlabUrl);
            } else {
                throw new IllegalStateException("GitLab 认证信息未配置（需要 token 或 username+password）");
            }

            // 测试连接
            gitLabApi.getProjectApi().getProject(projectId);
            log.info("GitLab 连接成功, 项目 ID: {}", projectId);

        } catch (Exception e) {
            log.error("初始化 GitLab API 失败", e);
            throw new RuntimeException("初始化 GitLab API 失败", e);
        }
    }

    @Override
    public InputStream loadDictFile() {
        try {
            initGitLabApi();

            log.info("从 GitLab 加载字典文件...");
            log.info("项目 ID: {}, 分支: {}, 文件路径: {}", projectId, branch, filePath);

            // 获取文件内容
            RepositoryFile file = gitLabApi.getRepositoryFileApi()
                    .getFile(projectId, filePath, branch);

            if (file == null) {
                log.error("文件不存在: {}", filePath);
                throw new RuntimeException("GitLab 文件不存在: " + filePath);
            }

            // 文件内容是 Base64 编码的
            String content = file.getContent();
            byte[] decodedBytes = Base64.getDecoder().decode(content);
            String xmlContent = new String(decodedBytes, StandardCharsets.UTF_8);

            log.info("成功加载字典文件, 大小: {} bytes", decodedBytes.length);
            log.debug("文件内容前 200 字符: {}", 
                    xmlContent.length() > 200 ? xmlContent.substring(0, 200) : xmlContent);

            return new ByteArrayInputStream(decodedBytes);

        } catch (Exception e) {
            log.error("从 GitLab 加载字典文件失败", e);
            throw new RuntimeException("从 GitLab 加载字典文件失败", e);
        }
    }

    @Override
    public String getLoaderType() {
        return "GitLab Repository";
    }

    @Override
    public boolean testConnection() {
        try {
            initGitLabApi();
            gitLabApi.getProjectApi().getProject(projectId);
            log.info("GitLab 连接测试成功");
            return true;
        } catch (Exception e) {
            log.error("GitLab 连接测试失败", e);
            return false;
        }
    }

    /**
     * 获取文件信息（用于调试）
     */
    public String getFileInfo() {
        try {
            initGitLabApi();

            RepositoryFile file = gitLabApi.getRepositoryFileApi()
                    .getFile(projectId, filePath, branch);

            if (file == null) {
                return "文件不存在";
            }

            return String.format("文件名: %s, 大小: %d bytes, 最后提交: %s",
                    file.getFileName(),
                    file.getSize(),
                    file.getLastCommitId());

        } catch (Exception e) {
            log.error("获取文件信息失败", e);
            return "获取文件信息失败: " + e.getMessage();
        }
    }
}
