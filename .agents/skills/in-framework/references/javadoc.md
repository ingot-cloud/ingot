# JavaDoc

动笔前先读仓库 [docs/standards/Javadoc.md](../../../../docs/standards/Javadoc.md)，与本篇冲突时以该文件为准。

1. 读完整目标类型及足够上下文，确认职责、协作者、生命周期、约束和调用方。
2. 必须注释：类型（`class` / `interface` / `enum` / `record` / `@interface` 及嵌套类型）；对外方法（接口方法、供其他模块使用的 `public` 方法）；对外字段（`@ConfigurationProperties`、会被其他模块 get/set 的 field）。不注释 private helper、Lombok getter/setter（注释写在 field 上）。
3. 按代码事实写简洁中文 JavaDoc，写职责和边界，不要复述名字或罗列成员。
4. 校验每个链接、示例、标签和断言后再落盘。新增或修改对外契约时注释一并写入；用户只要审查时只给建议不改源码。

类型：首段一个 `<p>...</p>` 一句话说明职责；仅当设计意图、场景、边界或约束确有帮助时再写第二段；能从可见 API 推导出正确示例时用 `<pre>{@code ... }</pre>`。标签顺序：`@author`、`@since`、可选 `@see`、可选 `@apiNote`、可选 `@implNote`。JavaDoc 紧挨类型注解之前。

方法写做什么、约束和失败语义，不要复述方法名。`@param` / `@return` / `@throws` 只在签名看不出来时写。字段写含义、默认值和合法取值；配置字段补 YAML 或模式语义。不要占位符或空标签。保留仍然正确的既有信息。

作者与版本：① 用户或当前任务显式指定的 `@author` / `@since`；② 更新既有注释时保留非空值；③ 新建且无显式值时用 `@author jy`、`@since 1.0.0`。不得从 Git 历史、邻近文件或当前日期推断。
