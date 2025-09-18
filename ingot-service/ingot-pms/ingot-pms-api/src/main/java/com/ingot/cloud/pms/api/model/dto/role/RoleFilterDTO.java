package com.ingot.cloud.pms.api.model.dto.role;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : RoleFilterDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/22.</p>
 * <p>Time         : 09:27.</p>
 */
@Data
public class RoleFilterDTO implements Serializable {
    private String roleName;
    private String roleType;
}
