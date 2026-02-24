package com.spdb.mcp.server.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 JGit 的 GitLab 字典文件加载器
 *
 * 首次调用时 shallow clone 仓库到本地临时目录，
 * 后续每次调用执行 git fetch 拉取最新内容，
 * 通过 JGit ObjectReader 直接从 Git 对象库读取文件内容，
 * 不产生工作区文件，不影响并发查询缓存。
 */
@Service
@ConditionalOnProperty(name = "dict.loader.type", havingValue = "gitlab", matchIfMissing = false)
public class GitLabDictLoader implements DictLoader {

    private static final Logger log = LoggerFactory.getLogger(GitLabDictLoader.class);

    /** Git 克隆地址，格式: http://host/group/repo.git */
    @Value("${dict.gitlab.git-url}")
    private String gitUrl;

    @Value("${dict.gitlab.branch:master}")
    private String branch;

    /** 仓库内的文件路径，用 / 分隔 */
    @Value("${dict.gitlab.file-path:src/main/resources/dict/MDict.d_schema.xml}")
    private String filePath;

    /** GitLab Personal Access Token（优先使用） */
    @Value("${dict.gitlab.token:}")
    private String token;

    @Value("${dict.gitlab.username:}")
    private String username;

    @Value("${dict.gitlab.password:}")
    private String password;

    private Git git;
    private File localRepoDir;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    // -------------------------------------------------------------------------
    // 认证
    // -------------------------------------------------------------------------

    /**
     * 构建 JGit 凭证提供器。
     * GitLab PAT 认证：用户名使用配置的 username（非 "oauth2"），密码为 token。
     * "oauth2" 仅适用于 OAuth2 令牌；PAT 必须使用实际 GitLab 用户名，
     * 否则部分 GitLab 实例会返回 404，导致 NoRemoteRepositoryException。
     */
    private UsernamePasswordCredentialsProvider buildCredentials() {
        if (token != null && !token.isEmpty()) {
            String user = (username != null && !username.isEmpty()) ? username : "oauth2";
            log.debug("使用 Token 认证, user={}", user);
            return new UsernamePasswordCredentialsProvider(user, token);
        }
        log.debug("使用用户名密码认证, user={}", username);
        return new UsernamePasswordCredentialsProvider(username, password);
    }

    // -------------------------------------------------------------------------
    // 仓库初始化 & 拉取
    // -------------------------------------------------------------------------

    /**
     * 首次：shallow clone（depth=1），减少数据传输量。
     */
    private void cloneRepo() throws GitAPIException, IOException {
        localRepoDir = Files.createTempDirectory("dict-mcp-repo-").toFile();
        log.info("开始克隆仓库: {} -> {}", gitUrl, localRepoDir.getAbsolutePath());

        git = Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(localRepoDir)
                .setDepth(1)
                .setBranch("refs/heads/" + branch)
                .setCloneAllBranches(false)
                .setCredentialsProvider(buildCredentials())
                .call();

        initialized.set(true);
        log.info("仓库克隆完成");
    }

    /**
     * 后续：fetch 最新提交（depth=1），只拉取指定分支，节省带宽。
     */
    private void fetchLatest() throws GitAPIException {
        log.debug("拉取最新提交: branch={}", branch);

        git.fetch()
                .setRemote("origin")
                .setRefSpecs(new RefSpec(
                        "+refs/heads/" + branch + ":refs/remotes/origin/" + branch))
                .setCredentialsProvider(buildCredentials())
                .setDepth(1)
                .call();

        log.debug("fetch 完成");
    }

    // -------------------------------------------------------------------------
    // 文件读取
    // -------------------------------------------------------------------------

    /**
     * 通过 JGit TreeWalk 从 Git 对象库读取指定文件内容。
     * 不依赖工作区文件，并发安全。
     */
    private byte[] readFileFromRepo() throws IOException {
        Repository repository = git.getRepository();

        // 解析远程追踪分支 -> 最新 commit
        ObjectId commitId = repository.resolve("refs/remotes/origin/" + branch);
        if (commitId == null) {
            // 回退：尝试本地分支（首次 clone 后分支引用位置）
            commitId = repository.resolve(branch);
        }
        if (commitId == null) {
            throw new RuntimeException("无法解析分支引用: " + branch);
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree = commit.getTree();

            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));

                if (!treeWalk.next()) {
                    throw new RuntimeException("仓库中不存在文件: " + filePath);
                }

                ObjectId blobId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(blobId);
                byte[] content = loader.getBytes();

                log.info("读取文件成功: {}, 大小: {} bytes, commit: {}",
                        filePath, content.length, commit.abbreviate(8).name());
                return content;
            }
        }
    }

    // -------------------------------------------------------------------------
    // DictLoader 接口实现
    // -------------------------------------------------------------------------

    @Override
    public InputStream loadDictFile() {
        try {
            if (!initialized.get()) {
                cloneRepo();
            } else {
                fetchLatest();
            }

            byte[] content = readFileFromRepo();
            return new ByteArrayInputStream(content);

        } catch (Exception e) {
            log.error("JGit 加载字典文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("JGit 加载字典文件失败", e);
        }
    }

    @Override
    public String getLoaderType() {
        return "GitLab / JGit";
    }

    @Override
    public boolean testConnection() {
        try {
            // 使用 ls-remote 测试连通性，不产生本地文件
            Git.lsRemoteRepository()
                    .setRemote(gitUrl)
                    .setCredentialsProvider(buildCredentials())
                    .call();
            log.info("JGit 连接测试成功: {}", gitUrl);
            return true;
        } catch (Exception e) {
            log.error("JGit 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // 资源清理
    // -------------------------------------------------------------------------

    @PreDestroy
    public void cleanup() {
        if (git != null) {
            git.close();
            log.info("JGit 实例已关闭");
        }
        if (localRepoDir != null && localRepoDir.exists()) {
            deleteDirectory(localRepoDir);
            log.info("本地临时仓库已删除: {}", localRepoDir.getAbsolutePath());
        }
    }

    private void deleteDirectory(File dir) {
        try {
            Files.walk(dir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("删除临时目录失败: {}", e.getMessage());
        }
    }
}
