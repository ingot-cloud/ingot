package com.ingot.cloud.pms.api.model.vo.user;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.ingot.framework.core.model.enums.UserStatusEnum;
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
    private Integer userId;
    /**
     * 租户名称
     */
    private String tenantName;
    /**
     * 部门名称
     */
    private String deptName;
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
    /**
     * 删除日期
     */
    private LocalDateTime deletedAt;
}
