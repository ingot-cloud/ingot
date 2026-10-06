package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.ingot.framework.commons.model.iam.*;

/**
 * <p>
 * v2只声明资源与操作，不允许替代用户身份。
 * </p>
 *
 * @param resource 服务器接入的资源
 * @param actionCodes 精确操作编码
 * @author jy
 * @since 1.0.0
 */
public record AuthorizationRequest(@NotNull @Valid ResourceKey resource,
        @NotEmpty @Size(max = AuthorizationRequest.MAX_ACTIONS) List<@NotBlank String> actionCodes) {
    /** 同一次资源求值的最大精确操作数。 */
    public static final int MAX_ACTIONS = 100;

    /**
     * 复制操作集合。
     */
    public AuthorizationRequest {
        actionCodes = actionCodes == null ? List.of() : List.copyOf(actionCodes);
    }
}
