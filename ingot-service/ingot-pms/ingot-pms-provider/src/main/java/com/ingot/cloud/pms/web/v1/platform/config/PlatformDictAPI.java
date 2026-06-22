package com.ingot.cloud.pms.web.v1.platform.config;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformDict;
import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.dto.dict.DictSortDTO;
import com.ingot.cloud.pms.api.model.vo.dict.DictItemVO;
import com.ingot.cloud.pms.api.model.vo.dict.DictTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizPlatformDictService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description : 平台字典管理模块。</p>
 *
 * @author jy
 * @since 2026/4/25
 */
@Slf4j
@RestController
@Tag(description = "PlatformDict", name = "平台字典管理模块")
@RequestMapping(value = "/v1/platform/config/dict")
@RequiredArgsConstructor
public class PlatformDictAPI implements RShortcuts {
    private final BizPlatformDictService platformDictService;

    @AdminOrHasAnyAuthority({"platform:config:dict:query"})
    @GetMapping("/tree")
    @Operation(summary = "字典树", description = "返回字典类型与字典项的树形结构")
    public R<List<DictTreeNodeVO>> tree(DictQueryDTO query) {
        return ok(platformDictService.tree(query));
    }

    @AdminOrHasAnyAuthority({"platform:config:dict:query"})
    @GetMapping("/page")
    @Operation(summary = "字典分页", description = "管理端分页查询字典节点")
    public R<IPage<PlatformDict>> page(Page<PlatformDict> page, DictQueryDTO query) {
        return ok(platformDictService.page(page, query));
    }

    @AdminOrHasAnyAuthority({"platform:config:dict:query"})
    @GetMapping("/items/{code}")
    @Operation(summary = "字典项查询", description = "根据 code 返回作用域生效字典项")
    public R<List<DictItemVO>> items(@PathVariable String code, DictQueryDTO query) {
        return ok(platformDictService.items(code, query));
    }

    @AdminOrHasAnyAuthority({"platform:config:dict:create"})
    @PostMapping
    @Operation(summary = "创建字典", description = "创建字典节点")
    public R<?> create(@Validated(Group.Create.class) @RequestBody PlatformDict params) {
        platformDictService.create(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:dict:update"})
    @PutMapping
    @Operation(summary = "更新字典", description = "更新字典节点")
    public R<?> update(@Validated(Group.Update.class) @RequestBody PlatformDict params) {
        platformDictService.update(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:dict:update"})
    @PatchMapping("/{id}/status/{status}")
    @Operation(summary = "切换字典状态", description = "启用/禁用字典节点")
    public R<?> changeStatus(@PathVariable Long id, @PathVariable CommonStatusEnum status) {
        platformDictService.changeStatus(id, status);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:dict:update"})
    @PutMapping("/sort")
    @Operation(summary = "批量排序", description = "批量更新字典排序")
    public R<?> sort(@RequestBody List<DictSortDTO> items) {
        platformDictService.batchSort(items);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:dict:delete"})
    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典", description = "删除字典节点（叶子节点）")
    public R<?> removeById(@PathVariable Long id) {
        platformDictService.delete(id);
        return ok();
    }
}
