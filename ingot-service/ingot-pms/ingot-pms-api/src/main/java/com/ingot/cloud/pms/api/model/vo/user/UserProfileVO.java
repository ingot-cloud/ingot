package com.ingot.cloud.pms.api.model.vo.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.ingot.framework.core.model.enums.UserStatusEnum;
import lombok.Data;

/**
 * <p>Description  : UserProfileVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/6/24.</p>
 * <p>Time         : 10:59 上午.</p>
 */
@Data
public class UserProfileVO implements Serializable {
    /**
     * 部门ID
     */
    private Integer deptId;

    /**
     * 角色ID
     */
    private List<Integer> roleIds;

    /**
     * 用户名
     */
    private String username;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮件地址
     */
    private String email;

    /**
     * 状态, 0:正常，9:禁用
     */
    private UserStatusEnum status;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;
}
