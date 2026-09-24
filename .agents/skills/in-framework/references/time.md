# 接口时间

接口上的墙钟时间（响应里的创建/更新时间、后台配置时间、前端提交的时间）按**本次请求的前端当地时间**解释和返回。数据库默认存 UTC。定时任务触发、过期/锁定/有效期是否到期、授权截止这类瞬时判断不跟请求时区走。

## 接口

- 格式继续用 `yyyy-MM-dd HH:mm:ss`（`InJavaTimeModule`），字符串本身不带偏移。
- 序列化、反序列化都按本次请求能识别出的客户端时区解释。
- 识别不到时区时用 `Asia/Shanghai`。禁止写 `Asia/Beijing`（该 ID 无效，`TimeZone.getTimeZone` 会落到 GMT）。
- 不要把库存 UTC 墙钟直接当本地时间返回，也不要把前端当地时间当 UTC 入库。进出 API 时在边界转换。

## 库存

- `LocalDateTime` 字段默认表示 UTC。写入用 `LocalDateTime.now(ZoneOffset.UTC)` 或 `LocalDateTime.ofInstant(instant, ZoneOffset.UTC)`。
- JDBC 会话时区与库存口径对齐为 UTC；不要在连接串里用 `serverTimezone=Asia/Shanghai` 把 UTC 墙钟再偏一次。

## 判断与定时

- 比较「现在是否已过某时刻」用 `Instant`，或把两边都换到同一套 UTC 时钟再比。
- 定时任务的触发时刻按任务配置的时区（或系统调度时钟）执行，不要用当前请求的前端时区去算下一次触发。
- 用户时区只影响接口展示和用户提交的墙钟，不影响任务是否该跑、锁是否该解、授权是否已过期。

## 不要做的事

- 不要在业务代码里写 `LocalDateTime.now()`（系统默认时区）再拿去入库或跟 UTC 字段比较。
- 不要为了展示方便把库里的 UTC `LocalDateTime` 原样塞进响应。
- 不要在 Jackson 全局 `timeZone` 上写死一个与请求无关的北京/上海时区后，就认为接口时间已经正确；请求有客户端时区时以前端为准。
