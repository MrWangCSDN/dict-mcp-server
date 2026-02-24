package com.spdb.mcp.server.service;

import com.spdb.mcp.server.model.FieldDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字典 Schema 解析器测试
 */
class DictSchemaParserTest {

    private final DictSchemaParser parser = new DictSchemaParser();

    @Test
    void testParseSchema() throws Exception {
        // 加载测试 XML 文件
        InputStream inputStream = new ClassPathResource("test-schema.xml").getInputStream();

        // 解析
        Map<String, FieldDefinition> result = parser.parseSchema(inputStream);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // 验证具体字段
        FieldDefinition custIdField = result.get("客户ID");
        assertNotNull(custIdField, "应该找到'客户ID'字段");
        assertEquals("custId", custIdField.getId());
        assertEquals("客户ID", custIdField.getLongname());
        assertEquals("MBaseType.U_KE_HU_BIAN_HAO", custIdField.getType());
        assertEquals("cust_id", custIdField.getDbname());
        assertEquals("MDict.A.custId", custIdField.getRef());

        // 验证其他字段
        FieldDefinition cstField = result.get("国家");
        assertNotNull(cstField, "应该找到'国家'字段");
        assertEquals("cst", cstField.getId());
        assertEquals("MDict.C.cst", cstField.getRef());

        // 打印所有字段
        System.out.println("解析到的字段:");
        result.forEach((key, value) -> {
            System.out.printf("  %s: id=%s, type=%s, ref=%s%n",
                    key, value.getId(), value.getType(), value.getRef());
        });
    }
}
