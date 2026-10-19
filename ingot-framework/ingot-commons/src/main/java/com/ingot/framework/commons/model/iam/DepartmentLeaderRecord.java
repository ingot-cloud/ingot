package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回经成员字段策略投影的负责人最小资料，不携带联系方式或权限。</p>
 *
 * @param memberId 本租户成员 ID
 * @param displayName 按成员字段策略投影的姓名，隐藏时省略
 * @param status 当前成员状态，包含已移出的历史成员
 * @param primary 是否为主负责人
 * @param available 成员有效且账号可用
 * @param departments 当前允许披露的任职部门，随候选和已绑定页返回
 * @author jy
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回经成员字段策略投影的负责人最小资料，不携带联系方式或权限。")
public record DepartmentLeaderRecord(
        @NotBlank String memberId,
        String displayName,
        @NotNull MemberStatus status,
        boolean primary,
        boolean available,
        @NotNull List<MemberDepartmentView> departments) {
    /** 兼容不附带任职的既有调用。 */
    public DepartmentLeaderRecord(String memberId, String displayName, MemberStatus status, boolean primary, boolean available) {
        this(memberId, displayName, status, primary, available, List.of());
    }
}
