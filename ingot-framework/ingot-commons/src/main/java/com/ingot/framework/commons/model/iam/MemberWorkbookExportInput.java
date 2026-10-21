package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>登记成员更新导出任务；空成员列表表示当前可见的全部未移出成员。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 组织版本
 * @param memberIds 部分成员 ID；空或省略表示全部可见未移出成员
 */
@Schema(description = "登记成员更新导出任务；空成员列表表示当前可见的全部未移出成员")
public record MemberWorkbookExportInput(
        @NotBlank @Schema(description = "组织版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @Schema(description = "部分成员 ID；空或省略表示全部可见未移出成员")
        List<String> memberIds) {

    /**
     * 复制成员 ID 列表。
     */
    public MemberWorkbookExportInput {
        memberIds = memberIds == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(memberIds));
    }
}
