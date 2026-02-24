package com.spdb.mcp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Maven 本地仓库字典加载器
 */
@Service
@ConditionalOnProperty(name = "dict.loader.type", havingValue = "maven", matchIfMissing = false)
public class MavenDictLoader implements DictLoader {

    private static final Logger log = LoggerFactory.getLogger(MavenDictLoader.class);

    @Value("${dict.group-id}")
    private String groupId;

    @Value("${dict.artifact-id}")
    private String artifactId;

    @Value("${dict.version}")
    private String version;

    @Value("${dict.xml-path}")
    private String xmlPath;

    private String mavenRepo;

    public void setMavenRepo(String mavenRepo) {
        this.mavenRepo = mavenRepo;
    }

    @Override
    public InputStream loadDictFile() {
        try {
            if (mavenRepo == null || mavenRepo.isEmpty()) {
                log.error("Maven 仓库路径未设置");
                throw new IllegalStateException("Maven 仓库路径未设置");
            }

            log.info("从 Maven 仓库加载字典文件...");
            log.info("Maven 仓库: {}", mavenRepo);
            log.info("坐标: {}:{}:{}", groupId, artifactId, version);

            // 查找最新的快照 JAR 包
            File jarFile = findLatestSnapshotJar();

            if (jarFile == null || !jarFile.exists()) {
                log.error("未找到字典 JAR 包: {}", getJarPath());
                throw new RuntimeException("未找到字典 JAR 包");
            }

            log.info("找到字典 JAR 包: {}", jarFile.getAbsolutePath());

            // 从 JAR 包中读取 XML 文件
            return readXmlFromJar(jarFile);

        } catch (Exception e) {
            log.error("从 Maven 仓库加载字典文件失败", e);
            throw new RuntimeException("从 Maven 仓库加载字典文件失败", e);
        }
    }

    @Override
    public String getLoaderType() {
        return "Maven Local Repository";
    }

    @Override
    public boolean testConnection() {
        try {
            if (mavenRepo == null || mavenRepo.isEmpty()) {
                log.error("Maven 仓库路径未设置");
                return false;
            }

            String dirPath = getJarDirectory();
            Path directory = Paths.get(dirPath);

            if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                log.error("Maven 仓库目录不存在: {}", directory);
                return false;
            }

            log.info("Maven 仓库路径验证成功");
            return true;

        } catch (Exception e) {
            log.error("Maven 仓库路径验证失败", e);
            return false;
        }
    }

    /**
     * 查找最新的快照 JAR 包
     */
    private File findLatestSnapshotJar() {
        String dirPath = getJarDirectory();
        Path directory = Paths.get(dirPath);

        log.debug("扫描目录: {}", directory);

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            log.error("目录不存在: {}", directory);
            return null;
        }

        try {
            // 查找所有 .jar 文件
            List<File> jarFiles = Files.list(directory)
                    .map(Path::toFile)
                    .filter(f -> f.isFile() && f.getName().endsWith(".jar"))
                    .filter(f -> f.getName().startsWith(artifactId + "-" + version.replace("-SNAPSHOT", "")))
                    .sorted(Comparator.comparingLong(File::lastModified).reversed())
                    .collect(Collectors.toList());

            if (jarFiles.isEmpty()) {
                log.error("未找到任何 JAR 文件");
                return null;
            }

            File latestJar = jarFiles.get(0);
            log.info("找到最新的快照版本: {} (修改时间: {})",
                    latestJar.getName(),
                    new Date(latestJar.lastModified()));

            return latestJar;

        } catch (Exception e) {
            log.error("扫描 JAR 文件失败", e);
            return null;
        }
    }

    /**
     * 从 JAR 包中读取 XML 文件
     */
    private InputStream readXmlFromJar(File jarFile) {
        try {
            JarFile jar = new JarFile(jarFile);
            ZipEntry entry = jar.getEntry(xmlPath);

            if (entry == null) {
                log.error("JAR 包中未找到文件: {}", xmlPath);
                jar.close();
                return null;
            }

            log.info("读取 JAR 包内文件: {}", xmlPath);

            InputStream inputStream = jar.getInputStream(entry);
            return new InputStream() {
                @Override
                public int read() throws java.io.IOException {
                    return inputStream.read();
                }

                @Override
                public void close() throws java.io.IOException {
                    inputStream.close();
                    jar.close();
                }

                @Override
                public int read(byte[] b) throws java.io.IOException {
                    return inputStream.read(b);
                }

                @Override
                public int read(byte[] b, int off, int len) throws java.io.IOException {
                    return inputStream.read(b, off, len);
                }
            };

        } catch (Exception e) {
            log.error("读取 JAR 包失败", e);
            return null;
        }
    }

    /**
     * 获取 JAR 包所在目录
     */
    private String getJarDirectory() {
        String groupPath = groupId.replace('.', File.separatorChar);
        return Paths.get(mavenRepo, groupPath, artifactId, version).toString();
    }

    /**
     * 获取 JAR 包路径
     */
    private String getJarPath() {
        return Paths.get(getJarDirectory(), artifactId + "-" + version + ".jar").toString();
    }

    public String getCurrentMavenRepo() {
        return mavenRepo;
    }
}
