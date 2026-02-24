# 包路径和模块映射规则（联机交易）

本文档详细说明根据交易码前缀自动确定包路径、模块名和文件路径的规则。

**前提**：在执行映射前，必须先通过 [project-detection.md](project-detection.md) 自动识别项目工程类型，确认当前为**业务主工程**。API 工程不创建联机交易元数据。

## 基本映射规则

### 核心映射表

| 交易码前缀 | 包路径 | 模块名 | 基础文件路径 |
|----------|--------|--------|-------------|
| **TC** | com.spdb.ccbs.dept.pbf.trans | dept-pbf | dept-pbf/src/main/resources/trans |
| **TD** | com.spdb.ccbs.loan.pbf.trans | loan-pbf | loan-pbf/src/main/resources/trans |
| **TG** | com.spdb.ccbs.sett.pbf.trans | sett-pbf | sett-pbf/src/main/resources/trans |
| **TY** | com.spdb.ccbs.comm.pbf.trans | comm-pbf | comm-pbf/src/main/resources/trans |

### 映射规则说明

**包路径规则**:
- TC → `com.spdb.ccbs.dept.pbf.trans` (存款)
- TD → `com.spdb.ccbs.loan.pbf.trans` (贷款)
- TG → `com.spdb.ccbs.sett.pbf.trans` (结算)
- TY → `com.spdb.ccbs.comm.pbf.trans` (平台公共)

**模块名规则**:
- TC → `dept-pbf` (Department - 存款)
- TD → `loan-pbf` (Loan - 贷款)
- TG → `sett-pbf` (Settlement - 结算)
- TY → `comm-pbf` (Common - 平台公共)

**文件路径规则**:
- 所有模块统一使用: `{模块名}/src/main/resources/trans`
- Maven 项目标准结构

## 详细示例

### TC 开头 - 存款领域

**交易码**: TC100

**生成信息**:
```yaml
交易码: TC100
领域: 对公分布式核心存款领域
包路径: com.spdb.ccbs.dept.pbf.trans
interface包: com.spdb.ccbs.dept.pbf.trans.intf
模块名: dept-pbf
文件路径: dept-pbf/src/main/resources/trans/TC100.flowtrans
```

**XML flowtran 标签**:
```xml
<flowtran id="TC100" 
          package="com.spdb.ccbs.dept.pbf.trans"
          ...>
```

**XML interface 标签**:
```xml
<interface package="com.spdb.ccbs.dept.pbf.trans.intf">
```

### TD 开头 - 贷款领域

**交易码**: TD250

**生成信息**:
```yaml
交易码: TD250
领域: 对公分布式核心贷款领域
包路径: com.spdb.ccbs.loan.pbf.trans
interface包: com.spdb.ccbs.loan.pbf.trans.intf
模块名: loan-pbf
文件路径: loan-pbf/src/main/resources/trans/TD250.flowtrans
```

**XML 标签**:
```xml
<flowtran id="TD250" 
          package="com.spdb.ccbs.loan.pbf.trans"
          ...>
    <interface package="com.spdb.ccbs.loan.pbf.trans.intf">
```

### TG 开头 - 结算领域

**交易码**: TG350

**生成信息**:
```yaml
交易码: TG350
领域: 对公分布式核心结算领域
包路径: com.spdb.ccbs.sett.pbf.trans
interface包: com.spdb.ccbs.sett.pbf.trans.intf
模块名: sett-pbf
文件路径: sett-pbf/src/main/resources/trans/TG350.flowtrans
```

### TY 开头 - 平台公共领域

**交易码**: TY291

**生成信息**:
```yaml
交易码: TY291
领域: 对公分布式核心平台公共领域
包路径: com.spdb.ccbs.comm.pbf.trans
interface包: com.spdb.ccbs.comm.pbf.trans.intf
模块名: comm-pbf
文件路径: comm-pbf/src/main/resources/trans/TY291.flowtrans
```

## 子目录处理

### 子目录识别

**关键词识别**: "在 {子目录名} 子目录下"

**示例**:
```
帮我在 chrg 子目录下新建 TY291 的联机交易
帮我在 sttt 子目录下新建 TC100 的联机交易
```

### 子目录路径调整

**基本规则**:
- 文件路径: 追加子目录
- 包路径: 追加子目录(用点分隔)
- interface 包: 在 flowtran 包路径基础上追加 .intf

**示例 1: TY291 在 chrg 子目录**

```yaml
自然语言: 帮我在 chrg 子目录下新建 TY291 的联机交易

交易码: TY291
基础包路径: com.spdb.ccbs.comm.pbf.trans
子目录: chrg
实际包路径: com.spdb.ccbs.comm.pbf.trans.chrg
interface包: com.spdb.ccbs.comm.pbf.trans.chrg.intf

基础文件路径: comm-pbf/src/main/resources/trans
实际文件路径: comm-pbf/src/main/resources/trans/chrg/TY291.flowtrans
```

**生成的 XML**:
```xml
<flowtran id="TY291" 
          package="com.spdb.ccbs.comm.pbf.trans.chrg"
          ...>
    <interface package="com.spdb.ccbs.comm.pbf.trans.chrg.intf">
```

**示例 2: TC100 在 sttt 子目录**

```yaml
自然语言: 帮我在 sttt 子目录下新建 TC100 的联机交易

交易码: TC100
基础包路径: com.spdb.ccbs.dept.pbf.trans
子目录: sttt
实际包路径: com.spdb.ccbs.dept.pbf.trans.sttt
interface包: com.spdb.ccbs.dept.pbf.trans.sttt.intf

实际文件路径: dept-pbf/src/main/resources/trans/sttt/TC100.flowtrans
```

**示例 3: 多级子目录**

```yaml
自然语言: 帮我在 chrg/plszn 子目录下新建 TY291 的联机交易

子目录: chrg/plszn
实际包路径: com.spdb.ccbs.comm.pbf.trans.chrg.plszn
interface包: com.spdb.ccbs.comm.pbf.trans.chrg.plszn.intf
实际文件路径: comm-pbf/src/main/resources/trans/chrg/plszn/TY291.flowtrans
```

### 子目录创建

**规则**: 如果子目录不存在,自动创建

```python
import os

def ensure_directory_exists(file_path: str):
    """确保目录存在,不存在则创建"""
    directory = os.path.dirname(file_path)
    if not os.path.exists(directory):
        os.makedirs(directory)
        print(f"✅ 创建子目录: {directory}")
```

## Interface 包路径规则

### 基本规则

**interface 包路径 = flowtran 包路径 + ".intf"**

**示例**:
- flowtran package: `com.spdb.ccbs.comm.pbf.trans`
- interface package: `com.spdb.ccbs.comm.pbf.trans.intf`

**带子目录示例**:
- flowtran package: `com.spdb.ccbs.comm.pbf.trans.chrg.plszn`
- interface package: `com.spdb.ccbs.comm.pbf.trans.chrg.plszn.intf`

### XML 表示

```xml
<flowtran id="TY291" 
          package="com.spdb.ccbs.comm.pbf.trans.chrg.plszn"
          ...>
    <interface package="com.spdb.ccbs.comm.pbf.trans.chrg.plszn.intf">
        <input>...</input>
        <output>...</output>
    </interface>
</flowtran>
```

## 实现示例

### Python 实现

```python
def get_package_info(trans_id: str, subdirectory: str = "") -> dict:
    """
    根据交易码获取包路径信息
    
    Args:
        trans_id: 交易码 (如 TY291)
        subdirectory: 子目录 (可选,如 chrg 或 chrg/plszn)
        
    Returns:
        包含包路径、模块名、文件路径等信息的字典
    """
    trans_id = trans_id.upper()
    prefix = trans_id[1]  # C/D/G/Y
    
    # 基础映射
    mapping = {
        'C': {
            'package': 'com.spdb.ccbs.dept.pbf.trans',
            'module': 'dept-pbf',
            'domain': '对公分布式核心存款领域'
        },
        'D': {
            'package': 'com.spdb.ccbs.loan.pbf.trans',
            'module': 'loan-pbf',
            'domain': '对公分布式核心贷款领域'
        },
        'G': {
            'package': 'com.spdb.ccbs.sett.pbf.trans',
            'module': 'sett-pbf',
            'domain': '对公分布式核心结算领域'
        },
        'Y': {
            'package': 'com.spdb.ccbs.comm.pbf.trans',
            'module': 'comm-pbf',
            'domain': '对公分布式核心平台公共领域'
        }
    }
    
    base_info = mapping.get(prefix)
    if not base_info:
        raise ValueError(f"无效的交易码前缀: {prefix}")
    
    # 处理子目录
    package_path = base_info['package']
    file_path = f"{base_info['module']}/src/main/resources/trans"
    
    if subdirectory:
        # 将路径分隔符转换为包路径格式
        sub_package = subdirectory.replace('/', '.')
        package_path = f"{package_path}.{sub_package}"
        file_path = f"{file_path}/{subdirectory}"
    
    # 生成完整信息
    return {
        'trans_id': trans_id,
        'domain': base_info['domain'],
        'module': base_info['module'],
        'package': package_path,
        'interface_package': f"{package_path}.intf",
        'file_path': f"{file_path}/{trans_id}.flowtrans",
        'directory': file_path,
        'filename': f"{trans_id}.flowtrans"
    }


# 使用示例
info = get_package_info('TY291', 'chrg/plszn')
print(info)
# {
#     'trans_id': 'TY291',
#     'domain': '对公分布式核心平台公共领域',
#     'module': 'comm-pbf',
#     'package': 'com.spdb.ccbs.comm.pbf.trans.chrg.plszn',
#     'interface_package': 'com.spdb.ccbs.comm.pbf.trans.chrg.plszn.intf',
#     'file_path': 'comm-pbf/src/main/resources/trans/chrg/plszn/TY291.flowtrans',
#     'directory': 'comm-pbf/src/main/resources/trans/chrg/plszn',
#     'filename': 'TY291.flowtrans'
# }
```

### JavaScript/TypeScript 实现

```typescript
interface PackageInfo {
    transId: string;
    domain: string;
    module: string;
    package: string;
    interfacePackage: string;
    filePath: string;
    directory: string;
    filename: string;
}

function getPackageInfo(transId: string, subdirectory: string = ""): PackageInfo {
    transId = transId.toUpperCase();
    const prefix = transId[1];  // C/D/G/Y
    
    const mapping: {[key: string]: {package: string, module: string, domain: string}} = {
        'C': {
            package: 'com.spdb.ccbs.dept.pbf.trans',
            module: 'dept-pbf',
            domain: '对公分布式核心存款领域'
        },
        'D': {
            package: 'com.spdb.ccbs.loan.pbf.trans',
            module: 'loan-pbf',
            domain: '对公分布式核心贷款领域'
        },
        'G': {
            package: 'com.spdb.ccbs.sett.pbf.trans',
            module: 'sett-pbf',
            domain: '对公分布式核心结算领域'
        },
        'Y': {
            package: 'com.spdb.ccbs.comm.pbf.trans',
            module: 'comm-pbf',
            domain: '对公分布式核心平台公共领域'
        }
    };
    
    const baseInfo = mapping[prefix];
    if (!baseInfo) {
        throw new Error(`无效的交易码前缀: ${prefix}`);
    }
    
    let packagePath = baseInfo.package;
    let filePath = `${baseInfo.module}/src/main/resources/trans`;
    
    if (subdirectory) {
        const subPackage = subdirectory.replace(/\//g, '.');
        packagePath = `${packagePath}.${subPackage}`;
        filePath = `${filePath}/${subdirectory}`;
    }
    
    return {
        transId: transId,
        domain: baseInfo.domain,
        module: baseInfo.module,
        package: packagePath,
        interfacePackage: `${packagePath}.intf`,
        filePath: `${filePath}/${transId}.flowtrans`,
        directory: filePath,
        filename: `${transId}.flowtrans`
    };
}
```

## 完整路径示例

### 无子目录示例

| 交易码 | 完整文件路径 | flowtran package | interface package |
|-------|-------------|------------------|-------------------|
| TC100 | dept-pbf/src/main/resources/trans/TC100.flowtrans | com.spdb.ccbs.dept.pbf.trans | com.spdb.ccbs.dept.pbf.trans.intf |
| TD250 | loan-pbf/src/main/resources/trans/TD250.flowtrans | com.spdb.ccbs.loan.pbf.trans | com.spdb.ccbs.loan.pbf.trans.intf |
| TG350 | sett-pbf/src/main/resources/trans/TG350.flowtrans | com.spdb.ccbs.sett.pbf.trans | com.spdb.ccbs.sett.pbf.trans.intf |
| TY291 | comm-pbf/src/main/resources/trans/TY291.flowtrans | com.spdb.ccbs.comm.pbf.trans | com.spdb.ccbs.comm.pbf.trans.intf |

### 带子目录示例

| 交易码 | 子目录 | 完整文件路径 | flowtran package | interface package |
|-------|-------|-------------|------------------|-------------------|
| TY291 | chrg | comm-pbf/src/main/resources/trans/chrg/TY291.flowtrans | com.spdb.ccbs.comm.pbf.trans.chrg | com.spdb.ccbs.comm.pbf.trans.chrg.intf |
| TY291 | chrg/plszn | comm-pbf/src/main/resources/trans/chrg/plszn/TY291.flowtrans | com.spdb.ccbs.comm.pbf.trans.chrg.plszn | com.spdb.ccbs.comm.pbf.trans.chrg.plszn.intf |
| TC100 | sttt | dept-pbf/src/main/resources/trans/sttt/TC100.flowtrans | com.spdb.ccbs.dept.pbf.trans.sttt | com.spdb.ccbs.dept.pbf.trans.sttt.intf |

## 常见问题

### Q: 如何判断使用哪个模块?
**A**: 根据交易码的第二个字符(C/D/G/Y)自动确定:
- TC → dept-pbf
- TD → loan-pbf
- TG → sett-pbf
- TY → comm-pbf

### Q: 子目录路径格式?
**A**: 
- 文件路径: 使用 `/` 分隔 (如 `chrg/plszn`)
- 包路径: 使用 `.` 分隔 (如 `chrg.plszn`)

### Q: interface 包路径如何生成?
**A**: 在 flowtran 包路径后面加 `.intf`

### Q: 多级子目录如何处理?
**A**: 支持多级子目录,如 `chrg/plszn/sub`,自动转换为包路径格式

### Q: 子目录不存在怎么办?
**A**: 自动创建目录,包括所有父目录
