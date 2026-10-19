package com.ingot.framework.commons.model.iam;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>解释待补资源范围，不使其他有效操作失效。</p>
 * @author jy
 * @since 1.0.0
 * @param resourceId 实际资源ID
 * @param actionIds 受影响操作ID
 * @param parameterKey 可空稳定参数键
 * @param reason 稳定待补原因
 */
@Schema(description = "解释待补资源范围，不使其他有效操作失效")
public record ScopeIssue(
        String resourceId,
        List<String> actionIds,
        String parameterKey,
        ScopeIssueReason reason) {

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public ScopeIssue {
        if (actionIds != null) actionIds = List.copyOf(actionIds);
    }
}
