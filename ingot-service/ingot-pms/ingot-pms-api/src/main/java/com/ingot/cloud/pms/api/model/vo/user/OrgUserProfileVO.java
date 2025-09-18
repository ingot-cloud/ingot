package com.ingot.cloud.pms.api.model.vo.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.ingot.framework.commons.model.enums.UserStatusEnum;
import lombok.Data;

/**
 * <p>Description  : 组织用户简介.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/19.</p>
 * <p>Time         : 11:48 AM.</p>
 */
@Data
public class OrgUserProfileVO implements Serializable {
    /**
     * 部门ID
     */
    private List<Long> deptIds;

    /**
     * 角色ID
     */
    private List<Long> roleIds;

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
    private String avatar;

    /**
     * 状态, 0:正常，9:禁用
     */
    private UserStatusEnum status;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;
}
