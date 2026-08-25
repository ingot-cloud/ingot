# Tasks

## 实施任务

- [x] T1：按 application name 隔离目录 + spool.lock；更新 example.yml、属性 JavaDoc、Nacos 注释
  - 依赖：无
  - 验收：同 parent 不同 app name 文件分离；第二实例锁失败
- [x] T2：完好空队列不扫描；inFlight 启动退回 pending；ack 后 truncate 无引用 active segment
  - 依赖：T1
  - 验收：drain 后重启 claim 为空；未 ack 重启可 claim
- [x] T3：state.json tmp+原子替换、compact、不序列化 record；损坏 quarantine；ack persist 失败回滚到 pending
  - 依赖：T2
  - 验收：截断 JSON 能构造 queue；残留 tmp 不影响完好 state
- [x] T4：补齐 `FileSpoolRecordQueueTest`
  - 依赖：T3
  - 验收：计划所列用例通过

## 验证任务

- [x] V1：执行 `ingot-security-recording` 模块测试

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
