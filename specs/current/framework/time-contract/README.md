# 统一时间契约与 UTC 处理

公共 API 以带时区 ISO-8601 表达时间点，后端响应、业务存储及瞬时判断统一 UTC；日期、日内时间、Duration、Cron 和 OAuth/JWT 数字时间保留各自语义。

2026-10-11，负责人 jy 确认跨服务联调已验收，并接受时间改造之外的既有测试问题另行跟踪。

- [当前契约与接入规则](./SPEC.md)
- [业务服务接入指南](../../../../docs/guides/TIME-CONTRACT.md)
- [已归档变更](../../../changes/archive/2026/20261008-framework-time-contract/README.md)
- [实施证据与最终验收决议](../../../changes/archive/2026/20261008-framework-time-contract/ACCEPTANCE.md)
- [既有问题独立跟踪](../../../changes/archive/2026/20261008-framework-time-contract/FOLLOW_UP.md)

公共实现涉及 commons、core、Feign、数据访问、安全、验证码、TSS、网关和各服务。关联前端实现位于 ingot-admin 的 shared 时间工具及消费页面；该仓库的独立 change 保留自身状态，本次归档仅处理本仓库 `20261008-framework-time-contract`。
