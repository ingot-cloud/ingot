package com.ingot.framework.commons.model.iam;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.commons.jackson.WallClockInstantDeserializer;
import com.ingot.framework.commons.jackson.WallClockInstantSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回服务器解析后的单条开通并集，期限和来源不代表业务授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param applicationId 应用 ID
 * @param applicationName 应用目录名称；应用缺失或名为空时省略
 * @param status 显式开通或停用状态
 * @param source 开通来源
 * @param sourceId 来源记录 ID，可空
 * @param validFrom 开始墙钟，可空
 * @param validUntil 结束墙钟，不包含，可空
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回服务器解析后的单条开通并集，期限和来源不代表业务授权")
public record EntitlementPreviewItem(
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @Schema(description = "应用目录名称；应用缺失或名为空时省略")
        String applicationName,
        @NotNull @Schema(description = "显式开通或停用状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status,
        @NotNull @Schema(description = "开通来源", requiredMode = Schema.RequiredMode.REQUIRED)
        EntitlementSource source,
        @Schema(description = "来源记录 ID，可空")
        String sourceId,
        @JsonSerialize(using = WallClockInstantSerializer.class)
        @JsonDeserialize(using = WallClockInstantDeserializer.class)
        @Schema(description = "开始墙钟，可空")
        Instant validFrom,
        @JsonSerialize(using = WallClockInstantSerializer.class)
        @JsonDeserialize(using = WallClockInstantDeserializer.class)
        @Schema(description = "结束墙钟，不包含，可空")
        Instant validUntil) {
}
