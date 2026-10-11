package com.ingot.framework.commons.model.iam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p>只更新版本说明，权限内容与版本引用保持不变。</p>
 * @param expectedMetadataVersion 元数据读取版本
 * @param versionName 展示名称，空白回退vN
 * @param remark 备注
 * @author jy
 * @since 1.0.0
 */
public record RoleRevisionMetadataInput(@NotBlank String expectedMetadataVersion,
        @Size(max = 128) String versionName, @Size(max = 512) String remark) { }
