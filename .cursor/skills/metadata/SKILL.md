---
name: metadata
description: Creates, modifies, and deletes XML metadata for flowtran online transactions (.flowtrans.xml) and composite types (.c_schema.xml). Use when user mentions TC/TD/TG/TY transaction codes, flowtrans, composite types, composite objects, or c_schema.
---

# 元数据模型开发技能

统一入口，支持两类 XML 元数据的创建、修改、删除：

| 类型 | 文件格式 | 触发关键词 |
|------|----------|-----------|
| 联机交易 | `{交易码}.flowtrans.xml` | TC/TD/TG/TY + 4位数字、联机交易、flowtrans |
| 复合类型 | `{SchemaId}.c_schema.xml` | 复合类型、复合对象、c_schema |

---

## 第一步：场景路由（必须先判定）

在执行任何操作前，根据用户输入判定进入哪个模式：

- **联机交易模式** → 出现交易码或 `.flowtrans.xml`
- **复合类型模式** → 出现"复合类型/复合对象"或 `.c_schema.xml`
- **混合模式** → 同时包含两类信号 → 拆分为两个独立子任务

**信息不足时必须先澄清**，不得直接生成 XML：

- 目标文件扩展名？（`.flowtrans.xml` / `.c_schema.xml`）
- 目标标识？（交易码 / schema id）

---

## 通用规则（两类共用，内联）

### 项目工程自动识别（创建时必须先执行）

在创建任何元数据前，**必须先扫描项目根路径 `pom.xml`** 自动识别工程类型和领域：

1. 读取 `pom.xml` 的 `<artifactId>`
2. 识别工程类型与领域：
   - **业务主工程**：`loan-parent` / `dept-parent` / `comm-parent` / `sett-parent`
   - **API 工程**：`loan-api-parent` / `dept-api-parent` / `comm-api-parent` / `sett-api-parent`
3. 自动确定：模块名、目录路径、package 属性
4. 向用户展示识别结果并确认
5. 询问是否需要子目录（有子目录时会追加到路径和 package）

**识别规则速查**：

| 工程类型 | 元数据类型 | 模块 | 基础目录 | 基础 package |
|---------|----------|------|---------|-------------|
| 业务主工程 | 联机交易 | `{领域}-pbf` | `src/main/resources/trans` | `com.spdb.ccbs.{领域}.pbf.trans` |
| 业务主工程 | 复合类型 | `{领域}-resources` | `src/main/resources/type` | `com.spdb.ccbs.{领域}.resources.type` |
| API 工程 | 复合类型 | `{领域}-beans` | `src/main/resources/type` | `com.spdb.{领域}.beans.type` |
| API 工程 | 联机交易 | ❌ 不支持 | - | - |

**关键规则**：

- **API 工程的 package 为 `com.spdb.{领域}`（无 `ccbs`）**
- **子目录处理（重要）**：
  - 若用户指定子目录（如 `act`、`ft/repay`），必须同时追加到**目录路径**和**package**
  - 子目录分隔符：路径用 `/`，package 用 `.`
  - **文件路径示例**：`loan-pbf/src/main/resources/trans/act/TD1001.flowtrans.xml`
  - **flowtran package 示例**：`com.spdb.ccbs.loan.pbf.trans.act`
  - **interface package 示例**：`com.spdb.ccbs.loan.pbf.trans.act.intf`（在 flowtran package 基础上加 `.intf`）
  - **schema package 示例**：`com.spdb.ccbs.loan.resources.type.ft.repay`

详见 [references/project-detection.md](references/project-detection.md)

### Maven 仓库配置

涉及字典字段查询时必须先配置：

1. 调用 `configureMavenRepo(action="get")` 获取建议路径
2. 与用户确认
3. 调用 `configureMavenRepo(action="save", mavenRepoPath=...)` 保存

### 字段查询

- 收集所有需要字典解析的字段中文名，去重后批量调用 `getDictDefByLongNameList`
- 禁止逐个字段多次调用

### 未贯标字段处理（硬性规则，必须严格遵守）

**判定规则**：

调用 `getDictDefByLongNameList` 后，返回值为 `null` 或 `undefined` 的字段视为未贯标。

**处理规则（不可违反）**：

1. **未贯标字段绝对不写入 XML**
   - ❌ 不得以任何形式（占位符、注释、空标签等）写入
   - ✅ 完全跳过该字段，不生成对应的 `<field>` 或 `<element>` 标签
   
2. **收集未贯标字段名称**
   - 记录所有返回 `null` 的字段中文名
   - 在流程结束时统一汇总
   
3. **提示用户（必须）**
   - 格式：`❌ 以下字段未贯标，未写入 XML：字段1, 字段2, 字段3`
   - 建议：`💡 请在字典系统中完成字段贯标后重新创建`
   
4. **仅保留已贯标字段**
   - 只有返回值不为 `null` 的字段才写入 XML
   - 确保生成的 XML 中所有字段都有完整的元数据

**示例处理流程**：

```javascript
// 批量查询结果
const mcpResult = {
  "客户ID": { id: "custId", type: "...", longname: "客户ID", ref: "..." },
  "查询日期": { id: "queryDate", type: "...", longname: "查询日期", ref: "..." },
  "未知字段": null,      // ❌ 未贯标
  "测试字段": null,      // ❌ 未贯标
  "收费金额": { id: "chrgAmt", type: "...", longname: "收费金额", ref: "..." }
};

// 过滤处理
const standardizedFields = [];   // 已贯标字段
const unstandardizedFields = [];  // 未贯标字段

for (const [longname, fieldDef] of Object.entries(mcpResult)) {
  if (fieldDef === null || fieldDef === undefined) {
    unstandardizedFields.push(longname);  // ❌ 收集，不写入
  } else {
    standardizedFields.push(fieldDef);    // ✅ 写入 XML
  }
}

// 生成 XML（仅使用 standardizedFields）
// 提示用户：❌ 以下字段未贯标，未写入 XML：未知字段, 测试字段
```

**关键要求**：

- ⚠️ **此规则为硬性约束，任何情况下都不得违反**
- ⚠️ 不得因为字段重要就强行写入未贯标字段
- ⚠️ 不得使用默认值、占位符等方式规避此规则

### 字段 required 属性

- 明确"必输/必须/required" → `required="true"`
- 明确"非必输/可选/optional"或**未标注** → `required="false"`

### XML 格式约定

- 所有属性必须在同一行
- 标签之间无空行
- 缩进使用 4 空格，子层级累加
- XML 头固定：`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`
- `xsi:noNamespaceSchemaLocation` 固定 `ltts-model.xsd`

### 输出要求（必须完整回报）

每次执行完成**必须**向用户回报以下信息：

**基本信息**：

- 模式判定（联机交易 / 复合类型 / 混合）
- 操作类型（创建 / 修改 / 删除）
- 目标文件路径

**字段统计（重要）**：

- 总字段数
- 已贯标字段数（已写入 XML）
- 未贯标字段数（未写入 XML）

**未贯标字段清单（如有，必须明确告知）**：

```
❌ 以下字段未贯标，未写入 XML：
   - 字段1
   - 字段2
   - 字段3

💡 建议：请在字典系统中完成字段贯标后重新操作
```

**完整输出示例**：

```
✅ 复合类型创建完成

📁 文件信息
   - 模式: 复合类型
   - 操作: 创建
   - 文件路径: loan-resources/src/main/resources/type/ft/repay/FtAcctgType.c_schema.xml
   - schema@package: com.spdb.ccbs.loan.resources.type.ft.repay

📊 字段统计
   - 总字段数: 8
   - 已贯标并写入: 6
   - 未贯标未写入: 2

❌ 以下字段未贯标，未写入 XML：
   - 临时字段A
   - 测试字段B

💡 建议：请在字典系统中完成字段贯标后，重新创建或修改本文件
```

**关键要求**：

- ⚠️ 必须如实告知未贯标字段，不得隐瞒
- ⚠️ 未贯标字段清单要逐个列出，便于用户识别
- ⚠️ 提供明确的后续操作建议

---

## 联机交易模式（仅联机交易时读取）

仅在判定为联机交易模式后使用以下规则。

**前提**：必须先执行"项目工程自动识别"确认当前为**业务主工程**。API 工程不支持创建联机交易。

### 交易码与模块映射

| 前缀 | 领域 | 基础 package | 模块 | 基础目录 |
|------|------|------------|------|---------|
| TC | 存款 | com.spdb.ccbs.dept.pbf.trans | dept-pbf | src/main/resources/trans |
| TD | 贷款 | com.spdb.ccbs.loan.pbf.trans | loan-pbf | src/main/resources/trans |
| TG | 结算 | com.spdb.ccbs.sett.pbf.trans | sett-pbf | src/main/resources/trans |
| TY | 公共 | com.spdb.ccbs.comm.pbf.trans | comm-pbf | src/main/resources/trans |

**基本规则**：

- 交易码格式：`T + (C/D/G/Y) + 4位数字`，全局唯一
- 文件名：`{交易码}.flowtrans.xml`

**子目录处理（必须严格遵守）**：

若用户指定子目录（如 `act`），必须同步应用到三处：

1. **文件路径**：`{模块}/src/main/resources/trans/{子目录}/{交易码}.flowtrans.xml`
   - 示例：`loan-pbf/src/main/resources/trans/act/TD1001.flowtrans.xml`
   
2. **flowtran package**：`com.spdb.ccbs.{领域}.pbf.trans.{子目录}`
   - 示例：`com.spdb.ccbs.loan.pbf.trans.act`
   
3. **interface package**：`com.spdb.ccbs.{领域}.pbf.trans.{子目录}.intf`
   - 示例：`com.spdb.ccbs.loan.pbf.trans.act.intf`
   - 规则：在 flowtran package 基础上追加 `.intf`

**多层子目录示例**（如 `ft/repay`）：

- 文件路径：`loan-pbf/src/main/resources/trans/ft/repay/TD1001.flowtrans.xml`
- flowtran package：`com.spdb.ccbs.loan.pbf.trans.ft.repay`
- interface package：`com.spdb.ccbs.loan.pbf.trans.ft.repay.intf`

### 创建流程（必须严格按顺序执行）

#### 第 1 步：项目工程识别

**必须首先执行**，读取项目根路径 `pom.xml`：

1. 提取 `<artifactId>`，识别工程类型和领域
2. **确认为业务主工程**（xxx-parent）— API 工程不支持联机交易
3. 确定领域：loan / dept / sett / comm
4. 向用户展示识别结果并确认

#### 第 2 步：验证交易码并确定模块

1. **验证交易码格式**：`T + (C/D/G/Y) + 4位数字`
2. **根据交易码前缀自动确定**：

| 前缀 | 领域 | 模块 | 基础目录 | 基础 flowtran package |
|------|------|------|---------|---------------------|
| TC | 存款 | dept-pbf | src/main/resources/trans | com.spdb.ccbs.dept.pbf.trans |
| TD | 贷款 | loan-pbf | src/main/resources/trans | com.spdb.ccbs.loan.pbf.trans |
| TG | 结算 | sett-pbf | src/main/resources/trans | com.spdb.ccbs.sett.pbf.trans |
| TY | 公共 | comm-pbf | src/main/resources/trans | com.spdb.ccbs.comm.pbf.trans |

#### 第 3 步：询问并确定子目录

询问用户是否需要子目录（如 `act`、`ft/repay`）：

- **有子目录**：追加到目录路径和 package
  - 文件路径：`{模块}/src/main/resources/trans/{子目录}/{交易码}.flowtrans.xml`
  - flowtran package：`com.spdb.ccbs.{领域}.pbf.trans.{子目录}`
  - interface package：`com.spdb.ccbs.{领域}.pbf.trans.{子目录}.intf`
  
- **无子目录**：使用基础路径
  - 文件路径：`{模块}/src/main/resources/trans/{交易码}.flowtrans.xml`
  - flowtran package：`com.spdb.ccbs.{领域}.pbf.trans`
  - interface package：`com.spdb.ccbs.{领域}.pbf.trans.intf`

**示例确认输出**：

```
✅ 工程识别完成
   - 工程类型: 业务主工程（loan-parent）
   - 交易码: TD1001
   - 领域: loan（贷款）
   - 模块: loan-pbf

📁 是否需要子目录？（如 act）
   用户输入: act

✅ 最终路径确认
   - 文件路径: loan-pbf/src/main/resources/trans/act/TD1001.flowtrans.xml
   - flowtran package: com.spdb.ccbs.loan.pbf.trans.act
   - interface package: com.spdb.ccbs.loan.pbf.trans.act.intf
```

#### 第 4 步：配置 Maven 仓库

调用 `configureMavenRepo` 完成交互。

#### 第 5 步：收集字段

收集 input 和 output 字段中文名，识别数组字段（id 以 Array 结尾）。

#### 第 6 步：批量查字典

去重后批量调用 `getDictDefByLongNameList`。

#### 第 7 步：过滤未贯标字段（硬性规则）

**必须严格执行**：

1. 检查 MCP 查询结果，识别返回 `null` 或 `undefined` 的字段
2. **未贯标字段绝对不写入 XML**，完全跳过
3. 收集未贯标字段的中文名称
4. 统一汇总提示用户

**输出示例**：

```
✅ 字段查询完成
   - 总字段数: 5
   - 已贯标: 3（custId, queryDate, chrgAmt）
   - 未贯标: 2（未知字段, 测试字段）

❌ 以下字段未贯标，未写入 XML：
   - 未知字段
   - 测试字段

💡 建议：请在字典系统中完成字段贯标后重新创建
```

#### 第 8 步：生成 XML

使用 flowtran package、interface package、字段元数据套模板。

#### 第 9 步：保存文件并回报

- 保存到：`{模块}/{目录路径}/{交易码}.flowtrans.xml`
- 回报：工程类型、模块、文件路径、flowtran package、interface package、字段统计、未贯标字段

### 修改流程

1. 配置 Maven → 2. 定位并读取原文件 → 3. 保留 `flowtran/description/interface` 属性 → 4. 查新增字段 → 5. 仅更新 `input/output` → 6. 保存

### 删除流程

直接删除目标 `*.flowtrans.xml` 文件。

### 数组字段

使用 `<fields>` 标签，`id` 以 "Array" 结尾，`multi="true"`, `scope=""`, `longname` 以"数组"结尾。

**深入了解请读取**：

- [references/transaction-id-rules.md](references/transaction-id-rules.md) — 交易码详细规则
- [references/package-module-mapping.md](references/package-module-mapping.md) — 包路径映射详情
- [references/xml-template.md](references/xml-template.md) — 联机交易 XML 完整模板与格式
- [references/array-fields.md](references/array-fields.md) — 数组字段详细处理
- [references/examples.md](references/examples.md) — 联机交易完整示例

---

## 复合类型模式（仅复合类型时读取）

仅在判定为复合类型模式后使用以下规则。

**前提**：必须先执行"项目工程自动识别"确定工程类型和领域，从而自动选择模块和 package 格式。

### 文件与标识

- 文件名：`{SchemaId}.c_schema.xml`
- `schema@id` 必须与文件名前缀一致且全局唯一

**ID 生成规则**：

- **schema@id（文件标识）**：
  - 用户提供 → 直接使用
  - 未提供 → 根据中文名翻译成英文，大驼峰（PascalCase），如：`福费延还款复合类型` → `FtAcctgType`
  
- **complexType@id（对象标识）**：
  - 用户提供 → 直接使用
  - 未提供 → 根据中文名翻译成英文，大驼峰，如：`福费延还款校验输入` → `FtAcctRepayChkInPojo`
  - **必须在同一文件内唯一**：若翻译后与已有 id 重复，需变更英文名确保唯一性

### 包路径与模块映射

| 领域 | 业务主工程（resources） | API 工程（beans） |
|------|----------------------|------------------|
| 存款 | `com.spdb.ccbs.dept.resources.type` / `dept-resources` | `com.spdb.dept.beans.type` / `dept-beans` |
| 贷款 | `com.spdb.ccbs.loan.resources.type` / `loan-resources` | `com.spdb.loan.beans.type` / `loan-beans` |
| 结算 | `com.spdb.ccbs.sett.resources.type` / `sett-resources` | `com.spdb.sett.beans.type` / `sett-beans` |
| 公共 | `com.spdb.ccbs.comm.resources.type` / `comm-resources` | `com.spdb.comm.beans.type` / `comm-beans` |

**基本规则**：

- 基础目录：`src/main/resources/type`
- 文件名：`{SchemaId}.c_schema.xml`
- API 工程 package 为 `com.spdb.{领域}`（无 `ccbs`）

**子目录处理（必须严格遵守）**：

若用户指定子目录（如 `ft/repay`），必须同步应用到两处：

1. **文件路径**：`{模块}/src/main/resources/type/{子目录}/{SchemaId}.c_schema.xml`
   - 业务主工程示例：`loan-resources/src/main/resources/type/ft/repay/FtAcctgType.c_schema.xml`
   - API 工程示例：`loan-beans/src/main/resources/type/account/CustAcctType.c_schema.xml`
   
2. **schema package**：`{基础package}.{子目录}`
   - 业务主工程示例：`com.spdb.ccbs.loan.resources.type.ft.repay`
   - API 工程示例：`com.spdb.loan.beans.type.account`

**多层子目录示例**：

- 文件路径：`loan-resources/src/main/resources/type/ft/repay/FtAcctgType.c_schema.xml`
- schema package：`com.spdb.ccbs.loan.resources.type.ft.repay`

### schema 标签属性（必须包含以下 6 个）

| 属性 | 必填 | 值 | 说明 |
|------|-----|-----|------|
| **`xmlns:xsi`** | ✅ | `http://www.w3.org/2001/XMLSchema-instance` | **固定值，第一个属性** |
| `id` | ✅ | 用户提供或翻译生成 | 文件标识，全局唯一 |
| **`package`** | ✅ | 根据工程类型和子目录自动生成 | **不能为空** |
| `longname` | ✅ | 用户提供的中文名 | 中文名称 |
| **`classgen`** | ✅ | `normal` | **固定值** |
| `xsi:noNamespaceSchemaLocation` | ✅ | `ltts-model.xsd` | 固定值 |

**属性顺序（必须严格遵守）**：

```xml
<schema xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="..." package="..." longname="..." classgen="normal" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
```

### complexType 标签

一个 `complexType` = 一个复合对象（对应 Java Bean）

- `id`：对象英文名（生成类名），同文件内唯一
- `longname`：对象中文名
- 默认属性：`abstract=false`, `dict=false`, `introduct=false`, `localName=""`, `extension=""`, `tags=""`

### 字段（element）属性规则（严格遵守）

**仅允许以下 11 个属性（禁止添加其他属性）**：

| 属性 | 来源 | 默认值 | 说明 |
|------|------|--------|------|
| `id` | 字典 MCP | - | 字段英文名 |
| `longname` | 字典 MCP | - | 字段中文名 |
| `type` | 字典 MCP | - | 字段类型 |
| `ref` | 字典 MCP | - | 字典引用（复合对象引用时不写） |
| `required` | 用户输入 | `false` | 是否必输 |
| `multi` | 固定值 | `false` | 是否多值（对象数组用 `true`） |
| `range` | 固定值 | `false` | 固定 |
| `array` | 固定值 | `false` | 固定 |
| `final` | 固定值 | `false` | 固定 |
| `override` | 固定值 | `false` | 固定 |
| `allowSubType` | 固定值 | `true` | 固定 |
| `key` | 固定值 | `false` | 固定 |

**禁止添加的属性（即使 MCP 返回也不写入）**：

- ❌ `dbname`（数据库字段名）
- ❌ `length`（字段长度）
- ❌ `precision`、`scale` 等其他属性

**关键规则**：

1. 从 MCP 查询结果只提取：`id`、`longname`、`type`、`ref`
2. 其他属性使用固定值或用户输入
3. 不得自行添加 MCP 返回的其他字段

### 复合对象引用（关键差异）

当字段引用其他复合类型时：

- `type` = `{SchemaId}.{ComplexTypeId}`
- **不写 `ref`**
- **不查字典 MCP**
- `multi=false` → 对象；`multi=true` → 对象数组（List）

**复合对象引用识别与检索流程（必须执行）**：

#### 第 1 步：识别复合对象引用

当用户提供的字段描述包含以下特征时，识别为复合对象引用：

- 字段中文名包含"对象""类型""信息""列表"等关键词
- 用户明确说明该字段引用其他复合类型
- 字段类型已包含 `.` 分隔符（如用户直接给出 `SomeType.SomePojo`）

#### 第 2 步：搜索 c_schema.xml 文件

**必须执行搜索**，在项目中查找所有 `*.c_schema.xml` 文件：

```bash
# 搜索模式（使用 Glob 工具）
**/*.c_schema.xml
```

**搜索范围**：

- `{模块}/src/main/resources/type/**/*.c_schema.xml`
- 涵盖所有子目录

#### 第 3 步：解析并匹配 complexType

对每个找到的 `.c_schema.xml` 文件：

1. **读取文件**，提取 `schema@id` 和所有 `complexType@id`、`complexType@longname`
2. **匹配用户提供的描述**：
   - 按 longname 匹配（优先）
   - 按 id 匹配（次选）
3. **记录匹配结果**：`{SchemaId}.{ComplexTypeId}`

**匹配示例**：

| 用户描述 | 找到的 c_schema.xml | schema@id | complexType@longname | 生成的 type |
|---------|-------------------|-----------|---------------------|------------|
| 银团贷款出资份额信息 | SyndAgrmLoanType.c_schema.xml | `SyndAgrmLoanType` | `银团贷款出资份额信息` | `SyndAgrmLoanType.SysdAgrmLoanInfoPojo` |
| 客户账户信息 | CustAcctType.c_schema.xml | `CustAcctType` | `客户账户信息` | `CustAcctType.CustAcctInfo` |

#### 第 4 步：确定引用格式

**找到匹配**：

- `type` = `{SchemaId}.{ComplexTypeId}`
- `multi` = 根据用户描述（是否为列表/数组）
- **不写 `ref` 属性**
- **不查字典 MCP**

**未找到匹配**：

- 提示用户：`❌ 未找到匹配的复合类型：{用户描述}`
- 询问用户：
  1. 是否为新复合类型（需要先创建）
  2. 是否为字典字段（需要查 MCP）
  3. 手动指定引用格式

**完整示例**：

```xml
<!-- 找到匹配的复合对象引用 -->
<element id="lstSyndAgrmLoanQryOutPojo" longname="银团贷款出资份额信息" type="SyndAgrmLoanType.SysdAgrmLoanInfoPojo" required="false" multi="false" range="false" array="false" final="false" override="false" allowSubType="true" key="false"/>

<!-- 注意：无 ref 属性 -->
```

#### 第 5 步：向用户确认

找到匹配后，向用户展示并确认：

```
✅ 找到复合对象引用
   - 字段描述: 银团贷款出资份额信息
   - 匹配到文件: loan-resources/src/main/resources/type/SyndAgrmLoanType.c_schema.xml
   - schema@id: SyndAgrmLoanType
   - complexType@id: SysdAgrmLoanInfoPojo
   - 生成的 type: SyndAgrmLoanType.SysdAgrmLoanInfoPojo
   
📋 确认使用此引用？
```

### 创建流程（必须严格按顺序执行）

#### 第 1 步：项目工程识别

**必须首先执行**，读取项目根路径 `pom.xml`：

1. 提取 `<artifactId>`，识别工程类型和领域
2. 确定工程类型：业务主工程（xxx-parent）或 API 工程（xxx-api-parent）
3. 确定领域：loan / dept / sett / comm
4. 向用户展示识别结果并确认

#### 第 2 步：确定模块与基础路径

**根据工程类型自动确定**：

| 工程类型 | 模块 | 基础目录 | 基础 package（schema@package 属性值） |
|---------|------|---------|-------------------------------------|
| 业务主工程（xxx-parent） | `{领域}-resources` | `src/main/resources/type` | `com.spdb.ccbs.{领域}.resources.type` |
| API 工程（xxx-api-parent） | `{领域}-beans` | `src/main/resources/type` | `com.spdb.{领域}.beans.type` |

**重要**：基础 package 是 `schema` 标签的 `package` 属性值，**必须填写，不能为空**。

#### 第 3 步：询问并确定子目录（影响 package 属性）

询问用户是否需要子目录（如 `ft/repay`、`account`）：

- **有子目录**：追加到目录路径和 package
  - 文件路径：`{模块}/src/main/resources/type/{子目录}/{SchemaId}.c_schema.xml`
  - **schema package（schema@package 属性）**：`{基础package}.{子目录}`（分隔符 `/` 变 `.`）
  
- **无子目录**：使用基础路径
  - 文件路径：`{模块}/src/main/resources/type/{SchemaId}.c_schema.xml`
  - **schema package（schema@package 属性）**：`{基础package}`

**关键规则**：

- **schema@package 属性必须填写**，不能为空
- 子目录分隔符：路径用 `/`，package 用 `.`
- 示例：子目录 `ft/repay` → package 追加 `.ft.repay`

**示例确认输出（必须向用户展示）**：

```
✅ 工程识别完成
   - 工程类型: 业务主工程（loan-parent）
   - 领域: loan（贷款）
   - 模块: loan-resources
   - 基础目录: src/main/resources/type
   - 基础 package: com.spdb.ccbs.loan.resources.type

📁 是否需要子目录？（如 ft/repay）
   用户输入: ft/repay

✅ 最终路径与 schema 属性确认
   - 文件路径: loan-resources/src/main/resources/type/ft/repay/{SchemaId}.c_schema.xml
   - schema@xmlns:xsi: http://www.w3.org/2001/XMLSchema-instance
   - schema@id: {SchemaId}
   - schema@package: com.spdb.ccbs.loan.resources.type.ft.repay
   - schema@longname: {用户提供的中文名}
   - schema@classgen: normal
   - schema@xsi:noNamespaceSchemaLocation: ltts-model.xsd
```

#### 第 4 步：确定 schema id

- 用户提供 id → 直接使用
- 未提供 → 根据中文名翻译成英文（大驼峰）

#### 第 5 步：配置 Maven 仓库（需要字典查询时）

调用 `configureMavenRepo` 完成交互。

#### 第 6 步：定义 complexType 和字段

1. 确认 complexType id（用户提供或翻译生成，确保文件内唯一）
2. 收集字段信息（中文名、是否必输、是否列表/数组）
3. **识别并处理复合对象引用字段**：
   - 判断字段是否引用其他复合类型（包含"对象""类型""信息"等关键词，或用户明确说明）
   - 若是复合引用 → 执行检索流程（搜索 `*.c_schema.xml`，匹配 complexType）
   - 若找到匹配 → 使用 `{SchemaId}.{ComplexTypeId}` 作为 type
   - 若未找到 → 提示用户并询问处理方式
4. 分类字段：
   - 复合对象引用字段（已确定 type，不查 MCP）
   - 字典字段（需要查 MCP）

#### 第 7 步：批量查字典（排除复合引用字段）

**查询范围**：

- ✅ **仅查询字典字段**（第 6 步中分类的"字典字段"）
- ❌ **不查询复合对象引用字段**（已在第 6 步确定 type）

**示例**：

假设有 5 个字段：

| 字段中文名 | 类型 | 是否查询 MCP |
|-----------|------|-------------|
| 申请编号 | 字典字段 | ✅ 查询 |
| 申请日期 | 字典字段 | ✅ 查询 |
| 银团贷款出资份额信息 | 复合对象引用 | ❌ 不查询（已确定 type=SyndAgrmLoanType.SysdAgrmLoanInfoPojo） |
| 收费金额 | 字典字段 | ✅ 查询 |
| 客户账户信息 | 复合对象引用 | ❌ 不查询（已确定 type=CustAcctType.CustAcctInfo） |

**调用 MCP**：

```javascript
// 仅查询字典字段
const dictFieldLongnames = ["申请编号", "申请日期", "收费金额"];

// 批量调用
const mcpResult = await getDictDefByLongNameList(dictFieldLongnames);
```

**关键规则**：

- 复合对象引用字段的 type 已在第 6 步确定，不需要查字典
- 复合对象引用字段不写 `ref` 属性

#### 第 8 步：过滤未贯标字段（硬性规则）

**必须严格执行**：

1. 检查 MCP 查询结果，识别返回 `null` 或 `undefined` 的字段
2. **未贯标字段绝对不写入 XML**，完全跳过，不生成 `<element>` 标签
3. 收集未贯标字段的中文名称
4. 统一汇总提示用户

**输出示例**：

```
✅ 字段查询完成
   - 总字段数: 8
   - 已贯标: 6（fRFTGDueBillCd, fncgBsnID, ...）
   - 未贯标: 2（临时字段1, 临时字段2）

❌ 以下字段未贯标，未写入 XML：
   - 临时字段1
   - 临时字段2

💡 建议：请在字典系统中完成字段贯标后重新创建
```

**关键**：未贯标字段不影响已贯标字段的正常生成，但必须明确告知用户。

#### 第 9 步：生成 XML

使用以下信息套模板生成 `schema` 标签：

**schema 标签必填属性（6 个）**：

| 属性 | 值来源 | 示例 |
|------|-------|------|
| **`xmlns:xsi`** | **固定值（第一个属性）** | `http://www.w3.org/2001/XMLSchema-instance` |
| `id` | 用户提供或翻译生成 | `FtAcctgType` |
| **`package`** | **第 2-3 步确定的 schema package** | `com.spdb.ccbs.loan.resources.type.ft.repay` |
| `longname` | 用户提供的中文名 | `福费延还款复合类型` |
| **`classgen`** | **固定值 `normal`** | `normal` |
| `xsi:noNamespaceSchemaLocation` | 固定值 | `ltts-model.xsd` |

**element 标签属性规则**：

- **仅使用 11 个允许的属性**：`id`、`longname`、`type`、`ref`、`required`、`multi`、`range`、`array`、`final`、`override`、`allowSubType`、`key`
- **禁止添加 MCP 返回的其他字段**：如 `dbname`、`length`、`precision` 等
- 从 MCP 只提取：`id`、`longname`、`type`、`ref`（复合引用时不写 `ref`）

**关键要求**：

- **xmlns:xsi 必须是第一个属性**，固定值
- **package 属性必须填写**，使用第 2-3 步确定的完整 schema package
- **不能留空或使用错误值**
- classgen 固定为 `normal`
- element 标签不得添加额外属性

#### 第 10 步：保存文件并回报

- 保存到：`{模块}/{目录路径}/{SchemaId}.c_schema.xml`
- 回报：工程类型、模块、文件路径、schema package、字段统计、未贯标字段

### 修改流程

1. 定位并读取原文件 → 2. 保留 `schema` 属性和其他 `complexType` → 3. 查新增字段 → 4. 更新目标 `complexType` 内的 `element` → 5. 保存

### 删除流程

- 删除整个文件：直接删除 `*.c_schema.xml`
- 删除单个 complexType：仅移除目标 `complexType` 标签，保留文件和其余对象

**深入了解请读取**：

- [references/composite-type-metadata.md](references/composite-type-metadata.md) — 复合类型完整规则与模板
- [references/composite-object-reference.md](references/composite-object-reference.md) — 复合对象引用检索流程详解

---

## 防干扰硬规则

**结构隔离规则**：

- 不在 `*.flowtrans.xml` 中生成 `schema/complexType/element`
- 不在 `*.c_schema.xml` 中生成 `flowtran/interface/input/output`
- 交易码规则仅用于联机交易；schema id 规则仅用于复合类型

**字段处理规则**：

- 字段引用判定：`type` 含 `.` 且匹配 `{SchemaId}.{ComplexTypeId}` → 复合引用（不查字典、不写 ref）；否则 → 字典字段
- **未贯标字段硬性约束**：MCP 查询返回 `null` 的字段**绝对不写入 XML**，无论是联机交易还是复合类型，无例外

**混合模式规则**：

- 混合需求拆分为两个独立产物，分别输出路径与统计
- 未贯标字段按模式分别统计和提示

详见 [references/mode-routing-and-anti-interference.md](references/mode-routing-and-anti-interference.md)

---

## MCP 服务

服务名：`dict-mcp-server`

| 方法 | 用途 | 参数 |
|------|------|------|
| `configureMavenRepo` | 配置 Maven 仓库路径 | action, mavenRepoPath, workingDirectory |
| `getDictDefByLongNameList` | 批量查询字段元数据 | longNameList, mavenRepoPath |

详见 [references/mcp-integration.md](references/mcp-integration.md)

---

## 参考资源索引

| 文档 | 适用范围 |
|------|---------|
| [project-detection.md](references/project-detection.md) | **通用（创建时必读）** |
| [mode-routing-and-anti-interference.md](references/mode-routing-and-anti-interference.md) | 通用 |
| [mcp-integration.md](references/mcp-integration.md) | 通用 |
| [error-handling.md](references/error-handling.md) | 通用 |
| [transaction-id-rules.md](references/transaction-id-rules.md) | 联机交易 |
| [package-module-mapping.md](references/package-module-mapping.md) | 联机交易 |
| [xml-template.md](references/xml-template.md) | 联机交易 |
| [array-fields.md](references/array-fields.md) | 联机交易 |
| [examples.md](references/examples.md) | 联机交易 |
| [composite-type-metadata.md](references/composite-type-metadata.md) | 复合类型 |
| [composite-object-reference.md](references/composite-object-reference.md) | **复合类型（引用时必读）** |
