package com.ingot.cloud.pms.model.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.store.mybatis.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author magician
 * @since 2020-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseModel<SysUser> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 版本号
     */
    private Long version;

    /**
     * 所属租户
     */
    private Long tenantId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

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
     * 状态
     */
    private String status;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    private LocalDateTime deletedAt;


}