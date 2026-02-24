# 数组字段处理详解

本文档详细说明如何处理 flowtran 中的数组字段 (fields 标签)。

## 数组字段概述

### 使用场景

当需要表示一组相关的字段集合时,使用 `<fields>` 标签而不是单个 `<field>` 标签。

**常见场景**:
- 收费代码数组 (chargCdArray)
- 结果列表 (resultArray)
- 明细数组 (detailArray)
- 账户列表 (accountArray)

## 自然语言格式

### 基本格式

使用 `start` 和 `end` 标记数组的开始和结束:

```
{数组名}Array {中文名}数组 start
    包含 {字段1} {中文名1} {必输/非必输}
    包含 {字段2} {中文名2} {必输/非必输}
{数组名}Array {中文名}数组 end
```

**示例 1: 收费代码数组**
```
chargCdArray 收费代码数组 start
    包含 fPrjCd   收费项目编码  非必输
    包含 chrgAmt  收费金额     必输
chargCdArray 收费代码数组 end
```

**示例 2: 结果数组**
```
resultArray 结果数组 start
    包含 code  代码
    包含 msg   消息
resultArray 结果数组 end
```

## XML 生成规则

### fields 标签属性 (必须单行)

| 属性 | 值 | 说明 |
|-----|---|------|
| id | {名称}Array | 必须以 Array 结尾 |
| scope | "" | 固定为空字符串 |
| required | false | 固定为 false |
| multi | true | 固定为 true |
| array | false | 固定为 false |
| longname | {中文名}数组 | 必须以"数组"结尾 |

### 内嵌 field 标签属性

fields 内的 field 标签缩进比 fields 多 4 个空格。

## 完整示例

### 示例 1: 基本数组

**自然语言**:
```
chargCdArray 收费代码数组 start
    包含 fPrjCd   收费项目编码  非必输
    包含 chrgAmt  收费金额     必输
chargCdArray 收费代码数组 end
```

**生成的 XML** (注意层级缩进):
```xml
<input packMode="true">
    <field id="custId" type="MBaseType.U_KE_HU_BIAN_HAO" required="true" multi="false" array="false" longname="客户ID" ref="MDict.C.custId"/>
    <fields id="chargCdArray" scope="" required="false" multi="true" array="false" longname="收费代码数组">
        <field id="fPrjCd" type="MBaseType.U_SHOU_FEI_XIANG_MU_BIAN_MA" required="false" multi="false" array="false" longname="收费项目编码" ref="MDict.F.fPrjCd"/>
        <field id="chrgAmt" type="MBaseType.U_JIN_E" required="true" multi="false" array="false" longname="收费金额" ref="MDict.C.chrgAmt"/>
    </fields>
</input>
```

**缩进说明**:
- `<input>`: 8 个空格
- `<field>` (input 内): 12 个空格
- `<fields>`: 12 个空格
- `<field>` (fields 内): 16 个空格

### 示例 2: 多个数组

**自然语言**:
```
输入:
custId  客户ID  必输
accountArray 账户数组 start
    包含 accountNo   账号  必输
accountArray 账户数组 end
transArray 交易数组 start
    包含 transId   交易ID  必输
    包含 transAmt  交易金额  必输
transArray 交易数组 end
queryDate  查询日期  非必输
```

**生成的 XML**:
```xml
<input packMode="true">
    <field id="custId" type="..." required="true" multi="false" array="false" longname="客户ID" ref="..."/>
    <fields id="accountArray" scope="" required="false" multi="true" array="false" longname="账户数组">
        <field id="accountNo" type="..." required="true" multi="false" array="false" longname="账号" ref="..."/>
    </fields>
    <fields id="transArray" scope="" required="false" multi="true" array="false" longname="交易数组">
        <field id="transId" type="..." required="true" multi="false" array="false" longname="交易ID" ref="..."/>
        <field id="transAmt" type="..." required="true" multi="false" array="false" longname="交易金额" ref="..."/>
    </fields>
    <field id="queryDate" type="..." required="false" multi="false" array="false" longname="查询日期" ref="..."/>
</input>
```

## Python 生成代码

```python
def generate_fields_xml(fields_data, indent_level=3):
    """生成 fields 标签 XML
    
    Args:
        fields_data: 数组字段数据
        indent_level: 缩进层级 (默认 3 表示 12 个空格)
    """
    base_indent = "    " * indent_level
    child_indent = "    " * (indent_level + 1)
    
    # fields 开始标签 (属性单行)
    xml = f'{base_indent}<fields '
    xml += f'id="{fields_data["id"]}" '
    xml += 'scope="" '
    xml += 'required="false" '
    xml += 'multi="true" '
    xml += 'array="false" '
    xml += f'longname="{fields_data["longname"]}">\n'
    
    # 内嵌 field 标签
    for field in fields_data['fields']:
        xml += f'{child_indent}<field '
        xml += f'id="{field["id"]}" '
        xml += f'type="{field["type"]}" '
        xml += f'required="{str(field.get("required", False)).lower()}" '
        xml += 'multi="false" '
        xml += 'array="false" '
        xml += f'longname="{field["longname"]}"'
        
        if field.get("ref"):
            xml += f' ref="{field["ref"]}"'
        
        xml += '/>\n'
    
    # fields 结束标签
    xml += f'{base_indent}</fields>'
    return xml
```

## 完整交易生成示例

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="TY292" longname="客户信息查询" kind="auto" package="com.spdb.ccbs.comm.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[客户信息查询]]></description>
    <interface package="com.spdb.ccbs.comm.pbf.trans.intf">
        <input packMode="true">
            <field id="custId" type="MBaseType.U_KE_HU_BIAN_HAO" required="true" multi="false" array="false" longname="客户ID" ref="MDict.C.custId"/>
            <fields id="chargCdArray" scope="" required="false" multi="true" array="false" longname="收费代码数组">
                <field id="fPrjCd" type="MBaseType.U_SHOU_FEI_XIANG_MU_BIAN_MA" required="false" multi="false" array="false" longname="收费项目编码" ref="MDict.F.fPrjCd"/>
                <field id="chrgAmt" type="MBaseType.U_JIN_E" required="true" multi="false" array="false" longname="收费金额" ref="MDict.C.chrgAmt"/>
            </fields>
        </input>
        <output asParm="true" packMode="true">
            <field id="custName" type="MBaseType.U_KE_HU_MING_CHENG" required="false" multi="false" array="false" longname="客户名称" ref="MDict.C.custName"/>
            <fields id="resultArray" scope="" required="false" multi="true" array="false" longname="结果数组">
                <field id="code" type="MBaseType.U_DAI_MA" required="false" multi="false" array="false" longname="代码" ref="MDict.C.code"/>
                <field id="msg" type="MBaseType.U_XIAO_XI" required="false" multi="false" array="false" longname="消息" ref="MDict.M.msg"/>
            </fields>
        </output>
    </interface>
</flowtran>
```

## 格式验证清单

生成 XML 时验证:
- [ ] 所有标签属性在一行内
- [ ] 标签之间无空行
- [ ] flowtran 子标签缩进 4 个空格
- [ ] interface 子标签缩进 8 个空格
- [ ] input/output 子标签缩进 12 个空格
- [ ] fields 内 field 缩进 16 个空格
- [ ] 使用空格不是 Tab
- [ ] 同层级标签缩进对齐
