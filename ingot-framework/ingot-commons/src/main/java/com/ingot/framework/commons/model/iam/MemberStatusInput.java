package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>暂停或恢复当前域成员资格，移出使用独立命令。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param status 只允许 ACTIVE 或 SUSPENDED
 * @param expectedVersion 预览或读取获得的配置版本；提交时比较并重新鉴权
 */
@Schema(description = "暂停或恢复当前域成员资格，移出使用独立命令")
public record MemberStatusInput(
        @NotNull @Valid @Schema(description = "只允许 ACTIVE 或 SUSPENDED", requiredMode = Schema.RequiredMode.REQUIRED)
        MemberStatus status,
        @NotBlank @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion) {

    /**
     * 移出成员必须使用独立 remove 命令。
     * @return 是否符合结构约束，实体归属仍须服务端校验
     */
    @AssertTrue(message = "移出成员必须使用独立 remove 命令")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isTransitionSupported() {
        return status != MemberStatus.REMOVED;
    }
}
