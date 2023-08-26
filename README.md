# ingot

## Versions
  * Java: 17
  * Spring Cloud: 2022.0.4
  * Spring Cloud Alibaba: 2022.0.0.0
  * Spring Authorization Server: 1.1.1
  * hutool: 5.8.21
  * mybatis plus: 3.5.3.2

## 功能点
* [ ] RBAC
* [X] 多租户
* [ ] dubbo
* [ ] grpc
* [ ] Seata
* [ ] Sentinel
* [ ] Token管理
  * [ ] Token信息缓存优化
  * [ ] 增加签退逻辑，签退某个Token的时候，需要将其缓存在Redis中，Token正常认证流程中，如果发现Redis存在该Token，那么代表需要签退
  * [ ] 需要考虑唯一用户情况如何判断
* [x] 验证码模块
  * [x] 图形验证码
  * [x] 邮箱验证码
  * [x] 短信验证码
* [x] 优化拆分store模块
* [ ] 分离账号体系，前端用户独立表
* [ ] 登录成功，异步日志通知

### 授权
 * [ ] 默认使用Authorization Code + PKCE
 * [ ] 自有APP登录默认使用Password或Social登录模式，注意Client Secret不暴露