package com.ingot.cloud.iam.interfaces.http.platform;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.application.id.model.IdAllocationInput;
import com.ingot.cloud.iam.application.id.model.IdAllocationRecord;
import com.ingot.cloud.iam.application.id.IdAllocationService;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>以平台精确 ACTION 接入既有发号配置，不改写发号算法。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 发号配置")
@RequestMapping("/v1/platform/id-allocations")
@RequiredArgsConstructor
public class PlatformIdAllocationAPI implements RShortcuts {
    private final IdAllocationService allocations;

    /**
     * 分页查询发号配置。
     *
     * @param page 分页
     * @param condition 查询条件
     * @return 配置页
     */
    @Operation(summary = "发号配置列表")
    @GetMapping
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ID_ALLOCATION_READ)
    public R<?> list(Page<IdAllocationRecord> page, IdAllocationInput condition) {
        return ok(allocations.list(page, condition));
    }

    /**
     * 创建发号配置。
     *
     * @param params 配置
     * @return 空成功
     */
    @Operation(summary = "创建发号配置")
    @PostMapping
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ID_ALLOCATION_CREATE)
    public R<Void> create(@RequestBody IdAllocationInput params) {
        allocations.create(params);
        return ok();
    }

    /**
     * 更新发号配置。
     *
     * @param id 业务标签
     * @param params 配置
     * @return 空成功
     */
    @Operation(summary = "更新发号配置")
    @PutMapping("/{id}")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ID_ALLOCATION_UPDATE)
    public R<Void> update(@PathVariable String id, @RequestBody IdAllocationInput params) {
        allocations.update(id, params);
        return ok();
    }

    /**
     * 删除发号配置。
     *
     * @param id 业务标签
     * @return 空成功
     */
    @Operation(summary = "删除发号配置")
    @DeleteMapping("/{id}")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ID_ALLOCATION_DELETE)
    public R<Void> remove(@PathVariable String id) {
        allocations.remove(id);
        return ok();
    }
}
