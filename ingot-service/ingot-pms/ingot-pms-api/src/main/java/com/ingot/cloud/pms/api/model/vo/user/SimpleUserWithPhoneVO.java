package com.ingot.cloud.pms.api.model.vo.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : SimpleUserWithPhoneVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/26.</p>
 * <p>Time         : 16:18.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SimpleUserWithPhoneVO extends SimpleUserVO {
    private String phone;
}
