package com.ingot.cloud.pms.api.model.dto.menu;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : MenuFilterDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/5/13.</p>
 * <p>Time         : 16:19.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuFilterDTO extends SysMenu {
    /**
     * 组织类型
     */
    private String orgTypeText;
}
