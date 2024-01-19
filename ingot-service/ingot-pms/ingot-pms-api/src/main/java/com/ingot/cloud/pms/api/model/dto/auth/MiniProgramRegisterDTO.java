package com.ingot.cloud.pms.api.model.dto.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : MiniProgramRegisterDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/19.</p>
 * <p>Time         : 11:38.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MiniProgramRegisterDTO extends SocialRegisterDTO {
    private String phoneCode;
}
