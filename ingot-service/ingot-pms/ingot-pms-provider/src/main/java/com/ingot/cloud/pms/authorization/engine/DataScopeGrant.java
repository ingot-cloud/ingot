package com.ingot.cloud.pms.authorization.engine;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import lombok.Builder;
import lombok.Getter;

/**
 * <p>拟授予或已持有的一条资源数据范围规则。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Builder
public class DataScopeGrant {

    /**
     * 功能权限 ID。
     */
    private final long permissionId;

    /**
     * 资源 ID。
     */
    private final long resourceId;

    /**
     * 数据范围类型。
     */
    private final DataScopeTypeEnum scopeType;

    /**
     * CUSTOM 部门 ID；其它类型为空列表。
     */
    private final List<Long> deptIds;

    /**
     * 规范化 CUSTOM 部门列表，避免空指针。
     *
     * @return 部门 ID 列表，永不为 {@code null}
     */
    public List<Long> safeDeptIds() {
        return CollUtil.emptyIfNull(deptIds);
    }
}
