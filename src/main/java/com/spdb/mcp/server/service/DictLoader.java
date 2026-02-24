package com.spdb.mcp.server.service;

import java.io.InputStream;

/**
 * 字典加载器接口
 */
public interface DictLoader {
    
    /**
     * 加载字典文件
     * 
     * @return XML 文件输入流
     */
    InputStream loadDictFile();
    
    /**
     * 获取加载器类型
     * 
     * @return 加载器类型描述
     */
    String getLoaderType();
    
    /**
     * 测试连接或路径是否有效
     * 
     * @return 是否连接成功
     */
    boolean testConnection();
}
