package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.framework.core.model.enums.UserStatusEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/6.</p>
 * <p>Time         : 2:09 下午.</p>
 */
@Data
public class UserDTO implements Serializable {
    /**
     * 用户ID，如果是编辑则不能为空
     */
    private Long id;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 状态, 0:正常，9:禁用
     */
    private UserStatusEnum status;
}
