package com.ingot.cloud.pms.api.model.dto.user;

import java.io.Serializable;

import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.oss.common.OssUrl;
import lombok.Data;

/**
 * <p>Description  : InnerUserDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/4.</p>
 * <p>Time         : 09:26.</p>
 */
@Data
public class InnerUserDTO implements Serializable {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮件地址
     */
    private String email;

    /**
     * 头像
     */
    @OssUrl
    private String avatar;

    /**
     * 状态, 0:正常，9:禁用
     */
    private UserStatusEnum status;
}
