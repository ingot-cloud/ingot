package com.ingot.cloud.pms.api.model.dto.user;

import java.io.Serializable;

import com.ingot.framework.core.utils.sensitive.Sensitive;
import com.ingot.framework.core.utils.sensitive.SensitiveMode;
import lombok.Data;

/**
 * <p>Description  : UserBaseInfoDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/25.</p>
 * <p>Time         : 8:58 上午.</p>
 */
@Data
public class UserBaseInfoDTO implements Serializable {
    @Sensitive(mode = SensitiveMode.MOBILE_PHONE)
    private String phone;
    @Sensitive(mode = SensitiveMode.EMAIL)
    private String email;
    private String nickname;
    private String avatar;
}
