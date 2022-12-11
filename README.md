# ingot

## 功能点
* [ ] RBAC
* [ ] 多租户
* [ ] dubbo
* [ ] grpc
* [ ] Seata
* [ ] Sentinel
* [X] 脱敏组件
* [ ] Token管理
  * [ ] Token信息缓存优化
  * [ ] 增加签退逻辑，签退某个Token的时候，需要将其缓存在Redis中，Token正常认证流程中，如果发现Redis存在该Token，那么代表需要签退
  * [ ] 需要考虑唯一用户情况如何判断
* [ ] 缓存相关
  * [ ] client缓存
  * [ ] token缓存


