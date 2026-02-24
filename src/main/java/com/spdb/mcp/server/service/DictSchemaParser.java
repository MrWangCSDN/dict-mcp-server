package com.spdb.mcp.server.service;

import com.spdb.mcp.server.model.FieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 字典 Schema XML 解析器
 */
@Service
public class DictSchemaParser {

    private static final Logger log = LoggerFactory.getLogger(DictSchemaParser.class);

    /**
     * 解析字典 XML 文件
     *
     * @param inputStream XML 文件输入流
     * @return Map<longname, FieldDefinition>
     */
    public Map<String, FieldDefinition> parseSchema(InputStream inputStream) {
        Map<String, FieldDefinition> fieldMap = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            // 获取 schema 标签的 id 属性 (如 MDict)
            Element schemaElement = (Element) document.getElementsByTagName("schema").item(0);
            String schemaId = schemaElement.getAttribute("id");

            log.debug("解析 schema: {}", schemaId);

            // 遍历所有 complexType 标签
            NodeList complexTypeList = schemaElement.getElementsByTagName("complexType");

            for (int i = 0; i < complexTypeList.getLength(); i++) {
                Element complexTypeElement = (Element) complexTypeList.item(i);
                String complexTypeId = complexTypeElement.getAttribute("id");

                log.debug("解析 complexType: {}", complexTypeId);

                // 遍历该 complexType 下的所有 element 标签
                NodeList elementList = complexTypeElement.getElementsByTagName("element");

                for (int j = 0; j < elementList.getLength(); j++) {
                    Element elementTag = (Element) elementList.item(j);

                    String elementId = elementTag.getAttribute("id");
                    String dbname = elementTag.getAttribute("dbname");
                    String longname = elementTag.getAttribute("longname");
                    String type = elementTag.getAttribute("type");

                    // 生成 ref: MDict.A.afMdCntDsc
                    String ref = String.format("%s.%s.%s", schemaId, complexTypeId, elementId);

                    FieldDefinition field = FieldDefinition.builder()
                            .id(elementId)
                            .longname(longname)
                            .type(type)
                            .dbname(dbname)
                            .ref(ref)
                            .build();

                    // 使用 longname 作为 key
                    fieldMap.put(longname, field);

                    log.trace("解析字段: {} -> {}", longname, field);
                }
            }

            log.info("成功解析字典文件, 共 {} 个字段", fieldMap.size());

        } catch (Exception e) {
            log.error("解析字典 XML 文件失败", e);
            throw new RuntimeException("解析字典 XML 文件失败", e);
        }

        return fieldMap;
    }
}
