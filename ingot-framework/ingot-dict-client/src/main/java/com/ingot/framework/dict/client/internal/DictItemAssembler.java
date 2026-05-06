package com.ingot.framework.dict.client.internal;

import java.util.List;
import java.util.Objects;

import com.ingot.cloud.pms.api.model.domain.PlatformDict;
import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.enums.DictScopeEnum;
import com.ingot.cloud.pms.api.model.enums.DictTypeEnum;
import com.ingot.cloud.pms.api.model.vo.dict.DictItemVO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.model.DictQuery;
import com.ingot.framework.dict.client.model.DictScope;

/**
 * PMS DTO/VO/Domain 与字典客户端稳定模型之间的装配器。
 *
 * @author jy
 * @since 2026/4/25
 */
public final class DictItemAssembler {

    private DictItemAssembler() {
    }

    public static DictItem fromVO(DictItemVO source) {
        if (source == null) {
            return null;
        }
        boolean enabled = source.getStatus() == null || source.getStatus() == CommonStatusEnum.ENABLE;
        return DictItem.builder()
                .id(source.getId())
                .pid(source.getPid())
                .code(source.getCode())
                .name(source.getName())
                .value(source.getValue())
                .label(source.getLabel())
                .item(source.getType() == DictTypeEnum.ITEM)
                .scope(toScope(source.getScopeType()))
                .sort(source.getSort())
                .enabled(enabled)
                .remark(source.getRemark())
                .extra(source.getExtra())
                .build();
    }

    public static DictItem fromEntity(PlatformDict source) {
        if (source == null) {
            return null;
        }
        boolean enabled = source.getStatus() == null || source.getStatus() == CommonStatusEnum.ENABLE;
        return DictItem.builder()
                .id(source.getId())
                .pid(source.getPid())
                .code(source.getCode())
                .name(source.getName())
                .value(source.getValue())
                .label(source.getLabel())
                .item(source.getType() == DictTypeEnum.ITEM)
                .scope(toScope(source.getScopeType()))
                .sort(source.getSort())
                .enabled(enabled)
                .remark(source.getRemark())
                .extra(source.getExtra())
                .build();
    }

    public static List<DictItem> fromVOs(List<DictItemVO> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(DictItemAssembler::fromVO).filter(Objects::nonNull).toList();
    }

    public static List<DictItem> fromEntities(List<PlatformDict> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(DictItemAssembler::fromEntity).filter(Objects::nonNull).toList();
    }

    public static DictQueryDTO toQueryDTO(DictQuery query) {
        DictQueryDTO dto = new DictQueryDTO();
        if (query == null) {
            dto.setScopeType(DictScopeEnum.PLATFORM);
            return dto;
        }
        dto.setScopeType(toScopeEnum(query.getScope()));
        dto.setTenantId(query.getTenantId());
        dto.setAppId(query.getAppId());
        if (!query.isIncludeDisabled()) {
            dto.setStatus(CommonStatusEnum.ENABLE);
        }
        return dto;
    }

    private static DictScope toScope(DictScopeEnum source) {
        if (source == null) {
            return DictScope.PLATFORM;
        }
        return switch (source) {
            case PLATFORM -> DictScope.PLATFORM;
            case TENANT -> DictScope.TENANT;
            case APP -> DictScope.APP;
        };
    }

    private static DictScopeEnum toScopeEnum(DictScope scope) {
        if (scope == null) {
            return DictScopeEnum.PLATFORM;
        }
        return switch (scope) {
            case PLATFORM -> DictScopeEnum.PLATFORM;
            case TENANT -> DictScopeEnum.TENANT;
            case APP -> DictScopeEnum.APP;
        };
    }
}
