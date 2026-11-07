package com.ingot.cloud.iam.interfaces.http.tenant;

import com.ingot.cloud.iam.application.tenant.model.OwnerTransferInput;
import com.ingot.cloud.iam.application.shared.model.SensitiveConfirmation;
import com.ingot.cloud.iam.domain.shared.SensitiveConfirmationKind;
import com.ingot.framework.security.crypto.annotation.InDecryptField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>转交所有者的入站体。commons 契约不能依赖 crypto，在此解开 {@code secret} 后再转入领域输入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@Schema(description = "在当前组织内原子转交所有者职责。确认必须与转交出现在同一次请求")
public class OwnerTransferRequest {
    /**
     * 组织设置读取版本。
     */
    @NotBlank
    @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedVersion;

    /**
     * 新所有者成员 ID。
     */
    @NotBlank
    @Schema(description = "当前组织内有效的新所有者成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newOwnerMemberId;

    /**
     * 当前账号身份确认。
     */
    @NotNull
    @Valid
    @Schema(description = "当前账号登录密码确认", requiredMode = Schema.RequiredMode.REQUIRED)
    private Confirmation confirmation;

    /**
     * 转成领域输入；此时 {@code secret} 已是明文。
     *
     * @return 转交命令
     */
    public OwnerTransferInput toInput() {
        SensitiveConfirmationKind kind = confirmation == null ? null : confirmation.getKind();
        String secret = confirmation == null ? null : confirmation.getSecret();
        return new OwnerTransferInput(expectedVersion, newOwnerMemberId,
                confirmation == null ? null : new SensitiveConfirmation(kind, secret));
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
