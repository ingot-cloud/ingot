package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformDict;
import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.dto.dict.DictSortDTO;
import com.ingot.cloud.pms.api.model.enums.DictScopeEnum;
import com.ingot.cloud.pms.api.model.enums.DictTypeEnum;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.PlatformDictMapper;
import com.ingot.cloud.pms.service.dict.DictChangedSpringEvent;
import com.ingot.cloud.pms.service.domain.PlatformDictService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 字典服务实现。
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
@RequiredArgsConstructor
public class PlatformDictServiceImpl extends BaseServiceImpl<PlatformDictMapper, PlatformDict>
        implements PlatformDictService {

    private final AssertionChecker assertionChecker;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Cacheable(value = CacheConstants.PLATFORM_DICTS, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<PlatformDict> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = CacheConstants.PLATFORM_DICTS, key = CacheKey.ItemKey, unless = "#result == null")
    public PlatformDict getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    public List<PlatformDict> listByCondition(DictQueryDTO query) {
        return list().stream()
                .filter(dictFilter(query))
                .sorted(comparator())
                .toList();
    }

    @Override
    public IPage<PlatformDict> page(Page<PlatformDict> page, DictQueryDTO query) {
        return page(page, Wrappers.<PlatformDict>lambdaQuery()
                .eq(query.getType() != null, PlatformDict::getType, query.getType())
                .eq(query.getScopeType() != null, PlatformDict::getScopeType, query.getScopeType())
                .eq(query.getTenantId() != null, PlatformDict::getTenantId, query.getTenantId())
                .eq(query.getAppId() != null, PlatformDict::getAppId, query.getAppId())
                .eq(query.getOrgType() != null, PlatformDict::getOrgType, query.getOrgType())
                .eq(query.getStatus() != null, PlatformDict::getStatus, query.getStatus())
                .eq(StrUtil.isNotEmpty(query.getCode()), PlatformDict::getCode, query.getCode())
                .likeRight(StrUtil.isNotEmpty(query.getKeyword()), PlatformDict::getName, query.getKeyword())
                .orderByAsc(PlatformDict::getSort)
                .orderByAsc(PlatformDict::getId));
    }

    @Override
    public List<PlatformDict> listItemsByCode(String dictCode, DictQueryDTO query) {
        if (StrUtil.isEmpty(dictCode)) {
            return List.of();
        }

        List<PlatformDict> all = list();
        // 找到 code 匹配的字典类型，并按作用域优先级合并：APP > TENANT > PLATFORM
        DictScopeEnum scope = query == null ? DictScopeEnum.PLATFORM
                : (query.getScopeType() == null ? DictScopeEnum.PLATFORM : query.getScopeType());
        Long tenantId = query == null ? null : query.getTenantId();
        Long appId = query == null ? null : query.getAppId();

        PlatformDict platformType = findType(all, dictCode, DictScopeEnum.PLATFORM, null, null);
        PlatformDict tenantType = scope == DictScopeEnum.TENANT
                ? findType(all, dictCode, DictScopeEnum.TENANT, tenantId, null)
                : null;
        PlatformDict appType = scope == DictScopeEnum.APP
                ? findType(all, dictCode, DictScopeEnum.APP, null, appId)
                : null;

        List<PlatformDict> items = new ArrayList<>();
        if (platformType != null) {
            items.addAll(itemsOf(all, platformType.getId()));
        }
        if (tenantType != null) {
            mergeOverride(items, itemsOf(all, tenantType.getId()));
        }
        if (appType != null) {
            mergeOverride(items, itemsOf(all, appType.getId()));
        }
        return items.stream()
                .filter(item -> item.getStatus() == null || item.getStatus() == CommonStatusEnum.ENABLE)
                .sorted(comparator())
                .toList();
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_DICTS, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void create(PlatformDict params) {
        validateBase(params);
        validateScope(params);
        validateParent(params);
        normalizeBeforeSave(params);
        validateUniqueness(null, params);

        if (params.getStatus() == null) {
            params.setStatus(CommonStatusEnum.ENABLE);
        }
        if (params.getSort() == null) {
            params.setSort(0);
        }
        if (params.getSystemFlag() == null) {
            params.setSystemFlag(Boolean.FALSE);
        }
        if (params.getScopeType() == null) {
            params.setScopeType(DictScopeEnum.PLATFORM);
        }

        params.setCreatedAt(DateUtil.now());
        params.setUpdatedAt(params.getCreatedAt());
        save(params);

        applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, params.getCode()));
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_DICTS, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void update(PlatformDict params) {
        PlatformDict current = getById(params.getId());
        assertionChecker.checkOperation(current != null, "PlatformDictServiceImpl.NonExist");
        assert current != null;
        // 内置字典禁止修改 code、value 和类型
        if (Boolean.TRUE.equals(current.getSystemFlag())) {
            params.setCode(null);
            params.setValue(null);
            params.setType(null);
            params.setScopeType(null);
            params.setTenantId(null);
            params.setAppId(null);
        }

        // 仅当 code / value 发生变化时再校验冲突
        DictTypeEnum effectiveType = params.getType() != null ? params.getType() : current.getType();
        boolean codeChanged = StrUtil.isNotEmpty(params.getCode())
                && !StrUtil.equals(params.getCode(), current.getCode());
        boolean valueChanged = effectiveType == DictTypeEnum.ITEM
                && params.getValue() != null
                && !StrUtil.equals(params.getValue(), current.getValue());

        if (codeChanged || valueChanged) {
            PlatformDict probe = new PlatformDict();
            probe.setType(effectiveType);
            probe.setCode(params.getCode() != null ? params.getCode() : current.getCode());
            probe.setValue(params.getValue() != null ? params.getValue() : current.getValue());
            probe.setPid(params.getPid() != null ? params.getPid() : current.getPid());
            probe.setScopeType(current.getScopeType());
            probe.setTenantId(current.getTenantId());
            probe.setAppId(current.getAppId());
            validateUniqueness(current.getId(), probe);
        }

        params.setUpdatedAt(DateUtil.now());
        updateById(params);

        String dictCode = params.getCode() != null ? params.getCode() : current.getCode();
        applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, dictCode));
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_DICTS, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        PlatformDict current = getById(id);
        assertionChecker.checkOperation(current != null, "PlatformDictServiceImpl.NonExist");
        assert current != null;
        assertionChecker.checkOperation(!Boolean.TRUE.equals(current.getSystemFlag()),
                "PlatformDictServiceImpl.SystemFlagNotAllow");
        // 叶子节点才可以删除
        boolean leaf = count(Wrappers.<PlatformDict>lambdaQuery()
                .eq(PlatformDict::getPid, id)) == 0;
        assertionChecker.checkOperation(leaf, "PlatformDictServiceImpl.ExistLeaf");

        removeById(id);

        applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, current.getCode()));
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_DICTS, allEntries = true)
    public void changeStatus(long id, CommonStatusEnum status) {
        PlatformDict current = getById(id);
        assertionChecker.checkOperation(current != null, "PlatformDictServiceImpl.NonExist");
        assert current != null;

        PlatformDict update = new PlatformDict();
        update.setId(id);
        update.setStatus(status);
        update.setUpdatedAt(DateUtil.now());
        updateById(update);

        applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, current.getCode()));
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_DICTS, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void batchSort(List<DictSortDTO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (DictSortDTO item : items) {
            PlatformDict update = new PlatformDict();
            update.setId(item.getId());
            update.setSort(item.getSort());
            update.setUpdatedAt(DateUtil.now());
            updateById(update);
        }

        applicationEventPublisher.publishEvent(DictChangedSpringEvent.all(this));
    }

    /**
     * 字典查询过滤器（基于已加载的全量列表）
     */
    private static Predicate<PlatformDict> dictFilter(DictQueryDTO query) {
        return (item) -> {
            if (query == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(query.getCode()) && !StrUtil.equals(item.getCode(), query.getCode())) {
                return false;
            }
            if (StrUtil.isNotEmpty(query.getKeyword()) && !StrUtil.startWith(item.getName(), query.getKeyword())) {
                return false;
            }
            if (query.getType() != null && item.getType() != query.getType()) {
                return false;
            }
            if (query.getScopeType() != null && item.getScopeType() != query.getScopeType()) {
                return false;
            }
            if (query.getTenantId() != null && !query.getTenantId().equals(item.getTenantId())) {
                return false;
            }
            if (query.getAppId() != null && !query.getAppId().equals(item.getAppId())) {
                return false;
            }
            if (query.getOrgType() != null && item.getOrgType() != query.getOrgType()) {
                return false;
            }
            if (query.getStatus() != null && item.getStatus() != query.getStatus()) {
                return false;
            }
            return true;
        };
    }

    private static Comparator<PlatformDict> comparator() {
        return Comparator.comparing(PlatformDict::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PlatformDict::getId);
    }

    private void validateBase(PlatformDict params) {
        assertionChecker.checkOperation(params.getType() != null, "PlatformDictServiceImpl.TypeNonNull");
        if (params.getType() == DictTypeEnum.TYPE) {
            // TYPE 节点：code 必填（业务唯一键）
            assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getCode()),
                    "PlatformDictServiceImpl.CodeNonNull");
        } else if (params.getType() == DictTypeEnum.ITEM) {
            // ITEM 节点：value 必填（业务唯一键）；code 可省略，由系统自动同步父 TYPE 的 code
            assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getValue()),
                    "PlatformDictServiceImpl.ValueNonNull");
        }
    }

    /**
     * 写入前归一化字段：
     * <ul>
     *     <li>ITEM 若未传 code，自动取父 TYPE 的 code，保持单表内 ITEM/TYPE 的 code 视觉一致，
     *         便于直接 SQL 排查时按 code 过滤同一字典所有节点。</li>
     * </ul>
     */
    private void normalizeBeforeSave(PlatformDict params) {
        if (params.getType() == DictTypeEnum.ITEM
                && StrUtil.isEmpty(params.getCode())
                && params.getPid() != null
                && params.getPid() != IDConstants.ROOT_TREE_ID) {
            PlatformDict parent = getById(params.getPid());
            if (parent != null && StrUtil.isNotEmpty(parent.getCode())) {
                params.setCode(parent.getCode());
            }
        }
    }

    /**
     * 唯一性校验：
     * <ul>
     *     <li>TYPE 节点：同作用域、同父节点下 {@code code} 唯一</li>
     *     <li>ITEM 节点：同作用域、同父节点下 {@code value} 唯一（{@code code} 仅冗余字段，不参与唯一性）</li>
     * </ul>
     */
    private void validateUniqueness(Long ignoreId, PlatformDict params) {
        if (params.getType() == DictTypeEnum.ITEM) {
            assertionChecker.checkOperation(!existsItemValue(ignoreId, params),
                    "PlatformDictServiceImpl.ExistValue");
        } else {
            assertionChecker.checkOperation(!existsTypeCode(ignoreId, params),
                    "PlatformDictServiceImpl.ExistCode");
        }
    }

    private void validateScope(PlatformDict params) {
        if (params.getScopeType() == null) {
            params.setScopeType(DictScopeEnum.PLATFORM);
        }
        if (params.getScopeType() == DictScopeEnum.TENANT) {
            assertionChecker.checkOperation(params.getTenantId() != null,
                    "PlatformDictServiceImpl.ScopeMissingTenant");
        }
        if (params.getScopeType() == DictScopeEnum.APP) {
            assertionChecker.checkOperation(params.getAppId() != null,
                    "PlatformDictServiceImpl.ScopeMissingApp");
        }
    }

    private void validateParent(PlatformDict params) {
        if (params.getPid() == null || params.getPid() == IDConstants.ROOT_TREE_ID) {
            // 根节点必须是字典类型
            assertionChecker.checkOperation(params.getType() == DictTypeEnum.TYPE
                            || params.getType() == DictTypeEnum.ITEM,
                    "PlatformDictServiceImpl.TypeNonNull");
            return;
        }
        PlatformDict parent = getById(params.getPid());
        assertionChecker.checkOperation(parent != null, "PlatformDictServiceImpl.ParentNonExist");
        assert parent != null;
        if (params.getType() == DictTypeEnum.ITEM) {
            assertionChecker.checkOperation(parent.getType() == DictTypeEnum.TYPE,
                    "PlatformDictServiceImpl.ItemMustHasParent");
        }
        if (params.getType() == DictTypeEnum.TYPE) {
            assertionChecker.checkOperation(parent.getType() == DictTypeEnum.TYPE,
                    "PlatformDictServiceImpl.ParentMustBeType");
        }
    }

    /**
     * TYPE 节点 code 冲突检测：同作用域、同父节点下 code 唯一。
     */
    private boolean existsTypeCode(Long ignoreId, PlatformDict params) {
        if (StrUtil.isEmpty(params.getCode())) {
            return false;
        }
        return count(Wrappers.<PlatformDict>lambdaQuery()
                .eq(PlatformDict::getType, DictTypeEnum.TYPE)
                .eq(PlatformDict::getCode, params.getCode())
                .eq(PlatformDict::getPid, params.getPid() == null ? IDConstants.ROOT_TREE_ID : params.getPid())
                .eq(params.getScopeType() != null, PlatformDict::getScopeType, params.getScopeType())
                .eq(params.getTenantId() != null, PlatformDict::getTenantId, params.getTenantId())
                .eq(params.getAppId() != null, PlatformDict::getAppId, params.getAppId())
                .ne(ignoreId != null, PlatformDict::getId, ignoreId)) > 0;
    }

    /**
     * ITEM 节点 value 冲突检测：同作用域、同父节点（同一字典类型）下 value 唯一。
     * <p>code 不参与 ITEM 的唯一性约束（属于冗余字段，通常等于父 TYPE 的 code）。</p>
     */
    private boolean existsItemValue(Long ignoreId, PlatformDict params) {
        if (StrUtil.isEmpty(params.getValue())) {
            return false;
        }
        return count(Wrappers.<PlatformDict>lambdaQuery()
                .eq(PlatformDict::getType, DictTypeEnum.ITEM)
                .eq(PlatformDict::getValue, params.getValue())
                .eq(PlatformDict::getPid, params.getPid() == null ? IDConstants.ROOT_TREE_ID : params.getPid())
                .eq(params.getScopeType() != null, PlatformDict::getScopeType, params.getScopeType())
                .eq(params.getTenantId() != null, PlatformDict::getTenantId, params.getTenantId())
                .eq(params.getAppId() != null, PlatformDict::getAppId, params.getAppId())
                .ne(ignoreId != null, PlatformDict::getId, ignoreId)) > 0;
    }

    private static PlatformDict findType(List<PlatformDict> all, String dictCode,
                                         DictScopeEnum scope, Long tenantId, Long appId) {
        return all.stream()
                .filter(item -> item.getType() == DictTypeEnum.TYPE)
                .filter(item -> StrUtil.equals(item.getCode(), dictCode))
                .filter(item -> item.getScopeType() == scope)
                .filter(item -> tenantId == null || tenantId.equals(item.getTenantId()))
                .filter(item -> appId == null || appId.equals(item.getAppId()))
                .findFirst()
                .orElse(null);
    }

    private static List<PlatformDict> itemsOf(List<PlatformDict> all, Long parentId) {
        return all.stream()
                .filter(item -> item.getType() == DictTypeEnum.ITEM)
                .filter(item -> parentId.equals(item.getPid()))
                .toList();
    }

    /**
     * ITEM 跨作用域覆盖：以 value 为业务键合并。
     * <p>租户级 / 应用级 ITEM 用同一 value 即视为覆盖平台级 ITEM，
     * 不再误用 code（ITEM 的 code 是父 TYPE 的冗余字段，不能区分不同 ITEM）。</p>
     */
    private static void mergeOverride(List<PlatformDict> base, List<PlatformDict> override) {
        for (PlatformDict item : override) {
            if (item.getValue() != null) {
                base.removeIf(b -> StrUtil.equals(b.getValue(), item.getValue()));
            }
            base.add(item);
        }
    }
}
