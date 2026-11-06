# 实施与验收记录

日期：2026-11-03。状态：validating。程序与本地定向验证完成，尚未完成正式发布验收。

用户已明确本地登录暂不可用，真实操作截图后补。本记录区分真实代码测试、受控浏览器预览和未完成的真实账号验收；没有使用真实账号，也没有修改现有账号、权限、数据库或部署服务。

## 实现范围和内容版本

- IAM新增固定tenant/platform两组目录、搜索、文章、原图/缩略图接口；每次访问复用CurrentIdentityService及强制改密门禁，无新增阅读权限码。
- 私有classpath内容包按两域原子加载；缺包/坏包为503，版本不匹配409，身份/域/参数先校验，再读取内容。帮助响应包含private/no-store及nosniff。
- 同域检索支持NFKC、空白分词、权重、分类子树、真实总数、稳定排序及分页；只返回纯文本高亮和命中摘要。
- 42篇正文：common 4、tenant 18、platform 20。目标代码功能账本147项（租户63、平台84），29项主说明绑定，核对26个正式菜单页面及隐藏导入/导出/下载流程。此处表示目标代码范围，不证明相关并行变更已经上线验收。
- 4张原创概念图及4张缩略图，源码和成品位于docs/help/zh-CN。概念图用于解释关系，不能替代真实操作截图。

产品版本：`2026.11-help-preview`。预览包版本如下，不能作为正式发布通过凭证：

| 域 | 文章数（含common） | 运行资产数（含缩略图） | contentVersion |
| --- | --- | --- | --- |
| TENANT | 22 | 2 | e7486142a979391b5b932197f2e9c68cf64da8477de5336152f66de5a8b5238d |
| PLATFORM | 24 | 6 | b617ade37ae67060725cf1e319fa1dde49ff01d8e0d7badd9bff76eed6d5357c |

## 自动化结果

环境：本机项目Gradle 8.14.5；Python 3.11，依赖见tools/help/requirements.txt。所有命令在后端仓根目录运行。

| 验证 | 结果与边界 |
| --- | --- |
| `python3 -m unittest discover -s tools/help -p 'test_*.py' -v` | 6项通过。真实源的确定性摘要/域闭包、断链、稳定锚点、原始HTML、远程图片、伪造媒体、无时区时间、覆盖遗漏，以及preview仍拒绝跨域账本图片引用 |
| provider帮助模块定向Gradle test | 8项通过，编译及预览资源构建成功；JUnit报告位于provider/build/reports/tests/test |
| 默认 `build_help.py` | 按预期失败：缺少实操评审证据；正式发布门禁没有因截图延期关闭 |
| API/CONTENT副本 | 与前端逐字一致 |
| 两前端制品检查 | 370/547个产物逐个扫描，无42篇正文首段、8个PNG文件或其Base64字节；未产出source map；证据在前端evidence/static-artifact-check.json |

Gradle验证命令：

```sh
./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test \
  -PhelpContentPreview=true -PhelpPython=/opt/anaconda3/bin/python3 \
  --tests 'com.ingot.cloud.iam.help.*' --console=plain
```

HelpContentTest使用真实预览包，覆盖域闭包、目录不含全文/索引、检索排序/分类/总数/分页、Unicode高亮与限额，以及第二包破损时不安装半包。

HelpHttpIsolationTest通过MockMvc运行真实控制器、内容服务、响应头过滤器与CurrentIdentityService，受控替代ActiveIdentityService/AuthorizationEvaluator。已验证：

1. 同账号当前租户/平台身份分别可读本端，双向请求另一端点403。
2. 双向猜测另一端文章、原图和缩略图404；另一端唯一关键词搜索total=0；普通不存在ID与跨域ID同一错误码。
3. 正文与图片版本绑定，旧版本409；撤销身份后原图/缩略图再次请求401。
4. 匿名401、强制改密403（包括图片）、身份依赖故障503；伪造域/身份参数、重复版本参数、非法ID及超限页大小400，路径穿越路径不匹配404。

以上不是完整Gateway/BFF、真实Cookie会话、网络设备头或部署认证过滤链验收。

## 浏览器及视觉证据

使用真实双前端生产制品和本机loopback开发夹具验证。夹具身份、BFF及检索响应为演示数据；后端检索和权限结论来自上述Java/HTTP测试。

首页、分类树、图文阅读、章节定位、高亮分页、后退恢复、受保护图片懒加载/原图预览、缩放、Esc及焦点恢复，以及light/dark和375/768/1280/1440宽度的代表性结果，见[前端验收记录](../../../../../../ingot-admin/specs/changes/active/20261103-common-help-center/ACCEPTANCE.md)。

## 尚未完成的验收

- 147项人工实操/评审证据均未填写。126项需配图的功能记录中，106项为PENDING、20项为CONCEPT_ONLY；这些是功能记录数量，不等同于要新增126张图片。
- 真实操作教程截图、逐篇文义/按钮/字段/结果核对，和目标功能制品版本的确认。
- 真实Gateway/BFF双端会话、低权限/暂停成员、退出/切换组织、身份撤销、设备头与实际反代静态路径检查。
- 目标环境后端先发、前端后发，以及旧后端/缺包/滚动混版和成套发布回滚演练。自动化只覆盖错误和恢复逻辑，不声明部署演练完成。
- 用户产品验收后再更新current并归档；当前未创建commit。

开发构建显式使用preview参数；补齐图片、capturedProductVersion、captureStatus=VERIFIED及人工证据后，必须去掉preview参数执行正常发布校验。
