package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>将操作与其允许范围绑定为完整授权条目，同一条目内的范围按并集合成。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param actionId 操作 ID，使用字符串传输
 * @param scopes 此操作的范围项；空集合表示有操作但无对象范围
 */
@Schema(description = "操作及其范围，不允许脱离操作合并范围")
public record ActionGrant(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String actionId,
        @NotNull @Schema(description = "按并集合成的范围项", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ScopeExpression> scopes) {

    /**
     * 复制范围集合，保持授权条目在校验和消费期间稳定；非法空值交由 Bean Validation 拒绝。
     */
    public ActionGrant {
        if (scopes != null) {
            scopes = Collections.unmodifiableList(new ArrayList<>(scopes));
        }
    }
}
