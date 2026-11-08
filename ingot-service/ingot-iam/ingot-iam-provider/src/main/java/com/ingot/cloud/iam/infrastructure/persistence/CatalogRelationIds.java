package com.ingot.cloud.iam.infrastructure.persistence;

import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.error.BizException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;



/**
 * <p>统一目录关系标识的排序和重复项拒绝规则，供菜单与套餐差量保存复用。</p>
 * @author jy
 * @since 1.0.0
 */
final class CatalogRelationIds {
    private CatalogRelationIds() { }


    static SortedSet<Long> uniqueRelationIds(List<Long> ids) {
        if (ids == null) return new TreeSet<>();
        var result = new TreeSet<>(ids);
        if (result.size() != ids.size()) {
            throw new BizException(
                    IamReasonCode.INVALID_ARGUMENT);
        }
        return result;
    }
}
