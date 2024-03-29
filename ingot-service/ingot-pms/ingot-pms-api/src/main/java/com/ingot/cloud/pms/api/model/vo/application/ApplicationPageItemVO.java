package com.ingot.cloud.pms.api.model.vo.application;

import com.ingot.cloud.pms.api.model.domain.SysApplication;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : ApplicationPageItemVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/26.</p>
 * <p>Time         : 16:19.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationPageItemVO extends SysApplication {
    private String menuName;
    private String menuIcon;
}