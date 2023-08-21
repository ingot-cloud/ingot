# ingot

## Versions
  * Java: 17
  * Spring Cloud: 2022.0.4
  * Spring Cloud Alibaba: 2022.0.0.0
  * Spring Authorization Server: 1.1.1

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

## 版本
* [ ] 更新Spring Cloud 2022, Java 17