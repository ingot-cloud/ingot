package com.ingot.cloud.iam.web.v1.platform;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.api.model.domain.BizLeafAlloc;
import com.ingot.cloud.iam.service.domain.BizLeafAllocService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.utils.DateUtil;
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
    private final IamAccess access;
    private final BizLeafAllocService allocations;

    /**
     * 分页查询发号配置。
     *
     * @param page 分页
     * @param condition 查询条件
     * @return 配置页
     */
    @Operation(summary = "发号配置列表")
    @GetMapping
    public R<?> list(Page<BizLeafAlloc> page, BizLeafAlloc condition) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ID_ALLOCATION_READ);
        return ok(allocations.page(page, Wrappers.lambdaQuery(condition)));
    }

    /**
     * 创建发号配置。
     *
     * @param params 配置
     * @return 空成功
     */
    @Operation(summary = "创建发号配置")
    @PostMapping
    public R<Void> create(@RequestBody BizLeafAlloc params) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ID_ALLOCATION_CREATE);
        params.setUpdateTime(DateUtil.now());
        allocations.save(params);
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
    public R<Void> update(@PathVariable String id, @RequestBody BizLeafAlloc params) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ID_ALLOCATION_UPDATE);
        params.setBizTag(id);
        params.setUpdateTime(DateUtil.now());
        allocations.updateById(params);
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
    public R<Void> remove(@PathVariable String id) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ID_ALLOCATION_DELETE);
        allocations.removeById(id);
        return ok();
    }
}
