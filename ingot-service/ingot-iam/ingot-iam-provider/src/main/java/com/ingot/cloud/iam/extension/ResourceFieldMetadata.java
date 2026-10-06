package com.ingot.cloud.iam.extension;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.type.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamResourceMapper;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.authorization.ResourceRegistry;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 批量关联资源目录和可信注册能力，不依据操作编码推断资源。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ResourceFieldMetadata {

    private static final TypeReference<List<FieldCapability>> FIELDS = new TypeReference<>() {
    };

    private final IamApplicationMapper applications;

    private final IamResourceMapper resources;

    private final IamActionMapper actions;

    private final ResourceRegistry registry;

    /**
     * 批量返回角色操作实际所属的资源 ID。
     * @param grants 完整授权集合
     * @return 操作对应资源去重集合
     */
    public Set<BigInteger> resourceIds(List<ActionGrant> grants) {
        if (grants.isEmpty())
            return Set.of();
        var ids = grants.stream().map(g -> new BigInteger(g.actionId())).distinct().toList();
        var rows = actions.selectBatchIds(ids);
        if (rows.size() != ids.size())
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        return rows.stream().map(IamActionEntity::getResourceId).collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 一次加载资源和应用，返回已注册平台执行描述；未接入资源不提供字段能力。
     * @param ids 资源 ID
     * @return 可信能力，以资源 ID 索引
     */
    public Map<String, ResourceDescriptor> load(Collection<BigInteger> ids) {
        if (ids.isEmpty())
            return Map.of();
        return describe(resources.selectBatchIds(ids));
    }

    /**
     * 已加载目录资源只批量关联应用，避免目录逐资源查询。
     * @param rows 目录资源
     * @return 可信能力
     */
    public Map<String, ResourceDescriptor> describe(List<IamResourceEntity> rows) {
        if (rows.isEmpty())
            return Map.of();
        Map<BigInteger, IamApplicationEntity> apps = new HashMap<>();
        applications.selectBatchIds(rows.stream().map(IamResourceEntity::getApplicationId).distinct().toList())
            .forEach(a -> apps.put(a.getId(), a));
        Map<String, ResourceDescriptor> result = new LinkedHashMap<>();
        for (var row : rows) {
            var app = apps.get(row.getApplicationId());
            if (app == null || app.getDomain() != AuthorizationDomain.PLATFORM || !Boolean.TRUE.equals(app.getEnabled())
                    || !Boolean.TRUE.equals(row.getEnabled()))
                continue;
            var provider = registry.find(new ResourceKey(app.getDomain(), app.getCode(), row.getCode()));
            if (provider == null)
                continue;
            var descriptor = provider.descriptor();
            Map<String, FieldCapability> catalog = new HashMap<>();
            IamJson.read(row.getFieldCapabilities(), FIELDS).forEach(f -> catalog.put(f.key(), f));
            var fields = descriptor.fields().stream().map(f -> {
                var upper = catalog.get(f.key());
                if (upper == null)
                    return new FieldCapability(f.key(), f.label(), List.of(FieldVisibility.HIDDEN), false, false,
                            false);
                var visible = f.visibilities().stream().filter(upper.visibilities()::contains).toList();
                var baseline = descriptor.defaults().get(f.key()).visibility();
                if (visible.stream().noneMatch(value -> value.ordinal() <= baseline.ordinal()))
                    return new FieldCapability(f.key(), upper.label(), List.of(FieldVisibility.HIDDEN), false, false,
                            false);
                return new FieldCapability(f.key(), upper.label(),
                        visible.isEmpty() ? List.of(FieldVisibility.HIDDEN) : visible,
                        f.editable() && upper.editable() && visible.contains(FieldVisibility.FULL),
                        f.filterable() && upper.filterable(), f.sortable() && upper.sortable());
            }).toList();
            Map<String, FieldAccess> defaults = new LinkedHashMap<>();
            for (var field : fields) {
                var original = descriptor.defaults()
                    .getOrDefault(field.key(), new FieldAccess(FieldVisibility.HIDDEN, false));
                var visibility = field.visibilities().contains(original.visibility()) ? original.visibility()
                        : field.visibilities()
                            .stream()
                            .min(Comparator.comparingInt(Enum::ordinal))
                            .orElse(FieldVisibility.HIDDEN);
                defaults.put(field.key(), new FieldAccess(visibility,
                        visibility == FieldVisibility.FULL && field.editable() && original.editable()));
            }
            result.put(row.getId().toString(),
                    new ResourceDescriptor(descriptor.key(), descriptor.actions(), descriptor.scopeCapabilities(),
                            fields, defaults, descriptor.readAction(), descriptor.hierarchical()));
        }
        return Map.copyOf(result);
    }

    /**
     * 用完整键定位唯一启用平台资源及经目录收紧的描述。
     * @param key 注册资源键
     * @return 资源与执行能力
     */
    public Entry require(ResourceKey key) {
        var app = applications.selectOne(Wrappers.<IamApplicationEntity>lambdaQuery()
            .eq(IamApplicationEntity::getDomain, key.domain())
            .eq(IamApplicationEntity::getCode, key.applicationCode()));
        if (app == null)
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        var row = resources.selectOne(Wrappers.<IamResourceEntity>lambdaQuery()
            .eq(IamResourceEntity::getApplicationId, app.getId())
            .eq(IamResourceEntity::getCode, key.resourceCode()));
        var value = row == null ? null : describe(List.of(row)).get(row.getId().toString());
        if (value == null)
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        return new Entry(row.getId().toString(), value);
    }

    /**
     * <p>
     * 资源真实 ID 及有效执行能力。
     * </p>
     *
     * @param id 资源 ID
     * @param descriptor 执行描述
     * @author jy
     * @since 1.0.0
     */
    public record Entry(String id, ResourceDescriptor descriptor) {
    }

}
