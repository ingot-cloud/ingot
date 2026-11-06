package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>声明当前域静态组内容，修改后必须重验委派派生授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param name 组名称
 * @param description 可选说明
 * @param selection 当前域成员和部门，创建必填，更新时省略表示保留现有关联
 */
@Schema(description = "声明当前域静态组内容，修改后必须重验委派派生授权")
public record GroupDraft(
        @NotBlank @Schema(description = "组名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Valid @Schema(description = "可选说明")
        String description,
        @Valid @Schema(description = "当前域成员和部门。创建必填，更新时省略表示保留关联，平台组无部门")
        Selection selection) {
}
