package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.*;

/**
 * <p>受信任IAM发起的对象查询或存在性检查。</p>
 *
 * @param query 完整查询上下文
 * @param verifyIds 为空时查询候选，否则仅检查存在性
 * @author jy
 * @since 1.0.0
 */
public record ResourceObjectInvocation(ResourceObjectQuery query, List<String> verifyIds) {
    /**
     * 复制存在性检查集合。
     */
    public ResourceObjectInvocation {
        verifyIds = verifyIds == null ? List.of() : List.copyOf(verifyIds);
    }
}
