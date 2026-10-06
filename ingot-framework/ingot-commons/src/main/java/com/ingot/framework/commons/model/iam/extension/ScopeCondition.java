package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.*;

/**
 * <p>一条合取范围；多条条件取并集，各部门集合分别必须命中。</p>
 *
 * @param all 全域但不取消域及租户隔离
 * @param objectIds 指定对象，空表示此维度不额外约束
 * @param ownerMemberId 本人归属约束
 * @param departmentSets 需逐集合命中的部门条件
 * @author jy
 * @since 1.0.0
 */
public record ScopeCondition(boolean all, List<String> objectIds, String ownerMemberId,
        List<List<String>> departmentSets) {
    /**
     * 复制条件，保留交集语义。
     */
    public ScopeCondition {
        objectIds = objectIds == null ? List.of() : List.copyOf(objectIds);
        departmentSets = departmentSets == null ? List.of() : departmentSets.stream().map(List::copyOf).toList();
    }
}
