package com.ingot.cloud.pms.api.model.vo.application;

import com.ingot.cloud.pms.api.model.domain.SysApplicationTenant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : ApplicationPageItemVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/23.</p>
 * <p>Time         : 09:40.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationPageItemVO extends SysApplicationTenant {
    private String menuName;
    private String menuIcon;
}
