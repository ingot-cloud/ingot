package com.ingot.framework.dict.client.model;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典查询条件，由调用方显式指定作用域上下文。
 *
 * @author jy
 * @since 2026/4/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 作用域。缺省按 {@link DictScope#PLATFORM} 处理。
     */
    private DictScope scope;

    /**
     * 租户 ID（{@link DictScope#TENANT} 必填）
     */
    private Long tenantId;

    /**
     * 应用 ID（{@link DictScope#APP} 必填）
     */
    private Long appId;

    /**
     * 是否包含禁用项，默认 false
     */
    private boolean includeDisabled;

    public static DictQuery platform() {
        return DictQuery.builder().scope(DictScope.PLATFORM).build();
    }

    public static DictQuery tenant(Long tenantId) {
        return DictQuery.builder().scope(DictScope.TENANT).tenantId(tenantId).build();
    }

    public static DictQuery app(Long appId) {
        return DictQuery.builder().scope(DictScope.APP).appId(appId).build();
    }
}
