package com.ingot.cloud.iam.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>创建应用内资源目录项请求，编码在应用内唯一且创建后不可改。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "创建应用资源")
public class AppResourceCreateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "资源编码，应用内稳定唯一")
    private String code;

    @Schema(description = "资源名称")
    private String name;

    @Schema(description = "状态，缺省启用")
    private CommonStatusEnum status;
}
