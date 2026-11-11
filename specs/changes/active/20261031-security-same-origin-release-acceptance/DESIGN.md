# 环境验收执行设计

## 当前架构与部署方式

- 平台 `platform-admin / PLATFORM` 与租户 `tenant-admin / TENANT` 各自部署管理宿主，登录插件随宿主提供 `/login/`。每个 origin 的 `/api` 代理 Gateway/BFF，不部署独立登录站。
- 默认两个 HTTPS 域名；纯 IP 双 HTTPS 端口为可选方案。Gateway/BFF 注册相同的两个 origin，按可信 Host/端口和应用身份处理；内部 OAuth 协议回调不增加浏览器入口。
- [平台宿主](../../../../../ingot-admin/apps/admin-platform/src/main.ts)、[租户宿主](../../../../../ingot-admin/apps/admin/src/main.ts)与[后端注册校验](../../../../ingot-framework/ingot-commons/src/main/java/com/ingot/framework/commons/utils/BffOriginPolicy.java)是本轮核对的实现入口。运行契约沿[同源登录基线](../../../current/security/same-origin-login/SPEC.md)。
- 当前交付采用新系统首次部署；不预设有可迁移旧会话或可回退的四站环境。

## 已有证据与剩余边界

| 能力 | 已有结果与来源 | 本 change 的剩余边界 |
|---|---|---|
| 两端登录、退出、切换组织、多标签页 | 2026-10-31 用户本地确认，见[同源登录最终验收](../../archive/2026/20261031-security-same-origin-login/VALIDATION.md#用户本地验收确认与归档2026-10-31)。 | 保留已通过结论；所选目标部署做必要冒烟，不重新要求整套本地验收。 |
| 真实服务接入、账号与会话规则 | [IAM 验收收口](../../archive/2026/20260912-iam-identity-access-management/ACCEPTANCE-CLOSEOUT.md#原编号处置)已记录真实 Auth/Gateway/BFF、新库首登改密、成功/错误登录、锁定恢复及密码/会话回归；强制表单与退出/返回另有受控 BFF 浏览器证据。[用户最终确认](../../archive/2026/20260912-iam-identity-access-management/USER-ACCEPTANCE.md)于 2026-11-11 通过。 | 按场景复用各自证据类型，目标资格变更、重放、多节点/网络及依赖故障缺口仍由 R03 跟踪，不将夹具结果扩大为真实全链路结果。 |
| 登录外观、配色与错误提示 | [外观 change](../../../../../ingot-admin/specs/changes/archive/2026/20261030-ingot-login-appearance/README.md#完成记录)与[错误提示 change](../../../../../ingot-admin/specs/changes/archive/2026/20261106-ingot-login-error-feedback/README.md#完成记录)均于 2026-11-06 用户验收并归档。 | 不再列为待完成视觉项目；新环境若出现回归，按实际缺陷处理。 |
| CA/SAN、反代、真实休眠、目标发布及恢复 | 原本地自动化与受控浏览器结果保留在[同源登录验证记录](../../archive/2026/20261031-security-same-origin-login/VALIDATION.md)。自签证书忽略信任错误和虚拟时钟不构成实际环境证据。 | R01/R02/R04/R05 继续待验；本轮不新增目标环境完成结论。 |

## 执行与证据记录

1. E00 锁定实际目标模式、两个 origin、证书、前后端/Auth/BFF/Gateway 版本、配置版本、测试账号、网络条件、执行窗口及操作范围。确认目标是否确有旧系统，并选择可执行的失败恢复路径后进入 `approved`。
2. 复用[归档 API](../../archive/2026/20261031-security-same-origin-login/API.md)、[原验收矩阵](../../archive/2026/20261031-security-same-origin-login/ACCEPTANCE.md)和前端受控脚本作为场景依据；按上表复用已通过结果，列出目标环境差异与缺口后执行 E01–E04。
3. 每项区分本地自动化、受控浏览器、用户本地确认与目标环境实测。记录场景编号、环境/模式、完整版本、执行者、实际时间、预期与实际结果、脱敏证据及失败处置；不适用项附条件和原因。
4. 证据只保留脱敏阶段、错误码、请求次数、Cookie 属性和补偿结果，不保存密码、Token、Cookie 值或 sid。真实休眠和长停留记录实际经历，不用加速时钟补齐。

目标参数尚未提供，属于后续执行前置条件；本次文档校准不启动部署或故障演练，也不重置已验收的本地环境。

## 首次部署与失败恢复

- 首次部署成套准备两前端、Auth/BFF/Gateway、Nacos、RegisteredClient、TLS 与反代配置；按实际目标做登录、租户选择、退出、强制改密门禁及独立会话冒烟，核对监控与持久补偿。初始化沿[当前持久化基线](../../../current/iam/database-persistence/SPEC.md)，本次 Spec 调整不执行初始化。
- 有已验证的双入口版本时，使用与其匹配的整套制品和配置回退；没有旧版本时，明确停止新入口、恢复本次配置变更、处置本次测试会话并修复重试的步骤。恢复后重新核对入口、身份和补偿结果，不承诺恢复已撤销 sid。
- [运行部署手册](../../../../deploy/services/SAME-ORIGIN-LOGIN.md)与[归档部署设计](../../archive/2026/20261031-security-same-origin-login/DEPLOYMENT.md)中的注册、反代、Cookie、监控和补偿规则继续适用；旧登录站停写、v1 Cookie/Redis 盘点清理及旧四站回退段落仅在目标确有旧系统且已确认该迁移范围时适用。首次部署不据这些历史段落额外建设四站，也不恢复旧库迁移门禁。

## 缺陷与完成

若验收发现实现缺陷，更新本 change 的设计、测试与批准记录后修正；若改变已归档接口/安全边界，重新对齐方案，不能借验收直接改变契约。

E01–E04 的适用项通过、失败处置完成且证据汇总后，进入 `validating`。E05 经用户确认实际发布事实后，按最终环境结果更新适用 current、记录完成信息并归档；本次校准保持 `draft`，不提前更新 current。
