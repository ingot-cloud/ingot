package com.ingot.cloud.iam.web.v1.platform;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.api.model.domain.PlatformDict;
import com.ingot.cloud.iam.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.iam.api.model.dto.dict.DictSortDTO;
import com.ingot.cloud.iam.service.biz.BizPlatformDictService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>以平台精确 ACTION 接入既有字典领域实现，不改写字典存储与校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台字典")
@RequestMapping("/v1/platform/dictionaries")
@RequiredArgsConstructor
public class PlatformDictionaryAPI implements RShortcuts {
    private static final String VIEW_TREE = "tree";
    private static final String VIEW_PAGE = "page";
    private static final String VIEW_ITEMS = "items";
    private final IamAccess access;
    private final BizPlatformDictService dictionaries;

    /**
     * 按 view 返回树、分页或按编码取项。
     *
     * @param view tree/page/items
     * @param code items 视图所需编码
     * @param page 分页参数
     * @param query 既有查询条件
     * @return 对应子能力结果
     */
    @Operation(summary = "平台字典")
    @GetMapping
    public R<?> list(@RequestParam(defaultValue = VIEW_PAGE) String view,
                     @RequestParam(required = false) String code,
                     Page<PlatformDict> page, DictQueryDTO query) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_DICTIONARY_READ);
        if (VIEW_TREE.equals(view)) {
            return ok(dictionaries.tree(query));
        }
        if (VIEW_ITEMS.equals(view)) {
            if (code == null || code.isBlank()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            return ok(dictionaries.items(code, query));
        }
        if (!VIEW_PAGE.equals(view)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        IPage<PlatformDict> result = dictionaries.page(page, query);
        return ok(result);
    }

    /**
     * 创建字典节点。
     *
     * @param params 字典节点
     * @return 空成功
     */
    @Operation(summary = "创建平台字典")
    @PostMapping
    public R<Void> create(@Validated(Group.Create.class) @RequestBody PlatformDict params) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_DICTIONARY_CREATE);
        dictionaries.create(params);
        return ok();
    }

    /**
     * 更新字典节点。
     *
     * @param id 字典 ID
     * @param params 字典节点
     * @return 空成功
     */
    @Operation(summary = "更新平台字典")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Validated(Group.Update.class) @RequestBody PlatformDict params) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_DICTIONARY_UPDATE);
        params.setId(id);
        dictionaries.update(params);
        return ok();
    }

    /**
     * 切换字典状态。
     *
     * @param id 字典 ID
     * @param status 目标状态
     * @return 空成功
     */
    @Operation(summary = "切换字典状态")
    @PatchMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable CommonStatusEnum status) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_DICTIONARY_UPDATE);
        dictionaries.changeStatus(id, status);
        return ok();
    }

    /**
     * 批量更新字典排序。
     *
     * @param items 排序项
     * @return 空成功
     */
    @Operation(summary = "批量排序字典")
    @PutMapping("/sort")
    public R<Void> sort(@RequestBody List<DictSortDTO> items) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_DICTIONARY_UPDATE);
        dictionaries.batchSort(items);
        return ok();
    }

    /**
     * 删除字典叶子节点。
     *
     * @param id 字典 ID
     * @return 空成功
     */
    @Operation(summary = "删除平台字典")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_DICTIONARY_DELETE);
        dictionaries.delete(id);
        return ok();
    }
}
