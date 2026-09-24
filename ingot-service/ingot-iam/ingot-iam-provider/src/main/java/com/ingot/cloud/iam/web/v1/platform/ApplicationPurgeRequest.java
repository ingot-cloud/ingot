package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.framework.commons.model.iam.ApplicationPurgeInput;
import com.ingot.framework.commons.model.iam.SensitiveConfirmation;
import com.ingot.framework.commons.model.iam.SensitiveConfirmationKind;
import com.ingot.framework.security.crypto.annotation.InDecryptField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>强制清除应用的入站体。commons 契约不能依赖 crypto，在此解开 {@code secret} 字段密文后再转入领域输入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@Schema(description = "强制清除应用及其全部关联")
public class ApplicationPurgeRequest {
    /**
     * 应用读取版本。
     */
    @NotBlank
    @Schema(description = "应用读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedVersion;

    /**
     * 当前账号身份确认。
     */
    @NotNull
    @Valid
    @Schema(description = "当前账号身份确认", requiredMode = Schema.RequiredMode.REQUIRED)
    private Confirmation confirmation;

    /**
     * 转成领域输入；此时 {@code secret} 已是明文。
     *
     * @return 清除命令
     */
    public ApplicationPurgeInput toInput() {
        SensitiveConfirmationKind kind = confirmation == null ? null : confirmation.getKind();
        String secret = confirmation == null ? null : confirmation.getSecret();
        return new ApplicationPurgeInput(expectedVersion, new SensitiveConfirmation(kind, secret));
    }

    /**
     * <p>入站确认；{@code secret} 走 HYBRID 字段解密。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Getter
    @Setter
    @Schema(description = "嵌在敏感写请求体内的身份确认")
    public static class Confirmation {
        /**
         * 确认种类。
         */
        @NotNull
        @Schema(description = "确认种类", requiredMode = Schema.RequiredMode.REQUIRED)
        private SensitiveConfirmationKind kind;

        /**
         * 口令密文；Jackson 按 HYBRID 字段解密为明文后再交给验密。
         */
        @NotBlank
        @InDecryptField
        @Schema(description = "口令明文；传输由 HYBRID 信封保护", requiredMode = Schema.RequiredMode.REQUIRED)
        private String secret;
    }
}
