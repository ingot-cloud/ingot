# ingot

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
* [ ] 验证码
  * [ ] 图形验证码
  * [ ] 邮箱验证码
  * [ ] 短信验证码
* [ ] 分离账号体系，前端用户独立表

## 版本
* [ ] 更新Spring Cloud 2022, Java 17