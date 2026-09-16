# IAM 脱敏元数据预检

本工具是 T02 的离线预检入口。只读取 JSON 清单，不连接任何数据库，不执行 SQL，不导入数据。`source_catalog.json` 来自仓库 28 张源表的 CREATE TABLE 元数据；实际源库多出或缺少表、列不一致时均阻塞，不能把仓库结构当成线上结构证明。

```sh
python3 tools/iam/migration/test_preflight.py
python3 tools/iam/migration/preflight.py \
  --manifest tools/iam/migration/fixtures/metadata.json \
  --output /tmp/ingot-iam-metadata-preflight.json
```

`fixtures/metadata.json` 全部为合成数据，包含一份完整输入示例。实际输入必须从一致快照提取并由迁移操作者提供；目前尚未实现从业务数据库自动导出该清单。清单包括：

- 批次、源快照指纹、源/目标标识、规则版本。标识使用字母、数字、下划线、点或连字符，不填写连接字符串或密码。
- `tables`：表名、完整列名及实际行数；未知行数必须为 null 并会阻塞。
- `accounts`：账号 ID、enabled/deleted 事实及 credentialVerified。此布尔值必须来自受控凭证格式核查；工具本身不读取摘要，因此不构成密码可登录证明。
- `tenants`、`memberships`、`departments`：标识、状态和必要关联。相应事实数量须与源表行数一致。
- `permissions`、`targetActions` 与带 actor/reason 的 `actionMappings`：旧权限必须显式映射到具体目标操作。没有映射的通配权限会阻塞，工具不按字符串相似或角色名称授予权限。
- `ownerMappings`：带 actor/reason 的显式所有者映射，必须指向本组织关系中的有效账号。不会从多个成员或管理员角色自动推断所有者。
- `platformCandidates`、`platformMappings` 和 platformIdentityReviewed：明确候选账号及独立平台成员 ID；缺失映射或重复标识均阻塞，不使用默认租户推断平台资格。

只接受声明的脱敏字段。账号对象带 passwordHash、token 等额外字段会被拒绝；报告仅包含必要标识、固定错误码、表级去向和状态计数，不回显旧权限码、处置理由或敏感原值。输出文件不能覆盖输入；已有报告只允许相同批次、相同清单内容再次生成。

返回码：0 表示这份元数据预检未发现已覆盖规则的阻塞项；1 表示生成了阻塞报告；2 表示输入或输出约定无效。PREFLIGHTED 不是 VERIFIED，readyForImport 和 readyForCutover 始终为 false。

尚未覆盖凭证摘要的实际格式核查、所有辅助表内容验证、实际平台治理授权拆分、范围包含关系证明、开通期限转换、真实目标库导入及源库/目标库连接隔离核验。缺少这些验证不能切换。源/目标标识不同仅是输入检查，不证明实际连接隔离。成员和账号状态保留还须在实际导入/verify 阶段验证，不能仅以状态计数验收 M02。

## 幂等导入与校验（不连业务库）

`import_tool.py` 提供 dry-run / import / verify / report。必须先 `preflight` 为 PREFLIGHTED；批次映射文件绑定 sourceFingerprint，源变化不能复用。import 只写入显式 `--target-dir` 的 JSON 映射结果，不打开 JDBC。verify 读取主体+操作比较清单，EXPANDED 无处置或 UNRESOLVED 则失败；readyForImport/readyForCutover 始终为 false。

```sh
python3.11 tools/iam/migration/test_import_tool.py
python3.11 tools/iam/migration/import_tool.py dry-run \
  --manifest tools/iam/migration/fixtures/metadata.json \
  --mapping /tmp/ingot-iam-map.json \
  --output /tmp/ingot-iam-dry-run.json
```

