package com.ingot.framework.commons.model.iam.extension;

import java.util.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;

/**
 * <p>业务资源的完整命名空间，不由操作编码切片推断。</p>
 *
 * @param domain 管理域
 * @param applicationCode 应用编码
 * @param resourceCode 资源编码
 * @author jy
 * @since 1.0.0
 */
public record ResourceKey(@NotNull AuthorizationDomain domain, @NotBlank String applicationCode,
        @NotBlank String resourceCode) {

}
