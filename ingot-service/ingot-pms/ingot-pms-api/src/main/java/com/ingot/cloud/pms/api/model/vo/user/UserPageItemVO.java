package com.ingot.cloud.pms.api.model.vo.user;

import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.sensitive.Sensitive;
import com.ingot.framework.core.utils.sensitive.SensitiveMode;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

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
    private String avatar;
    /**
     * 状态, 0:正常，9:禁用
     */
    private UserStatusEnum status;
    /**
     * 创建日期
     */
    private LocalDateTime createdAt;
    /**
     * 删除日期
     */
    private LocalDateTime deletedAt;
}
