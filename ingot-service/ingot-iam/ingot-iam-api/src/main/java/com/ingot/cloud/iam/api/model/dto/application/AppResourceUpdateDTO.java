package com.ingot.cloud.iam.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>更新应用资源请求，不可修改编码或跨应用迁移。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "更新应用资源")
public class AppResourceUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "资源名称")
    private String name;

    @Schema(description = "状态")
    private CommonStatusEnum status;
}
