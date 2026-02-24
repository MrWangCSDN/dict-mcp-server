package com.spdb.mcp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Maven Settings 读取器 - 用于读取 Maven settings.xml 中的本地仓库路径
 */
@Component
public class MavenSettingsReader {

    private static final Logger log = LoggerFactory.getLogger(MavenSettingsReader.class);

    /**
     * 获取 Maven 本地仓库路径
     * 优先级：
     * 1. 环境变量 M2_HOME 或 MAVEN_HOME
     * 2. 系统属性 maven.home
     * 3. 用户目录下的 .m2/settings.xml
     * 4. 默认路径 ${user.home}/.m2/repository
     *
     * @return Maven 本地仓库路径，如果找不到则返回 null
     */
    public String getLocalRepositoryPath() {
        // 1. 先尝试从环境变量获取
        String m2Home = System.getenv("M2_HOME");
        if (m2Home == null || m2Home.isEmpty()) {
            m2Home = System.getenv("MAVEN_HOME");
        }
        if (m2Home == null || m2Home.isEmpty()) {
            m2Home = System.getProperty("maven.home");
        }

        // 2. 查找 settings.xml 文件
        File settingsFile = findSettingsXml(m2Home);
        if (settingsFile == null || !settingsFile.exists()) {
            log.warn("未找到 Maven settings.xml 文件");
            return null;
        }

        log.info("找到 Maven settings.xml: {}", settingsFile.getAbsolutePath());

        // 3. 解析 settings.xml
        try {
            String localRepo = parseLocalRepository(settingsFile);
            if (localRepo != null && !localRepo.isEmpty()) {
                // 替换 ${user.home} 等变量
                localRepo = resolvePath(localRepo);
                log.info("从 settings.xml 读取到本地仓库路径: {}", localRepo);
                return localRepo;
            }
        } catch (Exception e) {
            log.error("解析 settings.xml 失败", e);
        }

        return null;
    }

    /**
     * 查找 settings.xml 文件
     */
    private File findSettingsXml(String mavenHome) {
        // 1. 用户目录下的 .m2/settings.xml (优先级最高)
        String userHome = System.getProperty("user.home");
        File userSettings = new File(userHome, ".m2/settings.xml");
        if (userSettings.exists()) {
            return userSettings;
        }

        // 2. Maven 安装目录下的 conf/settings.xml
        if (mavenHome != null && !mavenHome.isEmpty()) {
            File mavenSettings = new File(mavenHome, "conf/settings.xml");
            if (mavenSettings.exists()) {
                return mavenSettings;
            }
        }

        return null;
    }

    /**
     * 解析 settings.xml 获取本地仓库路径
     */
    private String parseLocalRepository(File settingsFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(settingsFile);

        // 查找 <localRepository> 标签
        NodeList localRepoNodes = doc.getElementsByTagName("localRepository");
        if (localRepoNodes.getLength() > 0) {
            Node node = localRepoNodes.item(0);
            String localRepo = node.getTextContent().trim();
            if (!localRepo.isEmpty()) {
                return localRepo;
            }
        }

        // 如果没有找到，返回默认路径
        return null;
    }

    /**
     * 解析路径中的变量，如 ${user.home}
     */
    private String resolvePath(String path) {
        String userHome = System.getProperty("user.home");
        if (path.contains("${user.home}")) {
            path = path.replace("${user.home}", userHome);
        }
        if (path.contains("${env.M2_HOME}")) {
            String m2Home = System.getenv("M2_HOME");
            if (m2Home != null) {
                path = path.replace("${env.M2_HOME}", m2Home);
            }
        }
        if (path.contains("${env.MAVEN_HOME}")) {
            String mavenHome = System.getenv("MAVEN_HOME");
            if (mavenHome != null) {
                path = path.replace("${env.MAVEN_HOME}", mavenHome);
            }
        }
        return path;
    }
}
