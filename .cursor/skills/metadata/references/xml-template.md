# Flowtran XML 模板详解

本文档详细说明 flowtran XML 的完整模板结构、标签属性和生成规则。

## 完整 XML 模板

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="TY291" longname="收费明细文件批量查询" kind="auto" package="com.spdb.ccbs.comm.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[收费明细文件批量查询]]></description>
    <interface package="com.spdb.ccbs.comm.pbf.trans.intf">
        <input packMode="true">
            <field id="fieldName" type="MBaseType.U_TYPE" required="false" multi="false" array="false" longname="字段中文名" ref="MDict.X.fieldName"/>
        </input>
        <output asParm="true" packMode="true">
            <field id="fieldName" type="MBaseType.U_TYPE" required="false" multi="false" array="false" longname="字段中文名" ref="MDict.X.fieldName"/>
        </output>
    </interface>
</flowtran>
```

## XML 格式规范

### 缩进规则

- **使用 4 个空格** 缩进
- **不使用 Tab** 字符
- **同层级标签缩进相同**
- **子层级基于父标签缩进 + 4 个空格**

**层级示例**:
```xml
<flowtran ...>              ← 第 0 层(根)
    <description>...</description>    ← 第 1 层(+4空格)
    <interface ...>                    ← 第 1 层
        <input ...>                    ← 第 2 层(+8空格)
            <field .../>               ← 第 3 层(+12空格)
            <fields ...>               ← 第 3 层
                <field .../>           ← 第 4 层(+16空格)
            </fields>
        </input>
    </interface>
</flowtran>
```

### 属性格式规则

- **所有属性必须在一行内**,不能换行
- **属性之间用空格分隔**
- **属性顺序保持一致**

**正确**:
```xml
<field id="custId" type="MBaseType.U_KE_HU_BIAN_HAO" required="true" multi="false" array="false" longname="客户ID" ref="MDict.C.custId"/>
```

**错误** (属性换行):
```xml
<field id="custId" 
       type="MBaseType.U_KE_HU_BIAN_HAO" 
       required="true"/>
```

### 标签间距规则

- **同级标签之间不要有空行**
- **紧密排列**
- **保持代码紧凑**

**正确**:
```xml
<input packMode="true">
    <field id="custId" .../>
    <field id="queryDate" .../>
    <field id="pageNo" .../>
</input>
```

**错误** (有空行):
```xml
<input packMode="true">
    <field id="custId" .../>
    
    <field id="queryDate" .../>
</input>
```

## flowtran 根标签

### 必需属性

| 属性 | 说明 | 来源 | 示例 | 固定值 |
|-----|------|------|------|--------|
| id | 交易编码 | 用户输入 | TY291 | - |
| longname | 交易中文名称 | 用户输入 | 收费明细文件批量查询 | - |
| kind | 交易类型 | - | auto | ✅ auto |
| package | Java包路径 | 根据交易码自动 | com.spdb.ccbs.comm.pbf.trans | - |
| txnMode | 事务模式 | 用户指定或默认 | A | A (默认) |
| xsi:noNamespaceSchemaLocation | Schema位置 | - | ltts-model.xsd | ✅ ltts-model.xsd |

### 完整示例

```xml
<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="TY291" longname="收费明细文件批量查询" kind="auto" package="com.spdb.ccbs.comm.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    ...
</flowtran>
```

## description 标签

**格式**:
```xml
<description><![CDATA[交易描述内容]]></description>
```

**规则**:
- 使用 CDATA 包裹
- 内容与 longname 保持一致
- 可以包含特殊字符
- 缩进 4 个空格(第 1 层)

## interface 标签

**属性**: `package` = flowtran package + ".intf"

**示例**:
```xml
<interface package="com.spdb.ccbs.comm.pbf.trans.intf">
    <input packMode="true">
        ...
    </input>
    <output asParm="true" packMode="true">
        ...
    </output>
</interface>
```

**缩进**: 
- interface 标签: 4 个空格(第 1 层)
- input/output: 8 个空格(第 2 层)
- field/fields: 12 个空格(第 3 层)

## input 标签

**属性**: `packMode="true"` (固定)

**完整示例**:
```xml
<input packMode="true">
    <field id="custId" type="MBaseType.U_KE_HU_BIAN_HAO" required="true" multi="false" array="false" longname="客户ID" ref="MDict.C.custId"/>
    <fields id="chargCdArray" scope="" required="false" multi="true" array="false" longname="收费代码数组">
        <field id="fPrjCd" type="MBaseType.U_SHOU_FEI_XIANG_MU_BIAN_MA" required="false" multi="false" array="false" longname="收费项目编码" ref="MDict.F.fPrjCd"/>
    </fields>
</input>
```

**缩进说明**:
- field 标签: 12 个空格(相对 flowtran 根标签)
- fields 内的 field: 16 个空格

## output 标签

**属性**: `asParm="true"`, `packMode="true"` (都固定)

**完整示例**:
```xml
<output asParm="true" packMode="true">
    <field id="custName" type="MBaseType.U_KE_HU_MING_CHENG" required="false" multi="false" array="false" longname="客户名称" ref="MDict.C.custName"/>
    <fields id="resultArray" scope="" required="false" multi="true" array="false" longname="结果数组">
        <field id="code" type="MBaseType.U_DAI_MA" required="false" multi="false" array="false" longname="代码" ref="MDict.C.code"/>
    </fields>
</output>
```

## field 标签 (普通字段)

**属性顺序** (必须单行):
```
id → type → required → multi → array → longname → ref
```

**生成代码**:
```python
def generate_field_xml(field_data, indent_level=3):
    """生成 field 标签 XML"""
    indent = "    " * indent_level
    
    xml = f'{indent}<field '
    xml += f'id="{field_data["id"]}" '
    xml += f'type="{field_data["type"]}" '
    xml += f'required="{str(field_data.get("required", False)).lower()}" '
    xml += 'multi="false" '
    xml += 'array="false" '
    xml += f'longname="{field_data["longname"]}"'
    
    if field_data.get("ref"):
        xml += f' ref="{field_data["ref"]}"'
    
    xml += '/>'
    return xml
```

## fields 标签 (数组字段)

**属性**: 必须单行

**生成代码**:
```python
def generate_fields_xml(fields_data, indent_level=3):
    """生成 fields 标签 XML"""
    base_indent = "    " * indent_level
    child_indent = "    " * (indent_level + 1)
    
    xml = f'{base_indent}<fields '
    xml += f'id="{fields_data["id"]}" '
    xml += 'scope="" '
    xml += 'required="false" '
    xml += 'multi="true" '
    xml += 'array="false" '
    xml += f'longname="{fields_data["longname"]}">\n'
    
    # 内嵌 field 标签
    for field in fields_data['fields']:
        xml += generate_field_xml(field, indent_level + 1) + '\n'
    
    xml += f'{base_indent}</fields>'
    return xml
```

## 完整生成示例

### Python 实现

```python
def generate_flowtran_xml(trans_data):
    """生成完整的 flowtran XML"""
    package_info = get_package_info(
        trans_data['id'], 
        trans_data.get('subdirectory', '')
    )
    
    lines = []
    lines.append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
    
    # flowtran 标签 (第 0 层)
    lines.append(f'<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="{trans_data["id"]}" longname="{trans_data["longname"]}" kind="auto" package="{package_info["package"]}" txnMode="{trans_data.get("txnMode", "A")}" xsi:noNamespaceSchemaLocation="ltts-model.xsd">')
    
    # description (第 1 层)
    lines.append(f'    <description><![CDATA[{trans_data["longname"]}]]></description>')
    
    # interface (第 1 层)
    lines.append(f'    <interface package="{package_info["interface_package"]}">')
    
    # input (第 2 层)
    lines.append('        <input packMode="true">')
    for field in trans_data["inputFields"]:
        if field.get('is_array'):
            lines.append(generate_fields_xml(field, 3))
        else:
            lines.append(generate_field_xml(field, 3))
    lines.append('        </input>')
    
    # output (第 2 层)
    lines.append('        <output asParm="true" packMode="true">')
    for field in trans_data["outputFields"]:
        if field.get('is_array'):
            lines.append(generate_fields_xml(field, 3))
        else:
            lines.append(generate_field_xml(field, 3))
    lines.append('        </output>')
    
    # 关闭标签
    lines.append('    </interface>')
    lines.append('</flowtran>')
    
    return '\n'.join(lines)
```

## 修改时的处理

### 保留原标签,仅更新 input/output

**原文件**:
```xml
<flowtran id="TY291" longname="原名称" kind="auto" package="..." txnMode="A" ...>
    <description><![CDATA[原描述]]></description>
    <interface package="...">
        <input packMode="true">
            <!-- 原字段 -->
        </input>
        <output asParm="true" packMode="true">
            <!-- 原字段 -->
        </output>
    </interface>
</flowtran>
```

**修改**: 只替换 input 和 output 标签的完整内容,包括缩进

**修改后**:
```xml
<flowtran id="TY291" longname="原名称" kind="auto" package="..." txnMode="A" ...>
    <description><![CDATA[原描述]]></description>
    <interface package="...">
        <input packMode="true">
            <field id="newField" .../>
        </input>
        <output asParm="true" packMode="true">
            <field id="newField" .../>
        </output>
    </interface>
</flowtran>
```

## 缩进级别对照表

| 标签 | 相对 flowtran 的层级 | 空格数 | 示例 |
|-----|---------------------|--------|------|
| flowtran | 0 | 0 | `<flowtran ...>` |
| description | 1 | 4 | `    <description>` |
| interface | 1 | 4 | `    <interface>` |
| input/output | 2 | 8 | `        <input>` |
| field (input/output 内) | 3 | 12 | `            <field .../>` |
| fields (数组) | 3 | 12 | `            <fields>` |
| field (fields 内) | 4 | 16 | `                <field .../>` |

## 常见问题

### Q: 为什么属性必须在一行?
**A**: 保持格式一致性和可读性,便于文本处理和比较。

### Q: 为什么标签间不要空行?
**A**: 保持 XML 紧凑,减少文件大小,符合项目规范。

### Q: 缩进用 Tab 可以吗?
**A**: 不可以,必须使用 4 个空格。

### Q: 修改时如何保持缩进一致?
**A**: 读取原文件,提取 input/output 的缩进级别,生成新内容时使用相同缩进。

### Q: fields 标签内的 field 缩进是多少?
**A**: fields 基础上再加 4 个空格,通常是 16 个空格(第 4 层)。
