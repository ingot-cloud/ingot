# Tasks

## 实施任务

- [x] T1：Java 前缀收口（`account.signal` / `account.bff` / `account.gateway`）+ JavaDoc + example.yml
  - 依赖：无
  - 验收：`@ConfigurationProperties` 新前缀；全库无旧前缀读取

- [x] T2：三环境 Nacos 重组（signal/delivery 进 policy；新建 `in-security-gateway.yml`；服务 yml 瘦身）
  - 依赖：T1
  - 验收：矩阵与 DESIGN 一致；数值与搬家前相同

- [x] T3：按消费关系改 `spring.config.import`；`NacosConstants.IN_SECURITY_GATEWAY`
  - 依赖：T2
  - 验收：BFF 挂 policy；Gateway 挂 gateway-policy；Auth 不挂

- [x] T4：更新 current `security/config-governance` 及 account / access / event / credential 相关 dataId 描述
  - 依赖：T1–T3
  - 验收：配置地图可按前缀查到 dataId 与消费者

## 验证任务

- [x] V1：全库检索旧前缀与 import 矩阵；相关模块可编译

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
