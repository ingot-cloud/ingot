package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.AuthorizationContext;

/**
 * <p>经过管理资格和依据筛选的对象查询，不承载SQL。</p>
 *
 * @param resource 完整资源键
 * @param context 原始可信身份
 * @param purpose 候选用途
 * @param keyword 搜索文字
 * @param ids 少量回显，非关联全集
 * @param allowedIds 额外范围；null无限制，空无对象
 * @param page 从1开始
 * @param pageSize 页大小
 * @param tree 查询树分支
 * @param parentId 父节点
 * @param association 可信关联分页条件
 * @author jy
 * @since 1.0.0
 */
public record ResourceObjectQuery(ResourceKey resource, AuthorizationContext context, ObjectQueryPurpose purpose,
        String keyword, List<String> ids, List<String> allowedIds, int page, int pageSize, boolean tree,
        String parentId, ObjectAssociation association) {
    /**
     * 防御复制，保留null与空范围差别。
     */
    public ResourceObjectQuery {
        ids = ids == null ? List.of() : List.copyOf(ids);
        allowedIds = allowedIds == null ? null : List.copyOf(allowedIds);
    }
}
