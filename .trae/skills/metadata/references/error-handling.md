# 错误处理指南

本文档详细说明 flowtran 交易处理中各种错误场景的处理方式和提示模板。

## XML 格式错误

### 属性换行错误

**场景**: 标签属性被错误地分成多行

**错误示例**:
```xml
<field id="custId" 
       type="MBaseType.U_KE_HU_BIAN_HAO" 
       required="true"
       multi="false"
       array="false"
       longname="客户ID"/>
```

**正确格式** (属性必须单行):
```xml
<field id="custId" type="MBaseType.U_KE_HU_BIAN_HAO" required="true" multi="false" array="false" longname="客户ID" ref="MDict.C.custId"/>
```

**错误提示**:
```
❌ XML 格式错误

问题: 标签属性必须在一行内,不能换行

💡 格式要求:
   - 所有属性必须在一行内
   - 属性之间用空格分隔
   - 不要在属性间换行
```

### 缩进错误

**场景**: 标签缩进不正确

**错误示例**:
```xml
<flowtran ...>
<description>...</description>
  <interface ...>
      <input ...>
<field .../>
      </input>
  </interface>
</flowtran>
```

**正确格式** (层级缩进):
```xml
<flowtran ...>
    <description>...</description>
    <interface ...>
        <input ...>
            <field .../>
        </input>
    </interface>
</flowtran>
```

**错误提示**:
```
❌ XML 缩进错误

问题: 标签缩进不一致

💡 缩进规则:
   - 同层级标签缩进相同
   - 子层级基于父标签 + 4 个空格
   - 使用空格,不使用 Tab
   
层级示例:
<flowtran>              ← 0 个空格
    <interface>         ← 4 个空格
        <input>         ← 8 个空格
            <field/>    ← 12 个空格
```

### 标签间空行错误

**场景**: 标签之间有空行

**错误示例**:
```xml
<input packMode="true">
    <field id="custId" .../>
    
    <field id="queryDate" .../>
</input>
```

**正确格式** (无空行):
```xml
<input packMode="true">
    <field id="custId" .../>
    <field id="queryDate" .../>
</input>
```

**错误提示**:
```
⚠️  XML 格式建议

问题: 标签之间不应有空行

💡 格式要求:
   - 标签紧密排列
   - 同级标签之间不要有空行
   - 保持代码紧凑
```

## 交易码验证错误

### 缺少 T 前缀

**示例输入**: `Y291`, `C100`

**错误提示**:
```
❌ 交易码验证失败: 交易码必须以 T 开头

当前: Y291
正确: TY291

💡 正确格式: T + C/D/G/Y + 4位数字
```

### 领域前缀错误

**示例输入**: `TX291`, `TA100`

**错误提示**:
```
❌ 交易码验证失败: 交易码第二个字符必须是 C/D/G/Y

当前: TX291
问题: X 不是有效的领域前缀

💡 有效格式:
   - TC021-TC999: 存款领域
   - TD001-TD499: 贷款领域
   - TG001-TG499: 结算领域
   - TY001-TY999: 平台公共领域
```

### 数字范围错误

**示例**: `TC001`, `TD500`, `TG999`, `TY000`

**错误提示模板**:
```
❌ 交易码验证失败: {前缀}开头的交易码必须在 {最小值}-{最大值} 范围内

当前: {实际值}
有效范围: {最小值}-{最大值}

💡 请使用有效范围内的编码
```

## MCP 服务错误

### dict-mcp-server 不可用

**错误提示**:
```
❌ dict-mcp-server 服务调用失败

错误信息: Connection refused

💡 建议:
1. 检查 dict-mcp-server 服务是否运行
2. 确认网络连接正常
3. 稍后重试
```

### getDictDefByLongNameList 调用失败

**错误提示**:
```
❌ MCP 方法调用失败

服务: dict-mcp-server
方法: getDictDefByLongNameList
参数: ["国家", "性别", "客户ID"]
错误: Method error / Timeout

💡 请检查:
   - 方法签名是否正确
   - 参数格式是否正确
   - 服务是否正常运行
```

### MCP 返回格式错误

**错误提示**:
```
❌ MCP 返回数据格式异常

期望: Map<String, Object | null>
实际: 其他格式

💡 getDictDefByLongNameList 应返回:
{
  "字段中文名1": { id, type, longname, ref },
  "字段中文名2": { id, type, longname, ref },
  "未贯标字段": null
}
```

## 字段未贯标错误

### MCP 返回 null

**场景**: dict-mcp-server.getDictDefByLongNameList 返回的 Map 中某些字段的 value 为 null

**示例返回**:
```javascript
{
  "客户ID": { id: "custId", type: "...", ... },
  "未知字段": null,  // ← 未贯标
  "测试字段": null   // ← 未贯标
}
```

**错误提示**:
```
❌ 以下字段未贯标,需要在 MCP 系统中完成贯标处理:
  1. 未知字段 - dict-mcp-server 返回 null
  2. 测试字段 - dict-mcp-server 返回 null

📊 处理统计:
   - 总字段数: 3
   - 已贯标: 1 (客户ID)
   - 未贯标: 2

💡 下一步:
   1. 访问 dict-mcp-server 字段管理系统
   2. 添加上述字段的元数据定义
   3. 完成贯标后重新运行创建命令

⚠️  当前已跳过未贯标字段,仅处理已贯标字段
```

### MCP 返回对象缺少必需字段

**场景**: 返回的对象缺少 id, type 等字段

**错误提示**:
```
❌ MCP 返回数据不完整

字段中文名: 客户ID
返回数据: { "longname": "客户ID" }
缺少字段: id, type

💡 字段定义对象必须包含:
   - id: 字段英文名 (必需)
   - type: 字段类型 (必需)
   - longname: 字段中文名 (必需)
   - ref: 字典引用 (可选)
```

## 数组字段错误

### 数组 id 未以 Array 结尾

**错误提示**:
```
❌ 数组字段格式错误

字段: chargCd
问题: 数组字段 id 必须以 "Array" 结尾

当前: chargCd
正确: chargCdArray

修正:
chargCdArray 收费代码数组 start
    包含 fPrjCd  收费项目编码
chargCdArray 收费代码数组 end
```

### 缺少 start/end 标记

**错误提示**:
```
❌ 数组字段格式错误

字段: chargCdArray
问题: 缺少 start 或 end 标记

正确格式:
chargCdArray 收费代码数组 start
    包含 fPrjCd  收费项目编码
chargCdArray 收费代码数组 end
```

### start 和 end 不匹配

**错误提示**:
```
❌ 数组字段格式错误

问题: start 和 end 的数组名不一致

Start: chargCdArray
End: otherArray

正确:
chargCdArray 收费代码数组 start
    包含 fPrjCd  收费项目编码
chargCdArray 收费代码数组 end
```

## 文件操作错误

### 文件已存在(自动切换修改模式)

**场景**: 创建时文件已存在

**提示**:
```
ℹ️  检测到文件已存在,自动切换为修改模式

📁 文件: comm-pbf/src/main/resources/trans/TY291.flowtrans.xml
📋 交易码: TY291

✅ 将更新 input 和 output 标签内容
✅ 保留其他标签和属性不变
```

### 文件不存在(修改模式)

**场景**: 修改时文件不存在

**错误提示**:
```
❌ 文件不存在: TY999.flowtrans.xml

💡 如果要创建新交易,请使用:
   帮我新建 TY999 交易名称 的联机交易
```

## 批量错误处理

### 同时多个错误

**错误提示**:
```
⚠️  处理完成,但存在以下问题:

❌ 字段未贯标 (3个):
  1. 未知字段1 - 需要贯标
  2. 未知字段2 - 需要贯标
  3. 未知字段3 - 需要贯标

⚠️  数组格式警告 (1个):
  - chargCd - id 应以 Array 结尾

📊 处理统计:
   - 总字段数: 10
   - 成功处理: 6
   - 需要贯标: 3
   - 格式警告: 1

💡 下一步:
   1. 完成字段贯标
   2. 修正数组格式
   3. 重新运行命令
```

## 错误恢复指导

### 字段未贯标恢复

```
🔄 恢复步骤:

1️⃣  访问 dict-mcp-server 字段管理系统
2️⃣  添加字段元数据定义:
   - 字段英文名 (id)
   - 字段类型 (type)
   - 字段中文名 (longname)
   - 字典引用 (ref,可选)
3️⃣  完成贯标后重新运行
```

## 快速排错清单

- [ ] 交易码以 T 开头
- [ ] 第二字符为 C/D/G/Y
- [ ] 数字在有效范围内
- [ ] 文件扩展名 .flowtrans.xml
- [ ] dict-mcp-server 服务正常
- [ ] getDictDefByLongNameList 返回非 null
- [ ] 数组 id 以 Array 结尾
- [ ] 标签属性在一行内
- [ ] 标签间无空行
- [ ] 缩进使用 4 个空格
- [ ] 同层级标签缩进对齐
