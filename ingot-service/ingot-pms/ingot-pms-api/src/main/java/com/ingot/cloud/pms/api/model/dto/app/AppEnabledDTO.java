package com.ingot.cloud.pms.api.model.dto.app;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : AppEnabledDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/24.</p>
 * <p>Time         : 15:50.</p>
 */
@Data
public class AppEnabledDTO implements Serializable {
    /**
     * 应用ID
     */
    private Long id;
    /**
     * 是否启用
     */
    private Boolean enabled;
}
