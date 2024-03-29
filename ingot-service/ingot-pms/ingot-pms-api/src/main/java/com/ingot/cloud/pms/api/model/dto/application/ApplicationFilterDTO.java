package com.ingot.cloud.pms.api.model.dto.application;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : ApplicationFilterDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/23.</p>
 * <p>Time         : 10:14.</p>
 */
@Data
public class ApplicationFilterDTO implements Serializable {
    /**
     * 应用名称
     */
    private String appName;
}
