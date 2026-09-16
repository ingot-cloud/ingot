package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * <p>按单一字段精确查找全局账号，必须声明用途且不返回组织关系。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param purpose 查找用途
 * @param username 登录名精确值，可空
 * @param phone 手机号精确值，可空
 * @param email 邮箱精确值，可空
 */
@Schema(description = "按单一字段精确查找全局账号，必须声明用途且不返回组织关系")
public record AccountLookupInput(
        @NotNull @Schema(description = "查找用途", requiredMode = Schema.RequiredMode.REQUIRED)
        AccountLookupPurpose purpose,
        @Schema(description = "登录名精确值，可空")
        String username,
        @Schema(description = "手机号精确值，可空")
        String phone,
        @Schema(description = "邮箱精确值，可空")
        String email) {

    /**
     * 三个查找字段必须恰好提供一个非空值。
     *
     * @return 是否满足精确查找形状
     */
    @JsonIgnore
    @AssertTrue(message = "精确查找必须且只能提供用户名、手机号或邮箱之一")
    @Schema(hidden = true)
    public boolean isExactlyOneCriterion() {
        int count = 0;
        if (username != null && !username.isBlank()) {
            count++;
        }
        if (phone != null && !phone.isBlank()) {
            count++;
        }
        if (email != null && !email.isBlank()) {
            count++;
        }
        return count == 1;
    }
}
