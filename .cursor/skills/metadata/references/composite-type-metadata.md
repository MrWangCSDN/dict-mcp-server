# 复合类型元数据（`*.c_schema.xml`）

本参考用于 `metadata` skill 在处理复合类型/复合对象时的规则约束。适用触发词：`复合类型`、`复合对象`、`c_schema.xml`。

## 快速规则

- 文件名必须为：`{SchemaId}.c_schema.xml`
- **`schema@xmlns:xsi` 必须是第一个属性**，固定值 `http://www.w3.org/2001/XMLSchema-instance`
- `schema@id` 必须与文件名前缀一致且全局唯一
- **`schema@package` 必须填写**，根据工程类型和子目录自动生成，不能为空
- `schema@classgen` 固定值 `normal`
- `schema@xsi:noNamespaceSchemaLocation` 固定值 `ltts-model.xsd`
- `complexType` 表示一个复合对象，`complexType@id` 在同文件内唯一
- **`element` 标签仅允许 11 个属性**，禁止添加 `dbname`、`length` 等 MCP 返回的其他字段
- 字段固定值：`required=false`、`multi=false`、`range=false`、`array=false`、`final=false`、`override=false`、`allowSubType=true`、`key=false`

## ID 生成规则

### schema@id（文件标识）

**规则**：

1. **用户提供 id**：直接使用用户给定的 id
2. **用户未提供 id**：根据中文名自动翻译成英文，使用大驼峰（PascalCase）

**翻译规则**：

- 提取中文名称的关键词
- 转为英文缩写或完整词
- 首字母大写，词间无分隔符
- 通常以 `Type` 结尾

**示例**：

| 中文名称 | 生成的 schema@id |
|---------|-----------------|
| 福费延还款复合类型 | `FtAcctgType` |
| 银团贷款信息 | `SyndAgrmLoanType` |
| 客户账户类型 | `CustAcctType` |

**唯一性**：schema@id 在整个项目中必须全局唯一。

### complexType@id（对象标识）

**规则**：

1. **用户提供 id**：直接使用用户给定的 id
2. **用户未提供 id**：根据中文名自动翻译成英文，使用大驼峰（PascalCase）

**翻译规则**：

- 提取中文名称的关键词
- 转为英文缩写或完整词
- 首字母大写，词间无分隔符
- 通常以 `Pojo` / `Info` / `Request` / `Response` 等后缀结尾

**示例**：

| 中文名称 | 生成的 complexType@id |
|---------|---------------------|
| 福费延还款校验输入 | `FtAcctRepayChkInPojo` |
| 福费延还款校验输出 | `FtAcctRepayChkOutPojo` |
| 银团贷款出资份额信息 | `SysdAgrmLoanInfoPojo` |
| 客户基本信息 | `CustBaseInfo` |

**唯一性（重要）**：

- complexType@id 必须在**同一文件内唯一**
- 若翻译后的 id 与文件内已有 id 重复，需变更英文名称确保唯一
- 可通过调整缩写、增加后缀、使用同义词等方式避免冲突

**去重示例**：

假设文件内已存在 `CustInfo`，若新增对象的中文名也翻译为 `CustInfo`：

| 重复场景 | 原 id | 新对象中文名 | 变更后 id |
|---------|-------|------------|-----------|
| 同名冲突 | `CustInfo` | 客户信息（详细） | `CustDetailInfo` |
| 同名冲突 | `CustInfo` | 客户信息（查询） | `CustQueryInfo` |
| 同名冲突 | `CustInfo` | 客户信息V2 | `CustInfoV2` |

**生成流程**：

1. 根据中文名翻译 → 生成候选 id
2. 检查文件内是否已存在相同 id
3. 若存在冲突 → 调整英文名（添加限定词、后缀、版本号等）
4. 再次检查直到唯一 → 使用最终 id

## 包路径与模块映射

**由项目工程类型自动识别**（见 [project-detection.md](project-detection.md)）

### 业务主工程（xxx-parent）

- 存款：`com.spdb.ccbs.dept.resources.type` -> `dept-resources` 模块，`src/main/resources/type`
- 贷款：`com.spdb.ccbs.loan.resources.type` -> `loan-resources` 模块，`src/main/resources/type`
- 结算：`com.spdb.ccbs.sett.resources.type` -> `sett-resources` 模块，`src/main/resources/type`
- 公共：`com.spdb.ccbs.comm.resources.type` -> `comm-resources` 模块，`src/main/resources/type`

### API 工程（xxx-api-parent）

- 存款：`com.spdb.dept.beans.type` -> `dept-beans` 模块，`src/main/resources/type`
- 贷款：`com.spdb.loan.beans.type` -> `loan-beans` 模块，`src/main/resources/type`
- 结算：`com.spdb.sett.beans.type` -> `sett-beans` 模块，`src/main/resources/type`
- 公共：`com.spdb.comm.beans.type` -> `comm-beans` 模块，`src/main/resources/type`

**注意**：API 工程的 package 为 `com.spdb.{领域}`，**没有 `ccbs` 段**。

## 字段来源与未贯标处理（硬性规则）

### 字段来源

- 常规字段的 `id/longname/type/ref` 来自字典 MCP 查询
- 调用 `getDictDefByLongNameList`，传入字段中文名列表

### 未贯标字段判定

查询返回 `null` 或 `undefined` 的字段视为未贯标。

### 未贯标字段处理（必须严格遵守）

**硬性规则**：

1. ❌ **未贯标字段绝对不写入 XML**
   - 不生成对应的 `<element>` 标签
   - 不使用占位符、默认值、注释等任何形式写入
   
2. ✅ **仅写入已贯标字段**
   - 只有返回值不为 `null` 的字段才生成 `<element>` 标签
   
3. 📋 **收集并提示用户**
   - 记录所有未贯标字段的中文名
   - 统一汇总提示：`❌ 以下字段未贯标，未写入 XML：{字段列表}`
   - 建议用户完成贯标后重新创建

### 处理示例

**场景**：创建包含 5 个字段的复合类型

```javascript
// MCP 查询结果
const mcpResult = {
  "福费延借据编码": { 
    id: "fRFTGDueBillCd", 
    type: "MBaseType.U_DAI_KUAN_JIE_JU_BIAN_MA",
    longname: "福费延借据编码",
    ref: "MDict.F.fRFTGDueBillCd"
  },
  "融资业务编码": {
    id: "fncgBsnID",
    type: "MBaseType.U_RONG_ZI_YE_WU_BIAN_MA",
    longname: "融资业务编码",
    ref: "MDict.F.fncgBsnID"
  },
  "临时字段A": null,        // ❌ 未贯标
  "测试字段B": null,        // ❌ 未贯标
  "收费金额": {
    id: "chrgAmt",
    type: "MBaseType.U_JIN_E",
    longname: "收费金额",
    ref: "MDict.C.chrgAmt"
  }
};

// 过滤处理
const validFields = [];
const unstandardized = [];

for (const [longname, fieldDef] of Object.entries(mcpResult)) {
  if (fieldDef === null || fieldDef === undefined) {
    unstandardized.push(longname);
  } else {
    validFields.push(fieldDef);
  }
}

// 生成 XML（仅使用 validFields）
```

**生成的 XML**：

```xml
<complexType abstract="false" dict="false" id="TestPojo" ...>
    <element id="fRFTGDueBillCd" longname="福费延借据编码" type="MBaseType.U_DAI_KUAN_JIE_JU_BIAN_MA" ... ref="MDict.F.fRFTGDueBillCd"/>
    <element id="fncgBsnID" longname="融资业务编码" type="MBaseType.U_RONG_ZI_YE_WU_BIAN_MA" ... ref="MDict.F.fncgBsnID"/>
    <element id="chrgAmt" longname="收费金额" type="MBaseType.U_JIN_E" ... ref="MDict.C.chrgAmt"/>
    <!-- 临时字段A 和 测试字段B 未贯标，未写入 -->
</complexType>
```

**用户提示**：

```
✅ 复合类型创建完成
   - 文件路径: loan-resources/src/main/resources/type/TestType.c_schema.xml
   - 总字段数: 5
   - 已贯标并写入: 3（福费延借据编码, 融资业务编码, 收费金额）
   - 未贯标未写入: 2

❌ 以下字段未贯标，未写入 XML：
   - 临时字段A
   - 测试字段B

💡 建议：请在字典系统中完成字段贯标后，重新创建或修改本复合类型
```

### 关键要求

- ⚠️ **此规则为硬性约束，任何情况下都不得违反**
- ⚠️ 未贯标字段不写入 XML 是为了确保元数据的完整性和一致性
- ⚠️ 不得使用任何变通方式（如手动填写、占位符等）规避此规则

## 复合对象引用规则与检索流程

**详细的检索流程、匹配算法、完整示例请参阅**：[composite-object-reference.md](composite-object-reference.md)

以下为快速参考。

### 基本规则

当字段引用其他复合类型时：

- `type` 使用 `{SchemaId}.{ComplexTypeId}`
- **不设置 `ref` 属性**
- **不调用字典 MCP 查询该字段**
- `multi="false"` 表示对象，`multi="true"` 表示对象数组（List）

### 检索流程（必须执行）

当遇到可能是复合对象引用的字段时，**必须执行以下检索流程**确定正确的引用关系。

#### 第 1 步：识别复合对象引用

**触发条件（满足任一）**：

- 字段中文名包含"对象""类型""信息""列表"等关键词
- 用户明确说明该字段引用其他复合类型
- 字段 type 已包含 `.` 分隔符
- 用户描述该字段为"复合对象""复合类型引用"

**示例**：

| 字段中文名 | 是否复合引用 | 原因 |
|-----------|------------|------|
| 银团贷款出资份额信息 | ✅ 是 | 包含"信息" |
| 客户账户对象 | ✅ 是 | 包含"对象" |
| 还款类型列表 | ✅ 是 | 包含"类型""列表" |
| 收费金额 | ❌ 否 | 常规字段 |
| 查询日期 | ❌ 否 | 常规字段 |

#### 第 2 步：搜索项目中的 c_schema.xml 文件

**搜索范围**：

```
{项目根目录}/**/*.c_schema.xml
```

**搜索工具**：使用 Glob 工具

```
模式：**/*.c_schema.xml
目标目录：项目根目录
```

**示例搜索结果**：

```
找到 15 个 c_schema.xml 文件：
- loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml
- loan-resources/src/main/resources/type/ft/repay/FtAcctgType.c_schema.xml
- dept-resources/src/main/resources/type/CustAcctType.c_schema.xml
- sett-resources/src/main/resources/type/PaymentType.c_schema.xml
- ...
```

#### 第 3 步：解析并匹配 complexType

**对每个找到的文件执行**：

1. **读取文件内容**
2. **提取信息**：
   - `schema@id`
   - 所有 `complexType@id`
   - 所有 `complexType@longname`
3. **与用户描述匹配**：
   - 优先按 `longname` 匹配
   - 其次按 `id` 匹配（模糊匹配，忽略大小写）

**匹配逻辑示例**：

```javascript
// 用户提供的字段描述
const userDescription = "银团贷款出资份额信息";

// 遍历所有 c_schema.xml 文件
for (const file of cSchemaFiles) {
  const content = readFile(file);
  const schemaId = extractSchemaId(content);
  const complexTypes = extractComplexTypes(content);
  
  for (const complexType of complexTypes) {
    // 按 longname 精确匹配
    if (complexType.longname === userDescription) {
      return {
        schemaId: schemaId,
        complexTypeId: complexType.id,
        type: `${schemaId}.${complexType.id}`
      };
    }
  }
}
```

**匹配示例**：

| 用户字段描述 | 找到的文件 | schema@id | 匹配的 complexType@longname | complexType@id | 生成的 type |
|------------|-----------|-----------|---------------------------|---------------|------------|
| 银团贷款出资份额信息 | SyndAgrmLoanType.c_schema.xml | `SyndAgrmLoanType` | 银团贷款出资份额信息 | `SysdAgrmLoanInfoPojo` | `SyndAgrmLoanType.SysdAgrmLoanInfoPojo` |
| 福费延还款校验输入 | FtAcctgType.c_schema.xml | `FtAcctgType` | 福费延还款校验输入 | `FtAcctRepayChkInPojo` | `FtAcctgType.FtAcctRepayChkInPojo` |
| 客户账户基本信息 | CustAcctType.c_schema.xml | `CustAcctType` | 客户账户基本信息 | `CustAcctBaseInfo` | `CustAcctType.CustAcctBaseInfo` |

#### 第 4 步：确定引用格式并生成 element

**找到匹配**：

```xml
<element 
  id="lstSyndAgrmLoanQryOutPojo" 
  longname="银团贷款出资份额信息" 
  type="SyndAgrmLoanType.SysdAgrmLoanInfoPojo" 
  required="false" 
  multi="false" 
  range="false" 
  array="false" 
  final="false" 
  override="false" 
  allowSubType="true" 
  key="false"/>
<!-- 注意：无 ref 属性 -->
```

**未找到匹配**：

提示用户并询问处理方式：

```
❌ 未找到匹配的复合类型

字段描述: {用户提供的描述}
已搜索: 15 个 c_schema.xml 文件
匹配结果: 无

可能的原因：
1. 该复合类型尚未创建
2. 字段描述与 complexType@longname 不一致
3. 该字段实际为字典字段，非复合对象引用

请选择处理方式：
A. 先创建该复合类型，再创建当前复合类型
B. 手动指定引用格式（如 SomeType.SomePojo）
C. 作为字典字段处理（查询 MCP）
```

#### 第 5 步：向用户确认

找到匹配后，展示并确认：

```
✅ 找到复合对象引用

字段信息
   - 字段中文名: 银团贷款出资份额信息
   - 字段英文 id: lstSyndAgrmLoanQryOutPojo（用户提供或生成）
   - 是否列表/数组: 否（multi=false）

匹配结果
   - 匹配到文件: loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml
   - schema@id: SyndAgrmLoanType
   - complexType@id: SysdAgrmLoanInfoPojo
   - complexType@longname: 银团贷款出资份额信息

生成的 element 标签
   - type: SyndAgrmLoanType.SysdAgrmLoanInfoPojo
   - 无 ref 属性
   - 不查询字典 MCP

确认使用此引用？
```

### 完整示例

**场景**：创建包含复合对象引用的复合类型

**输入**：

```
复合类型: 贷款申请信息（LoanAppType）
包含字段：
- 申请编号（字典字段）
- 申请日期（字典字段）
- 银团贷款出资份额信息（复合对象引用）
- 收费金额（字典字段）
```

**处理流程**：

1. 识别"银团贷款出资份额信息"为复合对象引用
2. 搜索项目中所有 `*.c_schema.xml` 文件
3. 在 `SyndAgrmLoanType.c_schema.xml` 中找到匹配：
   - complexType@longname = "银团贷款出资份额信息"
   - complexType@id = "SysdAgrmLoanInfoPojo"
4. 生成 type = `SyndAgrmLoanType.SysdAgrmLoanInfoPojo`
5. 其他字段查询字典 MCP

**生成的 XML**：

```xml
<schema xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="LoanAppType" ...>
    <complexType abstract="false" dict="false" id="LoanAppInfo" ...>
        <!-- 字典字段 -->
        <element id="appNo" longname="申请编号" type="MBaseType.U_..." required="false" ... ref="MDict.A.appNo"/>
        <element id="appDate" longname="申请日期" type="MBaseType.U_..." required="false" ... ref="MDict.A.appDate"/>
        
        <!-- 复合对象引用：无 ref 属性 -->
        <element id="syndLoanInfo" longname="银团贷款出资份额信息" type="SyndAgrmLoanType.SysdAgrmLoanInfoPojo" required="false" multi="false" range="false" array="false" final="false" override="false" allowSubType="true" key="false"/>
        
        <!-- 字典字段 -->
        <element id="chrgAmt" longname="收费金额" type="MBaseType.U_..." required="false" ... ref="MDict.C.chrgAmt"/>
    </complexType>
</schema>
```

### 对象数组引用

当字段为列表/数组时，设置 `multi="true"`：

```xml
<!-- 单个对象 -->
<element id="syndLoanInfo" longname="银团贷款出资份额信息" type="SyndAgrmLoanType.SysdAgrmLoanInfoPojo" required="false" multi="false" .../>

<!-- 对象数组/列表 -->
<element id="syndLoanInfoList" longname="银团贷款出资份额信息列表" type="SyndAgrmLoanType.SysdAgrmLoanInfoPojo" required="false" multi="true" .../>
```

### 关键要求

- ✅ 遇到复合对象引用时**必须执行检索流程**
- ✅ 优先按 `longname` 精确匹配
- ✅ 找到匹配后向用户确认
- ✅ 未找到匹配时明确告知用户并提供选项
- ❌ 不得猜测或假设复合对象引用格式
- ❌ 复合对象引用字段不查询字典 MCP
- ❌ 复合对象引用字段不写 `ref` 属性

## 创建流程细则

### 路径与 package 确定流程（最关键，package 不能为空）

**必须严格按顺序**：

1. **扫描 pom.xml** → 识别工程类型（业务主工程 / API 工程）和领域
2. **确定模块**：
   - 业务主工程 → `{领域}-resources`
   - API 工程 → `{领域}-beans`
3. **确定基础 package（schema@package 属性基础值）**：
   - 业务主工程：`com.spdb.ccbs.{领域}.resources.type`
   - API 工程：`com.spdb.{领域}.beans.type`（无 `ccbs`）
4. **询问子目录**：
   - 有子目录 → 追加到目录路径和 package（分隔符 `/` 变 `.`）
   - 无子目录 → 使用基础路径和基础 package
5. **构建完整路径与 package（最终值）**：
   - 文件路径：`{模块}/src/main/resources/type/{子目录}/{SchemaId}.c_schema.xml`
   - **schema@package 属性值**：`{基础package}.{子目录}`（无子目录时为 `{基础package}`）

**package 属性生成示例**：

| 工程类型 | 领域 | 子目录 | schema@package 最终值 |
|---------|------|-------|---------------------|
| 业务主工程 | loan | 无 | `com.spdb.ccbs.loan.resources.type` |
| 业务主工程 | loan | `ft/repay` | `com.spdb.ccbs.loan.resources.type.ft.repay` |
| API 工程 | loan | 无 | `com.spdb.loan.beans.type` |
| API 工程 | loan | `account` | `com.spdb.loan.beans.type.account` |

**关键检查**：

- ✅ package 属性值不能为空
- ✅ package 属性值不能只是基础段（如只有 `com.spdb.ccbs`）
- ✅ 有子目录时必须追加到 package
- ✅ 子目录分隔符 `/` 必须转为 `.`

### 完整创建步骤

1. **项目工程识别**：读 pom.xml，确定工程类型和领域
2. **确定模块与基础路径**：根据工程类型自动确定
3. **询问并确定子目录**：追加到路径和 package
4. **确定 schema id**：用户提供或翻译生成
5. **配置 Maven 仓库**：需要字典查询时配置
6. **定义 complexType 和字段**：
   - 确认 complexType id
   - 收集字段信息
   - **识别复合对象引用字段**
   - **执行复合对象检索流程**（搜索 `*.c_schema.xml`，匹配 complexType）
   - 分类：复合引用字段 vs 字典字段
7. **批量查字典**：仅查询字典字段，排除复合引用字段
8. **过滤未贯标**：返回 `null` 的字段不写入
9. **生成 XML**：合并字典字段和复合引用字段
10. **保存文件并回报**：输出路径、统计、未贯标字段

### 字段分类与处理对比

| 字段类型 | 识别方式 | 处理流程 | type 来源 | ref 属性 | 查询 MCP |
|---------|---------|---------|----------|---------|---------|
| 字典字段 | 常规字段描述 | 查 MCP | MCP 返回 | MCP 返回 | ✅ 是 |
| 复合对象引用 | 包含"对象""信息"等关键词 | 检索 c_schema.xml | 检索匹配结果 | 不写 | ❌ 否 |
| 未贯标字段 | MCP 返回 null | 不写入 XML | - | - | - |

## element 标签属性规则（严格遵守，禁止添加额外属性）

### 允许的属性列表（仅限以下 11 个）

| 属性 | 来源 | 值示例 | 说明 |
|------|------|--------|------|
| `id` | 字典 MCP | `fRFTGDueBillCd` | 字段英文名 |
| `longname` | 字典 MCP | `福费延借据编码` | 字段中文名 |
| `type` | 字典 MCP | `MBaseType.U_DAI_KUAN_JIE_JU_BIAN_MA` | 字段类型 |
| `ref` | 字典 MCP | `MDict.F.fRFTGDueBillCd` | 字典引用（复合对象引用时不写） |
| `required` | 用户输入 | `false` / `true` | 是否必输 |
| `multi` | 固定值 | `false` / `true` | 是否多值（对象数组用 `true`） |
| `range` | 固定值 | `false` | 固定为 `false` |
| `array` | 固定值 | `false` | 固定为 `false` |
| `final` | 固定值 | `false` | 固定为 `false` |
| `override` | 固定值 | `false` | 固定为 `false` |
| `allowSubType` | 固定值 | `true` | 固定为 `true` |
| `key` | 固定值 | `false` | 固定为 `false` |

### 禁止的属性（不得添加）

**禁止添加以下从 MCP 返回的属性**：

- ❌ `dbname`（数据库字段名）
- ❌ `length`（字段长度）
- ❌ `precision`（精度）
- ❌ `scale`（小数位）
- ❌ 任何其他非上述 11 个允许属性

**关键规则**：

1. **仅使用允许的 11 个属性**
2. 即使 MCP 查询返回了 `dbname` 等其他字段，**也不得写入 XML**
3. 属性顺序建议：`id` → `longname` → `type` → `required` → `multi` → `range` → `array` → `final` → `override` → `allowSubType` → `key` → `ref`

### 字段属性来源映射

**从 MCP 查询结果中提取**（仅这 4 个）：

```javascript
// MCP 返回的 FieldDefinition
{
  "id": "fRFTGDueBillCd",           // ✅ 使用
  "longname": "福费延借据编码",       // ✅ 使用
  "type": "MBaseType.U_...",        // ✅ 使用
  "ref": "MDict.F.fRFTGDueBillCd",  // ✅ 使用（复合引用时不写）
  "dbname": "F_RFTG_DUE_BILL_CD",   // ❌ 忽略，不写入
  "length": 32,                     // ❌ 忽略，不写入
  "precision": 0,                   // ❌ 忽略，不写入
  // ... 其他字段全部忽略
}
```

**固定值属性**（不从 MCP 获取）：

- `multi=false`（对象数组除外）
- `range=false`
- `array=false`
- `final=false`
- `override=false`
- `allowSubType=true`
- `key=false`

**用户输入属性**：

- `required`：根据用户明确标注（必输/非必输），默认 `false`

## 结构模板

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<schema xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="FtAcctgType" package="com.spdb.ccbs.loan.resources.type.ft.repay" longname="福费延还款复合类型" classgen="normal" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <complexType abstract="false" dict="false" id="FtAcctRepayChkInPojo" introduct="false" localName="" longname="福费延还款校验输入" extension="" tags="">
        <element id="fRFTGDueBillCd" longname="福费延借据编码" type="MBaseType.U_DAI_KUAN_JIE_JU_BIAN_MA" required="false" multi="false" range="false" array="false" final="false" override="false" allowSubType="true" key="false" ref="MDict.F.fRFTGDueBillCd"/>
    </complexType>
</schema>
```

**schema 标签属性说明**：

| 属性 | 必填 | 值来源 | 示例 |
|------|-----|--------|------|
| **`xmlns:xsi`** | ✅ | **固定值** | `http://www.w3.org/2001/XMLSchema-instance` |
| `id` | ✅ | 用户提供或翻译生成（全局唯一） | `FtAcctgType` |
| **`package`** | ✅ | **根据工程类型和子目录自动生成** | `com.spdb.ccbs.loan.resources.type.ft.repay` |
| `longname` | ✅ | 用户提供的中文名 | `福费延还款复合类型` |
| **`classgen`** | ✅ | **固定值 `normal`** | `normal` |
| `xsi:noNamespaceSchemaLocation` | ✅ | 固定值 | `ltts-model.xsd` |

**schema 标签属性顺序（必须）**：

```xml
<schema xmlns:xsi="固定值" id="..." package="..." longname="..." classgen="normal" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
```

**关键要求**：

- **xmlns:xsi 必须是第一个属性**，固定值 `http://www.w3.org/2001/XMLSchema-instance`
- **package 属性必须填写**，使用项目工程识别后确定的完整 package
- **classgen 固定为 `normal`**
- package 格式：
  - 业务主工程：`com.spdb.ccbs.{领域}.resources.type.{子目录}`
  - API 工程：`com.spdb.{领域}.beans.type.{子目录}`
