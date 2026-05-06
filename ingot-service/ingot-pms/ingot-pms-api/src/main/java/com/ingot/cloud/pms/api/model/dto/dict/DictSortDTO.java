package com.ingot.cloud.pms.api.model.dto.dict;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 字典排序更新 DTO。
 *
 * @author jy
 * @since 2026/4/25
 */
@Data
@Schema(description = "字典排序项")
public class DictSortDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "字典ID")
    private Long id;

    @NotNull
    @Schema(description = "排序权重")
    private Integer sort;
}
