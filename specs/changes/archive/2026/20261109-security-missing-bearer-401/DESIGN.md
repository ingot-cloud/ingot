# 修复设计

Bearer 认证过滤器在没有令牌时继续过滤器链。随后 `InTokenAuthFilter` 在异常翻译过滤器之前抛出 OAuth2 异常，导致受保护路径返回 500。

在 `InTokenAuthFilter` 的无身份、错误认证类型和缺少 sid 分支，复用资源服务器现有的 `InBearerTokenAuthenticationEntryPoint` 完成响应并结束该请求。拒绝发生在业务过滤器链之前。保持过滤器顺序和现有构造器签名，兼容共享组件调用方。变更只涉及错误响应处理，无数据迁移和配置要求，回退应用制品即可恢复旧版本。
