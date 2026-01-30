# 凭证安全模块文档说明

## 📚 文档结构

本模块采用三层文档结构，满足不同角色的阅读需求：

### 1. 快速入口（模块 README）

| 文档 | 位置 | 适合人群 | 阅读时间 |
|-----|------|---------|---------|
| **ingot-security-credential/README.md** | 核心模块 | 所有开发者 | 10分钟 |
| **ingot-security-credential-data/README.md** | 数据层模块 | 需要数据管理的开发者 | 5分钟 |

**内容：**
- 模块简介和核心特性
- 5分钟快速开始
- 主要功能和使用示例
- 配置说明

---

### 2. 核心文档（docs/credential-security/）

| 文档 | 大小 | 适合人群 | 阅读时间 | 说明 |
|-----|------|---------|---------|------|
| **README.md** | 30K | 所有人 | 15分钟 | 完整概述、架构、使用场景 |
| **POLICY-LOADER.md** | 18K | 架构师、开发者 | 20分钟 | 策略加载器详细设计 |
| **ARCHITECTURE.md** | 29K | 架构师 | 25分钟 | 完整架构设计文档 |
| **POLICY-GUIDE.md** | 21K | 运维、开发者 | 15分钟 | 策略配置详细指南 |
| **API-REFERENCE.md** | 25K | 开发者、测试 | 20分钟 | 完整的 API 文档 |
| **FAQ.md** | 27K | 所有人 | - | 常见问题解答 |

**总文档大小：** 150K  
**预计阅读时间：** 约 2 小时（完整阅读）

---

## 🎯 推荐阅读路径

### 路径 1：快速上手（30分钟）

适合：想快速集成的开发者

```
1. ingot-security-credential/README.md（10分钟）
   └─→ 了解核心特性和基本用法
   
2. docs/credential-security/README.md（15分钟）
   └─→ 了解完整功能和架构概览
   
3. 动手实践（5分钟）
   └─→ 添加依赖 + 配置 + 运行示例
```

---

### 路径 2：深入理解（2小时）

适合：架构师、技术负责人

```
1. docs/credential-security/README.md（15分钟）
   └─→ 整体概览
   
2. docs/credential-security/POLICY-LOADER.md（20分钟）
   └─→ 策略加载器设计
   
3. docs/credential-security/ARCHITECTURE.md（25分钟）
   └─→ 完整架构设计
   
4. docs/credential-security/POLICY-GUIDE.md（15分钟）
   └─→ 策略配置详解
   
5. docs/credential-security/API-REFERENCE.md（20分钟）
   └─→ API 接口文档
   
6. docs/credential-security/FAQ.md（按需查阅）
   └─→ 常见问题解答
```

---

### 路径 3：数据层集成（15分钟）

适合：需要密码历史和过期管理的开发者

```
1. ingot-security-credential-data/README.md（5分钟）
   └─→ 了解数据层功能
   
2. 执行数据库脚本（2分钟）
   └─→ 创建表结构
   
3. 添加依赖并使用（3分钟）
   └─→ 集成到项目
   
4. 查看完整文档（5分钟）
   └─→ docs/credential-security/README.md
```

---

## 📖 文档内容概览

### README.md（主文档）

**章节结构：**
1. 模块概述 - 核心特性、设计目标
2. 30秒快速开始 - 最简单的使用示例
3. 文档导航 - 所有文档的索引
4. 模块架构 - 三层架构、策略加载模式、依赖选择
5. 场景驱动设计 - 不同场景的策略组合
6. 统一服务接口 - CredentialSecurityService 说明
7. 核心设计 - 策略加载器、场景驱动、统一服务、默认空实现
8. 性能指标 - 各场景性能数据
9. 数据库设计 - 表结构和 SQL 脚本
10. 集成方案 - 三种集成方案对比
11. 使用对比 - 优化前后对比
12. 技术栈 - 依赖的技术
13. 架构优势 - 设计理念
14. 策略加载模式详解 - Local vs Remote
15. 扩展性 - 自定义加载器和策略
16. 部署指南 - 数据库初始化、配置、启动
17. 安全控制 - 权限和审计
18. 模块结构 - 详细的文件结构

---

### POLICY-LOADER.md（策略加载器）

**章节结构：**
1. 概述 - 设计目标
2. 架构设计 - 核心接口、三种实现
3. 实现详解
   - LocalCredentialPolicyLoader
   - RemoteCredentialPolicyLoader
   - DynamicCredentialPolicyLoader
4. 策略工具类 - PasswordPolicyUtil
5. 配置切换 - 自动配置逻辑
6. 使用场景 - 4种典型场景
7. 最佳实践 - 模式选择、缓存策略
8. 扩展性 - 自定义加载器
9. 性能指标 - 性能对比
10. 常见问题 - FAQ

---

### ARCHITECTURE.md（架构设计）

**章节结构：**
1. 设计目标 - 统一策略、解耦协同、联邦式数据
2. 架构分层 - 四层架构图
3. 系统交互流程 - 注册、修改、登录流程
4. 核心类图 - 详细的类关系
5. 数据流转 - 数据如何在系统中流转
6. 部署架构 - 物理部署结构
7. 核心组件 - 各组件详细说明
8. 策略管理 - 策略的生命周期
9. 高可用设计 - 降级、缓存、兜底
10. 安全设计 - 权限控制、审计日志

---

### POLICY-GUIDE.md（策略配置）

**章节结构：**
1. 策略类型 - 强度、历史、过期
2. 配置方式 - 本地配置、远程配置
3. 详细配置说明 - 每个策略的所有参数
4. 配置示例 - 标准、高安全、C端应用
5. 多租户配置 - 租户级策略
6. 优先级控制 - 策略优先级
7. 测试策略 - 如何测试配置

---

### API-REFERENCE.md（API 参考）

**章节结构：**
1. RPC 接口 - 所有 Feign 接口
2. REST 接口 - 所有 HTTP 接口
3. 请求示例 - 完整的请求/响应
4. 错误码 - 所有错误码说明
5. 数据模型 - DTO、VO 说明

---

### FAQ.md（常见问题）

**章节结构：**
1. 功能相关 - 策略、校验、历史、过期
2. 集成相关 - 依赖、配置、启动
3. 性能相关 - 优化、缓存、并发
4. 故障排查 - 常见错误和解决方案

---

## 🔍 文档查找指南

### 我想了解...

| 需求 | 推荐文档 | 章节 |
|-----|---------|------|
| **快速上手** | ingot-security-credential/README.md | 5分钟快速开始 |
| **策略加载模式** | POLICY-LOADER.md | 完整文档 |
| **如何配置策略** | POLICY-GUIDE.md | 详细配置说明 |
| **架构设计原理** | ARCHITECTURE.md | 架构分层、核心组件 |
| **API 接口** | API-REFERENCE.md | RPC/REST 接口 |
| **集成数据层** | ingot-security-credential-data/README.md | 完整文档 |
| **性能指标** | README.md | 性能指标章节 |
| **多租户配置** | POLICY-GUIDE.md | 多租户配置章节 |
| **常见错误** | FAQ.md | 故障排查章节 |
| **扩展开发** | POLICY-LOADER.md | 扩展性章节 |

---

## ✅ 文档质量保证

### 1. 完整性 ✅

- ✅ 覆盖所有核心功能
- ✅ 包含完整的使用示例
- ✅ 提供详细的配置说明
- ✅ 包含性能指标和最佳实践

### 2. 准确性 ✅

- ✅ 所有示例代码可运行
- ✅ 配置项与代码一致
- ✅ API 文档与实现一致

### 3. 易读性 ✅

- ✅ 清晰的章节结构
- ✅ 丰富的图表和示例
- ✅ 多层次的阅读路径
- ✅ 快速查找指南

### 4. 实用性 ✅

- ✅ 快速开始指南（5分钟）
- ✅ 完整的配置示例
- ✅ 常见问题解答
- ✅ 故障排查指南

---

## 📝 文档维护

### 更新原则

1. **代码优先** - 代码改动后立即更新文档
2. **示例真实** - 所有示例代码必须可运行
3. **版本同步** - 文档版本与代码版本保持一致

### 需要更新的场景

- ✅ 新增功能
- ✅ 修改配置项
- ✅ 变更 API
- ✅ 性能优化
- ✅ Bug 修复（如果影响使用）

### 不需要更新的场景

- ❌ 内部重构（不影响外部接口）
- ❌ 代码风格调整
- ❌ 单元测试增加

---

## 🎓 团队成员如何使用

### 新成员入职

```
Day 1: 阅读 README.md（主文档）
       └─→ 了解整体架构和核心概念
       
Day 2: 阅读 POLICY-LOADER.md
       └─→ 理解策略加载机制
       
Day 3: 实践集成
       └─→ 在测试项目中集成模块
       
Day 4: 阅读 ARCHITECTURE.md
       └─→ 深入理解架构设计
       
Day 5: 参考 FAQ.md
       └─→ 了解常见问题和解决方案
```

### 开发新功能

1. 阅读 ARCHITECTURE.md - 理解架构约束
2. 参考 POLICY-GUIDE.md - 了解现有策略
3. 查看 API-REFERENCE.md - 理解接口规范
4. 实现功能
5. 更新相关文档

### 解决问题

1. 查看 FAQ.md - 是否是常见问题
2. 阅读对应文档章节 - 理解原理
3. 查看示例代码 - 对比实现
4. 如果是新问题，补充到 FAQ.md

---

## 📊 文档覆盖矩阵

| 主题 | README | POLICY-LOADER | ARCHITECTURE | POLICY-GUIDE | API-REF | FAQ |
|-----|--------|--------------|--------------|-------------|---------|-----|
| 快速开始 | ✅✅✅ | ✅ | - | ✅ | - | ✅ |
| 策略加载 | ✅✅ | ✅✅✅ | ✅ | - | - | ✅ |
| 架构设计 | ✅✅ | ✅ | ✅✅✅ | - | - | - |
| 策略配置 | ✅ | - | - | ✅✅✅ | - | ✅ |
| API 接口 | ✅ | - | - | - | ✅✅✅ | ✅ |
| 使用示例 | ✅✅ | ✅✅ | ✅ | ✅✅ | ✅✅ | - |
| 性能指标 | ✅✅ | ✅✅ | ✅ | - | - | ✅ |
| 故障排查 | - | ✅ | - | - | - | ✅✅✅ |
| 扩展开发 | ✅ | ✅✅✅ | ✅ | - | - | ✅ |

**图例：**
- ✅✅✅ 完整覆盖
- ✅✅ 部分覆盖
- ✅ 简要提及
- \- 不涉及

---

## 🚀 快速链接

### 核心文档
- [README.md](./README.md) - 主文档
- [POLICY-LOADER.md](./POLICY-LOADER.md) - 策略加载器
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 架构设计

### 模块文档
- [ingot-security-credential/README.md](../../ingot-framework/ingot-security-credential/README.md)
- [ingot-security-credential-data/README.md](../../ingot-framework/ingot-security-credential-data/README.md)

### 参考文档
- [POLICY-GUIDE.md](./POLICY-GUIDE.md) - 策略配置
- [API-REFERENCE.md](./API-REFERENCE.md) - API 参考
- [FAQ.md](./FAQ.md) - 常见问题

---

**版本：** 1.0  
**最后更新：** 2026-01-30  
**维护者：** Ingot Cloud Team
