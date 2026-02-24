# 复合对象引用检索流程

当字段引用其他复合类型时，必须通过检索流程确定正确的引用关系，不得猜测或假设。

## 适用场景

**何时执行此流程**：

- 字段中文名包含"对象""类型""信息""列表"等关键词
- 用户明确说明该字段引用其他复合类型
- 字段描述包含 JavaBean、POJO、复合对象等术语
- 用户给出的 type 已包含 `.` 分隔符但需要验证

## 检索流程

### 步骤 1：搜索所有 c_schema.xml 文件

**使用 Glob 工具搜索**：

```
模式：**/*.c_schema.xml
目标目录：项目根目录
```

**预期结果**：

```
找到 18 个 c_schema.xml 文件：
- loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml
- loan-resources/src/main/resources/type/ft/repay/FtAcctgType.c_schema.xml
- loan-beans/src/main/resources/type/LoanAppType.c_schema.xml
- dept-resources/src/main/resources/type/CustAcctType.c_schema.xml
- dept-resources/src/main/resources/type/account/AcctDetailType.c_schema.xml
- sett-resources/src/main/resources/type/PaymentType.c_schema.xml
- ...
```

### 步骤 2：批量读取并解析文件

**优化策略**：

- 使用 Grep 工具快速提取关键信息（避免逐个读取）
- 提取 `schema@id`、`complexType@id`、`complexType@longname`

**使用 Grep 提取 schema@id**：

```
模式：<schema[^>]+id="([^"]+)"
类型：xml
输出：匹配内容
```

**使用 Grep 提取 complexType 信息**：

```
模式：<complexType[^>]+id="([^"]+)"[^>]+longname="([^"]+)"
类型：xml
输出：匹配内容
```

### 步骤 3：构建索引

**数据结构**：

```javascript
const complexTypeIndex = [
  {
    file: "loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml",
    schemaId: "SyndAgrmLoanType",
    complexTypes: [
      { id: "SysdAgrmLoanInfoPojo", longname: "银团贷款出资份额信息" },
      { id: "SysdAgrmLoanDetailPojo", longname: "银团贷款明细信息" }
    ]
  },
  {
    file: "loan-resources/src/main/resources/type/ft/repay/FtAcctgType.c_schema.xml",
    schemaId: "FtAcctgType",
    complexTypes: [
      { id: "FtAcctRepayChkInPojo", longname: "福费延还款校验输入" },
      { id: "FtAcctRepayChkOutPojo", longname: "福费延还款校验输出" }
    ]
  },
  // ...
];
```

### 步骤 4：匹配算法

**优先级**：

1. **精确匹配 longname**（最高优先级）
2. 模糊匹配 id（忽略大小写，部分匹配）
3. 关键词匹配（提取中文关键词）

**匹配代码**：

```javascript
function findComplexTypeReference(userDescription, complexTypeIndex) {
  // 1. 精确匹配 longname
  for (const schema of complexTypeIndex) {
    for (const complexType of schema.complexTypes) {
      if (complexType.longname === userDescription) {
        return {
          schemaId: schema.schemaId,
          complexTypeId: complexType.id,
          type: `${schema.schemaId}.${complexType.id}`,
          matchType: "exact",
          file: schema.file
        };
      }
    }
  }
  
  // 2. 模糊匹配 id
  const normalizedDescription = userDescription.toLowerCase().replace(/\s+/g, '');
  for (const schema of complexTypeIndex) {
    for (const complexType of schema.complexTypes) {
      const normalizedId = complexType.id.toLowerCase();
      if (normalizedId.includes(normalizedDescription) || 
          normalizedDescription.includes(normalizedId)) {
        return {
          schemaId: schema.schemaId,
          complexTypeId: complexType.id,
          type: `${schema.schemaId}.${complexType.id}`,
          matchType: "fuzzy",
          file: schema.file
        };
      }
    }
  }
  
  // 3. 未找到匹配
  return null;
}
```

### 步骤 5：处理匹配结果

#### 找到精确匹配

```
✅ 找到复合对象引用（精确匹配）

字段描述: 银团贷款出资份额信息
匹配结果:
   - 文件: loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml
   - schema@id: SyndAgrmLoanType
   - complexType@id: SysdAgrmLoanInfoPojo
   - complexType@longname: 银团贷款出资份额信息
   
生成的 type: SyndAgrmLoanType.SysdAgrmLoanInfoPojo

确认使用此引用？
```

#### 找到模糊匹配

```
⚠️  找到可能的复合对象引用（模糊匹配）

字段描述: 银团贷款信息
匹配结果:
   - 文件: loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml
   - schema@id: SyndAgrmLoanType
   - complexType@id: SysdAgrmLoanInfoPojo
   - complexType@longname: 银团贷款出资份额信息
   
生成的 type: SyndAgrmLoanType.SysdAgrmLoanInfoPojo

匹配度: 模糊匹配
是否使用此引用？（Y/N，或输入其他选项）
```

#### 未找到匹配

```
❌ 未找到匹配的复合类型

字段描述: 临时对象信息
已搜索: 18 个 c_schema.xml 文件
匹配结果: 无

可能的原因：
1. 该复合类型尚未创建
2. 字段描述与 complexType@longname 不一致
3. 该字段实际为字典字段，非复合对象引用

请选择处理方式：
A. 先创建该复合类型，再创建当前复合类型（推荐）
B. 手动指定引用格式（如 SomeType.SomePojo）
C. 作为字典字段处理（查询 MCP）
D. 跳过该字段，暂不处理
```

## 完整示例

### 示例 1：包含复合对象引用的复合类型创建

**用户输入**：

```
创建复合类型: 贷款申请信息（LoanAppType）
子目录: application
包含字段：
- 申请编号（必输，字典字段）
- 申请日期（非必输，字典字段）
- 银团贷款出资份额信息（非必输，复合对象引用）
- 客户账户信息（非必输，复合对象引用）
- 收费金额（必输，字典字段）
```

**执行流程**：

```
✅ 工程识别完成
   - 工程类型: 业务主工程（loan-parent）
   - 模块: loan-resources
   - 基础 package: com.spdb.ccbs.loan.resources.type
   - 子目录: application
   - 最终 package: com.spdb.ccbs.loan.resources.type.application

🔍 识别复合对象引用字段（2 个）
   - 银团贷款出资份额信息
   - 客户账户信息

🔎 搜索 c_schema.xml 文件...
   找到 18 个文件

📋 匹配结果：

字段: 银团贷款出资份额信息
✅ 精确匹配
   - 文件: loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml
   - type: SyndAgrmLoanType.SysdAgrmLoanInfoPojo

字段: 客户账户信息
✅ 精确匹配
   - 文件: dept-resources/src/main/resources/type/CustAcctType.c_schema.xml
   - type: CustAcctType.CustAcctInfo

🔍 查询字典字段（3 个）
   - 申请编号
   - 申请日期
   - 收费金额

✅ 字段查询完成
   - 总字段数: 5
   - 复合对象引用: 2（无需查 MCP）
   - 字典字段已贯标: 3
   - 字典字段未贯标: 0

📝 生成 XML...

✅ 文件创建完成
   - 文件路径: loan-resources/src/main/resources/type/application/LoanAppType.c_schema.xml
```

**生成的 XML**：

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<schema xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="LoanAppType" package="com.spdb.ccbs.loan.resources.type.application" longname="贷款申请信息" classgen="normal" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <complexType abstract="false" dict="false" id="LoanAppInfo" introduct="false" localName="" longname="贷款申请信息" extension="" tags="">
        <!-- 字典字段 -->
        <element id="appNo" longname="申请编号" type="MBaseType.U_SHEN_QING_BIAN_HAO" required="true" multi="false" range="false" array="false" final="false" override="false" allowSubType="true" key="false" ref="MDict.A.appNo"/>
        <element id="appDate" longname="申请日期" type="MBaseType.U_RI_QI" required="false" multi="false" range="false" array="false" final="false" override="false" allowSubType="true" key="false" ref="MDict.A.appDate"/>
        
        <!-- 复合对象引用：无 ref 属性 -->
        <element id="syndLoanInfo" longname="银团贷款出资份额信息" type="SyndAgrmLoanType.SysdAgrmLoanInfoPojo" required="false" multi="false" range="false" array="false" final="false" override="false" allowSubType="true" key="false"/>
        <element id="custAcctInfo" longname="客户账户信息" type="CustAcctType.CustAcctInfo" required="false" multi="false" range="false" array="false" final="false" override="false" allowSubType="true" key="false"/>
        
        <!-- 字典字段 -->
        <element id="chrgAmt" longname="收费金额" type="MBaseType.U_JIN_E" required="true" multi="false" range="false" array="false" final="false" override="false" allowSubType="true" key="false" ref="MDict.C.chrgAmt"/>
    </complexType>
</schema>
```

### 示例 2：复合对象引用未找到匹配

**用户输入**：

```
创建复合类型: 测试类型（TestType）
包含字段：
- 申请编号（字典字段）
- 未知对象信息（复合对象引用？）
- 收费金额（字典字段）
```

**执行流程**：

```
🔍 识别复合对象引用字段（1 个）
   - 未知对象信息

🔎 搜索 c_schema.xml 文件...
   找到 18 个文件

❌ 未找到匹配的复合类型

字段描述: 未知对象信息
已搜索: 18 个 c_schema.xml 文件
匹配结果: 无

可能的原因：
1. 该复合类型尚未创建
2. 字段描述与 complexType@longname 不一致
3. 该字段实际为字典字段，非复合对象引用

请选择处理方式：
A. 先创建该复合类型，再创建当前复合类型（推荐）
B. 手动指定引用格式（如 SomeType.SomePojo）
C. 作为字典字段处理（查询 MCP）
D. 跳过该字段，暂不处理
```

**用户选择 C**：

```
✅ 将 "未知对象信息" 作为字典字段处理

🔍 查询字典字段（3 个）
   - 申请编号
   - 未知对象信息
   - 收费金额

查询结果：
   - 申请编号: ✅ 已贯标
   - 未知对象信息: ❌ 未贯标（返回 null）
   - 收费金额: ✅ 已贯标

❌ 以下字段未贯标，未写入 XML：
   - 未知对象信息

💡 建议：请在字典系统中完成字段贯标，或先创建对应的复合类型
```

## 匹配算法详解

### 精确匹配（最高优先级）

```javascript
function exactMatch(userDescription, complexTypes) {
  for (const ct of complexTypes) {
    if (ct.longname === userDescription) {
      return ct;
    }
  }
  return null;
}
```

### 模糊匹配（次优先级）

```javascript
function fuzzyMatch(userDescription, complexTypes) {
  const normalized = userDescription
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/信息|对象|类型|列表/g, '');
  
  for (const ct of complexTypes) {
    const normalizedId = ct.id.toLowerCase();
    const normalizedLongname = ct.longname
      .toLowerCase()
      .replace(/\s+/g, '')
      .replace(/信息|对象|类型|列表/g, '');
    
    if (normalizedId.includes(normalized) || 
        normalized.includes(normalizedId) ||
        normalizedLongname.includes(normalized)) {
      return ct;
    }
  }
  return null;
}
```

### 多结果处理

若匹配到多个复合类型，展示列表让用户选择：

```
⚠️  找到多个可能的匹配

字段描述: 贷款信息

匹配结果：
1. SyndAgrmLoanType.SysdAgrmLoanInfoPojo
   - 文件: loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml
   - longname: 银团贷款出资份额信息

2. LoanType.LoanBasicInfo
   - 文件: loan-resources/src/main/resources/type/LoanType.c_schema.xml
   - longname: 贷款基本信息

3. LoanAppType.LoanAppInfo
   - 文件: loan-resources/src/main/resources/type/LoanAppType.c_schema.xml
   - longname: 贷款申请信息

请选择使用哪个引用（输入序号 1-3，或输入 N 跳过）：
```

## 性能优化建议

### 缓存搜索结果

同一次创建或修改中，多个字段可能引用复合对象，建议缓存搜索结果：

```javascript
// 仅搜索一次
const allCSchemaFiles = glob("**/*.c_schema.xml");
const complexTypeIndex = parseAllFiles(allCSchemaFiles);

// 多次匹配
const ref1 = findMatch("银团贷款信息", complexTypeIndex);
const ref2 = findMatch("客户账户信息", complexTypeIndex);
const ref3 = findMatch("收费项目列表", complexTypeIndex);
```

### 并行解析

对大量文件，使用并行读取和解析：

```javascript
const parsePromises = allCSchemaFiles.map(file => 
  readAndParseFile(file)
);

const allResults = await Promise.all(parsePromises);
```

## 错误处理

### 文件读取失败

```
⚠️  文件读取失败

文件: loan-resources/src/main/resources/type/BrokenType.c_schema.xml
错误: XML 格式错误

跳过此文件，继续搜索其他文件...
```

### XML 解析失败

```
⚠️  XML 解析失败

文件: loan-resources/src/main/resources/type/InvalidType.c_schema.xml
错误: 缺少 schema@id 属性

跳过此文件，继续搜索其他文件...
```

## 关键要求

- ✅ **必须搜索项目中所有 c_schema.xml 文件**
- ✅ **优先按 longname 精确匹配**
- ✅ **找到匹配后向用户确认**
- ✅ **未找到匹配时提供明确的处理选项**
- ❌ **不得猜测或假设引用格式**
- ❌ **不得跳过检索流程直接生成**
- ❌ **复合对象引用字段不查询字典 MCP**
- ❌ **复合对象引用字段不写 ref 属性**
