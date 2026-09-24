package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * <p>按操作 ID 批量解析名称与归属，空列表返回空结果。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param ids 待解析的操作 ID
 */
@Schema(description = "按操作 ID 批量解析名称与归属，空列表返回空结果")
public record ActionLookupInput(
        @NotNull @Schema(description = "待解析的操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> ids) {

    /**
     * 复制标识列表，避免外部修改改变已经接收的查询。
     */
    public ActionLookupInput {
        if (ids != null) {
            ids = Collections.unmodifiableList(new ArrayList<>(ids));
        }
    }
}
