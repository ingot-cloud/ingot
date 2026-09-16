package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.account.AccountService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AccountCreateInput;
import com.ingot.framework.commons.model.iam.AccountLockInput;
import com.ingot.framework.commons.model.iam.AccountLookupInput;
import com.ingot.framework.commons.model.iam.AccountRecord;
import com.ingot.framework.commons.model.iam.AccountSecret;
import com.ingot.framework.commons.model.iam.AccountUpdateInput;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.VersionInput;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供平台全局账号查询、创建、资料与安全命令，不返回组织关系。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台账号")
@RequestMapping("/v1/platform/accounts")
@RequiredArgsConstructor
public class PlatformAccountAPI implements RShortcuts {
    private final AccountService accounts;

    /**
     * 分页列出全局账号。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 账号页
     */
    @Operation(summary = "全局账号列表")
    @GetMapping
    public R<PageResponse<ResourceDetail<AccountRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(accounts.list(page, pageSize));
    }

    /**
     * 创建全局账号。
     *
     * @param input 登录名与可选联系方式
     * @return 新账号 ID
     */
    @Operation(summary = "创建全局账号")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody AccountCreateInput input) {
        return ok(accounts.create(input));
    }

    /**
     * 按用途精确查找全局账号。
     *
     * @param input 单一查找条件
     * @return 受限投影
     */
    @Operation(summary = "精确查找全局账号")
    @PostMapping("/lookup")
    public R<ResourceDetail<AccountRecord>> lookup(@Valid @RequestBody AccountLookupInput input) {
        return ok(accounts.lookup(input));
    }

    /**
     * 读取全局账号详情。
     *
     * @param id 账号 ID
     * @return 安全投影
     */
    @Operation(summary = "全局账号详情")
    @GetMapping("/{id}")
    public R<ResourceDetail<AccountRecord>> get(@PathVariable String id) {
        return ok(accounts.get(id));
    }

    /**
     * 更新全局账号联系资料。
     *
     * @param id 账号 ID
     * @param input 联系资料
     * @return 更新后投影
     */
    @Operation(summary = "更新全局账号资料")
    @PatchMapping("/{id}")
    public R<ResourceDetail<AccountRecord>> update(@PathVariable String id,
                                                   @Valid @RequestBody AccountUpdateInput input) {
        return ok(accounts.update(id, input));
    }

    /**
     * 删除未被成员引用的全局账号。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 版本信封
     */
    @Operation(summary = "删除全局账号")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id, @Valid @RequestBody VersionInput input) {
        return ok(accounts.delete(id, input));
    }

    /**
     * 启用全局账号。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 版本信封
     */
    @Operation(summary = "启用全局账号")
    @PostMapping("/{id}/enable")
    public R<CreatedResource> enable(@PathVariable String id, @Valid @RequestBody VersionInput input) {
        return ok(accounts.enable(id, input));
    }

    /**
     * 停用全局账号。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 版本信封
     */
    @Operation(summary = "停用全局账号")
    @PostMapping("/{id}/disable")
    public R<CreatedResource> disable(@PathVariable String id, @Valid @RequestBody VersionInput input) {
        return ok(accounts.disable(id, input));
    }

    /**
     * 锁定全局账号。
     *
     * @param id 账号 ID
     * @param input 原因与期限
     * @return 版本信封
     */
    @Operation(summary = "锁定全局账号")
    @PostMapping("/{id}/lock")
    public R<CreatedResource> lock(@PathVariable String id, @Valid @RequestBody AccountLockInput input) {
        return ok(accounts.lock(id, input));
    }

    /**
     * 解锁全局账号。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 版本信封
     */
    @Operation(summary = "解锁全局账号")
    @PostMapping("/{id}/unlock")
    public R<CreatedResource> unlock(@PathVariable String id, @Valid @RequestBody VersionInput input) {
        return ok(accounts.unlock(id, input));
    }

    /**
     * 重置全局账号密码。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 一次性明文初始密码
     */
    @Operation(summary = "重置全局账号密码")
    @PostMapping("/{id}/reset-password")
    public R<AccountSecret> resetPassword(@PathVariable String id, @Valid @RequestBody VersionInput input) {
        return ok(accounts.resetPassword(id, input));
    }
}
