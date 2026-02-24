# 项目工程自动识别

在创建元数据前，先扫描项目根路径的 `pom.xml`，自动识别工程类型和领域，确定模块、目录、package。

## 识别流程

### 1. 扫描 pom.xml

读取项目根路径 `pom.xml` 的 `<artifactId>` 内容。

### 2. 识别工程类型与领域

根据 `artifactId` 判定：

| artifactId | 工程类型 | 领域代码 | 领域名称 |
|------------|---------|---------|---------|
| loan-parent | 业务主工程 | loan | 贷款 |
| dept-parent | 业务主工程 | dept | 存款 |
| comm-parent | 业务主工程 | comm | 公共 |
| sett-parent | 业务主工程 | sett | 结算 |
| loan-api-parent | API工程 | loan | 贷款 |
| dept-api-parent | API工程 | dept | 存款 |
| comm-api-parent | API工程 | comm | 公共 |
| sett-api-parent | API工程 | sett | 结算 |

## 业务主工程规则

### 联机交易（业务主工程）

- **模块**：`{领域代码}-pbf`
- **基础目录**：`src/main/resources/trans`
- **基础 flowtran package**：`com.spdb.ccbs.{领域代码}.pbf.trans`
- **基础 interface package**：`com.spdb.ccbs.{领域代码}.pbf.trans.intf`

**无子目录示例**：

| 领域 | 模块 | 文件路径 | flowtran package | interface package |
|------|------|---------|-----------------|------------------|
| loan | loan-pbf | loan-pbf/src/main/resources/trans/TD1001.flowtrans.xml | com.spdb.ccbs.loan.pbf.trans | com.spdb.ccbs.loan.pbf.trans.intf |
| dept | dept-pbf | dept-pbf/src/main/resources/trans/TC1001.flowtrans.xml | com.spdb.ccbs.dept.pbf.trans | com.spdb.ccbs.dept.pbf.trans.intf |

**有子目录示例（用户指定 `act` 子目录）**：

| 项目 | 值 |
|------|---|
| 文件路径 | `loan-pbf/src/main/resources/trans/act/TD1001.flowtrans.xml` |
| flowtran package | `com.spdb.ccbs.loan.pbf.trans.act` |
| interface package | `com.spdb.ccbs.loan.pbf.trans.act.intf` |

**多层子目录示例（用户指定 `ft/repay` 子目录）**：

| 项目 | 值 |
|------|---|
| 文件路径 | `loan-pbf/src/main/resources/trans/ft/repay/TD1001.flowtrans.xml` |
| flowtran package | `com.spdb.ccbs.loan.pbf.trans.ft.repay` |
| interface package | `com.spdb.ccbs.loan.pbf.trans.ft.repay.intf` |

**关键规则**：

1. 子目录必须同时追加到**文件路径**和**package**
2. 路径分隔符用 `/`，package 分隔符用 `.`
3. interface package = flowtran package + `.intf`

### 复合类型（业务主工程）

- **模块**：`{领域代码}-resources`
- **基础目录**：`src/main/resources/type`
- **基础 schema package**：`com.spdb.ccbs.{领域代码}.resources.type`

**无子目录示例**：

| 领域 | 模块 | 文件路径 | schema package |
|------|------|---------|---------------|
| loan | loan-resources | loan-resources/src/main/resources/type/FtAcctgType.c_schema.xml | com.spdb.ccbs.loan.resources.type |
| sett | sett-resources | sett-resources/src/main/resources/type/SettType.c_schema.xml | com.spdb.ccbs.sett.resources.type |

**有子目录示例（用户指定 `ft/repay` 子目录）**：

| 项目 | 值 |
|------|---|
| 文件路径 | `loan-resources/src/main/resources/type/ft/repay/FtAcctgType.c_schema.xml` |
| schema package | `com.spdb.ccbs.loan.resources.type.ft.repay` |

**单层子目录示例（用户指定 `account` 子目录）**：

| 项目 | 值 |
|------|---|
| 文件路径 | `loan-resources/src/main/resources/type/account/AcctType.c_schema.xml` |
| schema package | `com.spdb.ccbs.loan.resources.type.account` |

**关键规则**：

1. 子目录必须同时追加到**文件路径**和**package**
2. 路径分隔符用 `/`，package 分隔符用 `.`

## API 工程规则

### 复合类型（API 工程）

- **模块**：`{领域代码}-beans`
- **基础目录**：`src/main/resources/type`
- **基础 schema package**：`com.spdb.{领域代码}.beans.type`

**注意**：API 工程的 package 为 `com.spdb.{领域代码}`，**没有** `ccbs` 段。

**无子目录示例**：

| 领域 | 模块 | 文件路径 | schema package |
|------|------|---------|---------------|
| loan | loan-beans | loan-beans/src/main/resources/type/CustType.c_schema.xml | com.spdb.loan.beans.type |
| dept | dept-beans | dept-beans/src/main/resources/type/AcctType.c_schema.xml | com.spdb.dept.beans.type |

**有子目录示例（用户指定 `account` 子目录）**：

| 项目 | 值 |
|------|---|
| 文件路径 | `loan-beans/src/main/resources/type/account/CustAcctType.c_schema.xml` |
| schema package | `com.spdb.loan.beans.type.account` |

**多层子目录示例（用户指定 `customer/info` 子目录）**：

| 项目 | 值 |
|------|---|
| 文件路径 | `loan-beans/src/main/resources/type/customer/info/CustInfoType.c_schema.xml` |
| schema package | `com.spdb.loan.beans.type.customer.info` |

**关键规则**：

1. 子目录必须同时追加到**文件路径**和**package**
2. 路径分隔符用 `/`，package 分隔符用 `.`
3. API 工程 package 不含 `ccbs`

### 联机交易（API 工程）

**API 工程不创建联机交易元数据**，仅创建复合类型。若用户在 API 工程下请求创建联机交易，应提示：

```
⚠️  当前为 API 工程（{artifactId}），不支持创建联机交易元数据。
    联机交易应在业务主工程（{领域}-parent）下创建。
```

## 完整识别示例

### 示例 1：业务主工程 + 联机交易

**pom.xml**：`<artifactId>loan-parent</artifactId>`

**识别结果**：

- 工程类型：业务主工程
- 领域：loan（贷款）
- 模块：loan-pbf
- 目录：src/main/resources/trans
- flowtran package：com.spdb.ccbs.loan.pbf.trans
- interface package：com.spdb.ccbs.loan.pbf.trans.intf

### 示例 2：业务主工程 + 复合类型

**pom.xml**：`<artifactId>sett-parent</artifactId>`

**识别结果**：

- 工程类型：业务主工程
- 领域：sett（结算）
- 模块：sett-resources
- 目录：src/main/resources/type
- schema package：com.spdb.ccbs.sett.resources.type

### 示例 3：API 工程 + 复合类型

**pom.xml**：`<artifactId>dept-api-parent</artifactId>`

**识别结果**：

- 工程类型：API 工程
- 领域：dept（存款）
- 模块：dept-beans
- 目录：src/main/resources/type
- schema package：com.spdb.dept.beans.type

## 识别失败处理

若 `artifactId` 不匹配任何已知工程类型，提示用户：

```
❌ 无法识别当前工程类型

pom.xml artifactId: {实际值}

支持的 artifactId：
  - 业务主工程: loan-parent / dept-parent / comm-parent / sett-parent
  - API 工程: loan-api-parent / dept-api-parent / comm-api-parent / sett-api-parent

请确认当前项目是否为上述工程之一。
```

## 用户交互流程

1. **自动扫描**：读取 `pom.xml`，识别工程类型和领域
2. **显示识别结果**：向用户展示识别到的信息
3. **确认或修正**：询问用户是否正确，若不正确则手动指定
4. **确认子目录**：询问是否创建子目录，若有则追加到路径和 package
5. **继续流程**：进入字段查询和 XML 生成
