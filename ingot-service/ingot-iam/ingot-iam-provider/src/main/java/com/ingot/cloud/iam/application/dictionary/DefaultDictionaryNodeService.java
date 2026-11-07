package com.ingot.cloud.iam.application.dictionary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.infrastructure.persistence.entity.DictionaryEntity;
import com.ingot.cloud.iam.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.iam.application.dictionary.model.DictSortDTO;
import com.ingot.cloud.iam.api.model.enums.DictScopeEnum;
import com.ingot.cloud.iam.api.model.enums.DictTypeEnum;
import com.ingot.cloud.iam.infrastructure.cache.DictionaryCacheNames;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.DictionaryMapper;
import com.ingot.cloud.iam.domain.shared.DictChangedSpringEvent;
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
public class DefaultDictionaryNodeService extends BaseServiceImpl<DictionaryMapper, DictionaryEntity>
        implements DictionaryNodeService {

    private final AssertionChecker assertionChecker;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Cacheable(value = DictionaryCacheNames.ENTITY_CACHE, key = DictionaryCacheNames.LIST_KEY, unless = "#result.isEmpty()")
    public List<DictionaryEntity> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = DictionaryCacheNames.ENTITY_CACHE, key = DictionaryCacheNames.NODE_KEY, unless = "#result == null")
    public DictionaryEntity getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    public List<DictionaryEntity> listByCondition(DictQueryDTO query) {
        return list().stream()
                .filter(dictFilter(query))
                .sorted(comparator())
                .toList();
    }

    @Override
    public IPage<DictionaryEntity> page(IPage<DictionaryEntity> page, DictQueryDTO query) {
        return page(page, Wrappers.<DictionaryEntity>lambdaQuery()
                .eq(query.getType() != null, DictionaryEntity::getType, query.getType())
                .eq(query.getScopeType() != null, DictionaryEntity::getScopeType, query.getScopeType())
                .eq(query.getTenantId() != null, DictionaryEntity::getTenantId, query.getTenantId())
                .eq(query.getAppId() != null, DictionaryEntity::getAppId, query.getAppId())
                .eq(query.getOrgType() != null, DictionaryEntity::getOrgType, query.getOrgType())
                .eq(query.getStatus() != null, DictionaryEntity::getStatus, query.getStatus())
                .eq(StrUtil.isNotEmpty(query.getCode()), DictionaryEntity::getCode, query.getCode())
                .likeRight(StrUtil.isNotEmpty(query.getKeyword()), DictionaryEntity::getName, query.getKeyword())
                .orderByAsc(DictionaryEntity::getSort)
                .orderByAsc(DictionaryEntity::getId));
    }

    @Override
    public List<DictionaryEntity> listItemsByCode(String dictCode, DictQueryDTO query) {
        if (StrUtil.isEmpty(dictCode)) {
            return List.of();
        }

        List<DictionaryEntity> all = list();
        // 找到 code 匹配的字典类型，并按作用域优先级合并：APP > TENANT > PLATFORM
        DictScopeEnum scope = query == null ? DictScopeEnum.PLATFORM
                : (query.getScopeType() == null ? DictScopeEnum.PLATFORM : query.getScopeType());
        Long tenantId = query == null ? null : query.getTenantId();
        Long appId = query == null ? null : query.getAppId();

        DictionaryEntity platformType = findType(all, dictCode, DictScopeEnum.PLATFORM, null, null);
        DictionaryEntity tenantType = scope == DictScopeEnum.TENANT
                ? findType(all, dictCode, DictScopeEnum.TENANT, tenantId, null)
                : null;
        DictionaryEntity appType = scope == DictScopeEnum.APP
                ? findType(all, dictCode, DictScopeEnum.APP, null, appId)
                : null;

        List<DictionaryEntity> items = new ArrayList<>();
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
    @CacheEvict(value = DictionaryCacheNames.ENTITY_CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void create(DictionaryEntity params) {
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

        params.setCreatedAt(DateUtil.utc());
        params.setUpdatedAt(params.getCreatedAt());
        save(params);

        applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, params.getCode()));
    }

    @Override
    @CacheEvict(value = DictionaryCacheNames.ENTITY_CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void update(DictionaryEntity params) {
        DictionaryEntity current = getById(params.getId());
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
            DictionaryEntity probe = new DictionaryEntity();
            probe.setType(effectiveType);
            probe.setCode(params.getCode() != null ? params.getCode() : current.getCode());
            probe.setValue(params.getValue() != null ? params.getValue() : current.getValue());
            probe.setPid(params.getPid() != null ? params.getPid() : current.getPid());
            probe.setScopeType(current.getScopeType());
            probe.setTenantId(current.getTenantId());
            probe.setAppId(current.getAppId());
            validateUniqueness(current.getId(), probe);
        }

        params.setUpdatedAt(DateUtil.utc());
        updateById(params);

        String dictCode = params.getCode() != null ? params.getCode() : current.getCode();
        applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, dictCode));
    }

    @Override
    @CacheEvict(value = DictionaryCacheNames.ENTITY_CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        DictionaryEntity current = getById(id);
        assertionChecker.checkOperation(current != null, "PlatformDictServiceImpl.NonExist");
        assert current != null;
        assertionChecker.checkOperation(!Boolean.TRUE.equals(current.getSystemFlag()),
                "PlatformDictServiceImpl.SystemFlagNotAllow");
        // 叶子节点才可以删除
        boolean leaf = count(Wrappers.<DictionaryEntity>lambdaQuery()
                .eq(DictionaryEntity::getPid, id)) == 0;
        assertionChecker.checkOperation(leaf, "PlatformDictServiceImpl.ExistLeaf");

        removeById(id);

        applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, current.getCode()));
    }

    @Override
    @CacheEvict(value = DictionaryCacheNames.ENTITY_CACHE, allEntries = true)
    public void changeStatus(long id, CommonStatusEnum status) {
        DictionaryEntity current = getById(id);
        assertionChecker.checkOperation(current != null, "PlatformDictServiceImpl.NonExist");
        assert current != null;

        DictionaryEntity update = new DictionaryEntity();
        update.setId(id);
        update.setStatus(status);
        update.setUpdatedAt(DateUtil.utc());
        updateById(update);

        applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, current.getCode()));
    }

    @Override
    @CacheEvict(value = DictionaryCacheNames.ENTITY_CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void batchSort(List<DictSortDTO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (DictSortDTO item : items) {
            DictionaryEntity update = new DictionaryEntity();
            update.setId(item.getId());
            update.setSort(item.getSort());
            update.setUpdatedAt(DateUtil.utc());
            updateById(update);
        }

        applicationEventPublisher.publishEvent(DictChangedSpringEvent.all(this));
    }

    /**
     * 字典查询过滤器（基于已加载的全量列表）
     */
    private static Predicate<DictionaryEntity> dictFilter(DictQueryDTO query) {
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

    private static Comparator<DictionaryEntity> comparator() {
        return Comparator.comparing(DictionaryEntity::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(DictionaryEntity::getId);
    }

    private void validateBase(DictionaryEntity params) {
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
    private void normalizeBeforeSave(DictionaryEntity params) {
        if (params.getType() == DictTypeEnum.ITEM
                && StrUtil.isEmpty(params.getCode())
                && params.getPid() != null
                && params.getPid() != IDConstants.ROOT_TREE_ID) {
            DictionaryEntity parent = getById(params.getPid());
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
    private void validateUniqueness(Long ignoreId, DictionaryEntity params) {
        if (params.getType() == DictTypeEnum.ITEM) {
            assertionChecker.checkOperation(!existsItemValue(ignoreId, params),
                    "PlatformDictServiceImpl.ExistValue");
        } else {
            assertionChecker.checkOperation(!existsTypeCode(ignoreId, params),
                    "PlatformDictServiceImpl.ExistCode");
        }
    }

    private void validateScope(DictionaryEntity params) {
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

    private void validateParent(DictionaryEntity params) {
        if (params.getPid() == null || params.getPid() == IDConstants.ROOT_TREE_ID) {
            // 根节点必须是字典类型
            assertionChecker.checkOperation(params.getType() == DictTypeEnum.TYPE
                            || params.getType() == DictTypeEnum.ITEM,
                    "PlatformDictServiceImpl.TypeNonNull");
            return;
        }
        DictionaryEntity parent = getById(params.getPid());
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
    private boolean existsTypeCode(Long ignoreId, DictionaryEntity params) {
        if (StrUtil.isEmpty(params.getCode())) {
            return false;
        }
        return count(Wrappers.<DictionaryEntity>lambdaQuery()
                .eq(DictionaryEntity::getType, DictTypeEnum.TYPE)
                .eq(DictionaryEntity::getCode, params.getCode())
                .eq(DictionaryEntity::getPid, params.getPid() == null ? IDConstants.ROOT_TREE_ID : params.getPid())
                .eq(params.getScopeType() != null, DictionaryEntity::getScopeType, params.getScopeType())
                .eq(params.getTenantId() != null, DictionaryEntity::getTenantId, params.getTenantId())
                .eq(params.getAppId() != null, DictionaryEntity::getAppId, params.getAppId())
                .ne(ignoreId != null, DictionaryEntity::getId, ignoreId)) > 0;
    }

    /**
     * ITEM 节点 value 冲突检测：同作用域、同父节点（同一字典类型）下 value 唯一。
     * <p>code 不参与 ITEM 的唯一性约束（属于冗余字段，通常等于父 TYPE 的 code）。</p>
     */
    private boolean existsItemValue(Long ignoreId, DictionaryEntity params) {
        if (StrUtil.isEmpty(params.getValue())) {
            return false;
        }
        return count(Wrappers.<DictionaryEntity>lambdaQuery()
                .eq(DictionaryEntity::getType, DictTypeEnum.ITEM)
                .eq(DictionaryEntity::getValue, params.getValue())
                .eq(DictionaryEntity::getPid, params.getPid() == null ? IDConstants.ROOT_TREE_ID : params.getPid())
                .eq(params.getScopeType() != null, DictionaryEntity::getScopeType, params.getScopeType())
                .eq(params.getTenantId() != null, DictionaryEntity::getTenantId, params.getTenantId())
                .eq(params.getAppId() != null, DictionaryEntity::getAppId, params.getAppId())
                .ne(ignoreId != null, DictionaryEntity::getId, ignoreId)) > 0;
    }

    private static DictionaryEntity findType(List<DictionaryEntity> all, String dictCode,
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

    private static List<DictionaryEntity> itemsOf(List<DictionaryEntity> all, Long parentId) {
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
    private static void mergeOverride(List<DictionaryEntity> base, List<DictionaryEntity> override) {
        for (DictionaryEntity item : override) {
            if (item.getValue() != null) {
                base.removeIf(b -> StrUtil.equals(b.getValue(), item.getValue()));
            }
            base.add(item);
        }
    }
}
