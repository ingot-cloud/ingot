package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>仅汇总当前可见负责人；受限时不披露隐藏成员或完整人数。</p>
 *
 * @param primaryLeader 当前可见主负责人，不可见或未设置时省略
 * @param visibleCount 当前成员读取范围内可见的负责人数
 * @param restricted 是否存在不可见关系或缺少成员读取权限
 * @author jy
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "仅汇总当前可见负责人；受限时不披露隐藏成员或完整人数。")
public record DepartmentLeaderSummary(
        DepartmentLeaderRecord primaryLeader,
        long visibleCount,
        boolean restricted) {
}
