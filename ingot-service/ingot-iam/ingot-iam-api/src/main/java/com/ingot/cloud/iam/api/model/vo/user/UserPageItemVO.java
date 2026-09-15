package com.ingot.cloud.iam.api.model.vo.user;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.ingot.framework.core.utils.sensitive.Sensitive;
import com.ingot.framework.core.utils.sensitive.SensitiveMode;
import com.ingot.framework.oss.common.OssUrl;
import lombok.Data;

/**
 * <p>Description  : UserPageItemVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/6.</p>
 * <p>Time         : 4:51 下午.</p>
 */
@Data
public class UserPageItemVO implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;
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
    @Sensitive(mode = SensitiveMode.MOBILE_PHONE)
    private String phone;
    /**
     * 邮件地址
     */
    @Sensitive(mode = SensitiveMode.EMAIL)
    private String email;
    /**
     * 头像
     */
    @OssUrl
    private String avatar;

    /**
     * 是否可用
     */
    private Boolean enabled;

    /**
     * 是否锁定
     */
    private Boolean locked;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;
}
