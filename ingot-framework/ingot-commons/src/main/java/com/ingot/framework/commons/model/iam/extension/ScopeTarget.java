package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.*;

/**
 * <p>由业务实体构造的执行目标，禁止直接信任浏览器归属字段。</p>
 *
 * @param objectId 稳定对象标识
 * @param ownerMemberId 归属成员标识，不是账号标识
 * @param tenantId 实际租户标识，平台为空
 * @param departmentIds 实际归属部门
 * @author jy
 * @since 1.0.0
 */
public record ScopeTarget(String objectId, String ownerMemberId, String tenantId, List<String> departmentIds) {
    /**
     * 复制归属集合。
     */
    public ScopeTarget {
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
    }
}
