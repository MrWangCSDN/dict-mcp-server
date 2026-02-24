# Flowtran 完整示例集

本文档提供 flowtran 交易创建和修改的完整示例。

## 示例 1: 基本交易创建 (TY291)

**用户输入**:
```
帮我新建 TY291 收费明细文件批量查询 的联机交易

输入:
cst     国家    非必输
xb      性别    必输

输出:
cst     国家
xb      性别
```

**生成的 XML** (注意:属性单行,标签间无空行,层级缩进):
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="TY291" longname="收费明细文件批量查询" kind="auto" package="com.spdb.ccbs.comm.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[收费明细文件批量查询]]></description>
    <interface package="com.spdb.ccbs.comm.pbf.trans.intf">
        <input packMode="true">
            <field id="cst" type="MBaseType.U_GUO_JIA" required="false" multi="false" array="false" longname="国家" ref="MDict.C.cst"/>
            <field id="xb" type="MBaseType.U_XING_BIE" required="true" multi="false" array="false" longname="性别" ref="MDict.X.xb"/>
        </input>
        <output asParm="true" packMode="true">
            <field id="cst" type="MBaseType.U_GUO_JIA" required="false" multi="false" array="false" longname="国家" ref="MDict.C.cst"/>
            <field id="xb" type="MBaseType.U_XING_BIE" required="false" multi="false" array="false" longname="性别" ref="MDict.X.xb"/>
        </output>
    </interface>
</flowtran>
```

**文件路径**: `comm-pbf/src/main/resources/trans/TY291.flowtrans.xml`

## 示例 2: 带数组字段的交易 (TY292)

**用户输入**:
```
帮我新建 TY292 客户信息查询 的联机交易

输入:
custId  客户ID  必输
chargCdArray 收费代码数组 start
    包含 fPrjCd   收费项目编码  非必输
    包含 chrgAmt  收费金额     必输
chargCdArray 收费代码数组 end

输出:
custName  客户名称
resultArray 结果数组 start
    包含 code  代码
    包含 msg   消息
resultArray 结果数组 end
```

**生成的 XML** (注意 fields 内 field 多缩进 4 个空格):
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

## 示例 3: 子目录创建 (TY291 in chrg)

**用户输入**:
```
帮我在 chrg 子目录下新建 TY291 收费明细查询 的联机交易

输入:
fPrjCd  收费项目编码  必输

输出:
fPrjCd  收费项目编码
chrgAmt 收费金额
```

**生成的 XML**:
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="TY291" longname="收费明细查询" kind="auto" package="com.spdb.ccbs.comm.pbf.trans.chrg" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[收费明细查询]]></description>
    <interface package="com.spdb.ccbs.comm.pbf.trans.chrg.intf">
        <input packMode="true">
            <field id="fPrjCd" type="MBaseType.U_SHOU_FEI_XIANG_MU_BIAN_MA" required="true" multi="false" array="false" longname="收费项目编码" ref="MDict.F.fPrjCd"/>
        </input>
        <output asParm="true" packMode="true">
            <field id="fPrjCd" type="MBaseType.U_SHOU_FEI_XIANG_MU_BIAN_MA" required="false" multi="false" array="false" longname="收费项目编码" ref="MDict.F.fPrjCd"/>
            <field id="chrgAmt" type="MBaseType.U_JIN_E" required="false" multi="false" array="false" longname="收费金额" ref="MDict.C.chrgAmt"/>
        </output>
    </interface>
</flowtran>
```

**文件路径**: `comm-pbf/src/main/resources/trans/chrg/TY291.flowtrans.xml`

**关键差异**:
- package: `com.spdb.ccbs.comm.pbf.trans.chrg` (追加了 .chrg)
- interface package: `com.spdb.ccbs.comm.pbf.trans.chrg.intf`

## 示例 4: 不同领域交易

### 存款领域 (TC100)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="TC100" longname="存款账户查询" kind="auto" package="com.spdb.ccbs.dept.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[存款账户查询]]></description>
    <interface package="com.spdb.ccbs.dept.pbf.trans.intf">
        <input packMode="true">
            <field id="accountNo" type="MBaseType.U_ZHANG_HAO" required="true" multi="false" array="false" longname="账号" ref="MDict.A.accountNo"/>
        </input>
        <output asParm="true" packMode="true">
            <field id="accountNo" type="MBaseType.U_ZHANG_HAO" required="false" multi="false" array="false" longname="账号" ref="MDict.A.accountNo"/>
            <field id="balance" type="MBaseType.U_JIN_E" required="false" multi="false" array="false" longname="余额" ref="MDict.B.balance"/>
        </output>
    </interface>
</flowtran>
```

**文件**: `dept-pbf/src/main/resources/trans/TC100.flowtrans.xml`

### 贷款领域 (TD250)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="TD250" longname="贷款申请" kind="auto" package="com.spdb.ccbs.loan.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[贷款申请]]></description>
    <interface package="com.spdb.ccbs.loan.pbf.trans.intf">
        <input packMode="true">
            <field id="custId" type="MBaseType.U_KE_HU_BIAN_HAO" required="true" multi="false" array="false" longname="客户ID" ref="MDict.C.custId"/>
            <field id="loanAmount" type="MBaseType.U_JIN_E" required="true" multi="false" array="false" longname="贷款金额" ref="MDict.L.loanAmount"/>
        </input>
        <output asParm="true" packMode="true">
            <field id="applicationId" type="MBaseType.U_BIAN_HAO" required="false" multi="false" array="false" longname="申请编号" ref="MDict.A.applicationId"/>
            <field id="approvalStatus" type="MBaseType.U_ZHUANG_TAI" required="false" multi="false" array="false" longname="审批状态" ref="MDict.A.approvalStatus"/>
        </output>
    </interface>
</flowtran>
```

**文件**: `loan-pbf/src/main/resources/trans/TD250.flowtrans.xml`

## 示例 5: 修改现有交易

**原 TY291 文件内容**:
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran id="TY291" longname="收费明细文件批量查询" kind="auto" package="com.spdb.ccbs.comm.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[收费明细文件批量查询]]></description>
    <interface package="com.spdb.ccbs.comm.pbf.trans.intf">
        <input packMode="true">
            <field id="cst" type="MBaseType.U_GUO_JIA" required="false" multi="false" array="false" longname="国家" ref="MDict.C.cst"/>
        </input>
        <output asParm="true" packMode="true">
            <field id="cst" type="MBaseType.U_GUO_JIA" required="false" multi="false" array="false" longname="国家" ref="MDict.C.cst"/>
        </output>
    </interface>
</flowtran>
```

**用户输入**:
```
修改 TY291

输入:
cst     国家    非必输
xb      性别    必输
newField  新字段  非必输

输出:
cst     国家
xb      性别
newField  新字段
result    结果
```

**修改后的文件** (保留 flowtran/description/interface 属性,仅更新 input/output):
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran id="TY291" longname="收费明细文件批量查询" kind="auto" package="com.spdb.ccbs.comm.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[收费明细文件批量查询]]></description>
    <interface package="com.spdb.ccbs.comm.pbf.trans.intf">
        <input packMode="true">
            <field id="cst" type="MBaseType.U_GUO_JIA" required="false" multi="false" array="false" longname="国家" ref="MDict.C.cst"/>
            <field id="xb" type="MBaseType.U_XING_BIE" required="true" multi="false" array="false" longname="性别" ref="MDict.X.xb"/>
            <field id="newField" type="MBaseType.U_NEW_FIELD" required="false" multi="false" array="false" longname="新字段" ref="MDict.N.newField"/>
        </input>
        <output asParm="true" packMode="true">
            <field id="cst" type="MBaseType.U_GUO_JIA" required="false" multi="false" array="false" longname="国家" ref="MDict.C.cst"/>
            <field id="xb" type="MBaseType.U_XING_BIE" required="false" multi="false" array="false" longname="性别" ref="MDict.X.xb"/>
            <field id="newField" type="MBaseType.U_NEW_FIELD" required="false" multi="false" array="false" longname="新字段" ref="MDict.N.newField"/>
            <field id="result" type="MBaseType.U_JIE_GUO" required="false" multi="false" array="false" longname="结果" ref="MDict.R.result"/>
        </output>
    </interface>
</flowtran>
```

**关键**:
- ✅ 保留 flowtran 标签的所有属性
- ✅ 保留 description 标签内容
- ✅ 保留 interface package 属性
- ✅ 只更新 input 和 output 的内容
- ✅ 保持正确的缩进层级

## 示例 6: 复杂数组嵌套

**用户输入**:
```
帮我新建 TY295 客户账户综合查询 的联机交易

输入:
custId  客户ID  必输
accountArray 账户数组 start
    包含 accountNo    账号        必输
    包含 accountType  账户类型     非必输
accountArray 账户数组 end
transArray 交易数组 start
    包含 transId      交易ID      必输
    包含 transAmt     交易金额     必输
transArray 交易数组 end
queryDate  查询日期  非必输

输出:
custName  客户名称
accountArray 账户数组 start
    包含 accountNo    账号
    包含 balance      余额
accountArray 账户数组 end
totalCount  总记录数
```

**生成的 XML** (完整层级缩进):
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<flowtran xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="TY295" longname="客户账户综合查询" kind="auto" package="com.spdb.ccbs.comm.pbf.trans" txnMode="A" xsi:noNamespaceSchemaLocation="ltts-model.xsd">
    <description><![CDATA[客户账户综合查询]]></description>
    <interface package="com.spdb.ccbs.comm.pbf.trans.intf">
        <input packMode="true">
            <field id="custId" type="MBaseType.U_KE_HU_BIAN_HAO" required="true" multi="false" array="false" longname="客户ID" ref="MDict.C.custId"/>
            <fields id="accountArray" scope="" required="false" multi="true" array="false" longname="账户数组">
                <field id="accountNo" type="MBaseType.U_ZHANG_HAO" required="true" multi="false" array="false" longname="账号" ref="MDict.A.accountNo"/>
                <field id="accountType" type="MBaseType.U_ZHANG_HU_LEI_XING" required="false" multi="false" array="false" longname="账户类型" ref="MDict.A.accountType"/>
            </fields>
            <fields id="transArray" scope="" required="false" multi="true" array="false" longname="交易数组">
                <field id="transId" type="MBaseType.U_JIAO_YI_BIAN_HAO" required="true" multi="false" array="false" longname="交易ID" ref="MDict.T.transId"/>
                <field id="transAmt" type="MBaseType.U_JIN_E" required="true" multi="false" array="false" longname="交易金额" ref="MDict.T.transAmt"/>
            </fields>
            <field id="queryDate" type="MBaseType.U_RI_QI" required="false" multi="false" array="false" longname="查询日期" ref="MDict.Q.queryDate"/>
        </input>
        <output asParm="true" packMode="true">
            <field id="custName" type="MBaseType.U_KE_HU_MING_CHENG" required="false" multi="false" array="false" longname="客户名称" ref="MDict.C.custName"/>
            <fields id="accountArray" scope="" required="false" multi="true" array="false" longname="账户数组">
                <field id="accountNo" type="MBaseType.U_ZHANG_HAO" required="false" multi="false" array="false" longname="账号" ref="MDict.A.accountNo"/>
                <field id="balance" type="MBaseType.U_YU_E" required="false" multi="false" array="false" longname="余额" ref="MDict.B.balance"/>
            </fields>
            <field id="totalCount" type="MBaseType.U_JI_SHU" required="false" multi="false" array="false" longname="总记录数" ref="MDict.T.totalCount"/>
        </output>
    </interface>
</flowtran>
```

## 缩进层级对照表

| 标签路径 | 层级 | 空格数 | 示例 |
|---------|------|--------|------|
| flowtran | 0 | 0 | `<flowtran>` |
| flowtran/description | 1 | 4 | `    <description>` |
| flowtran/interface | 1 | 4 | `    <interface>` |
| flowtran/interface/input | 2 | 8 | `        <input>` |
| flowtran/interface/input/field | 3 | 12 | `            <field>` |
| flowtran/interface/input/fields | 3 | 12 | `            <fields>` |
| flowtran/interface/input/fields/field | 4 | 16 | `                <field>` |

## 快速参考

### XML 格式清单

- [ ] 所有属性在一行内
- [ ] 标签之间无空行
- [ ] flowtran 子标签缩进 4 个空格
- [ ] interface 子标签缩进 8 个空格  
- [ ] input/output 子标签缩进 12 个空格
- [ ] fields 内 field 缩进 16 个空格
- [ ] 使用空格不是 Tab
- [ ] 同层级标签对齐

### 创建命令模板

```
帮我新建 {交易码} {交易名称} 的联机交易

输入:
{字段} {中文名} {必输/非必输}

输出:
{字段} {中文名}
```
