package com.ingot.cloud.pms.api.model.vo.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ingot.framework.base.model.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 1:56 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserVo extends BaseVo {
    @JsonIgnore
    private Long version;

    /**
     * 所属租户
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenant_id;

    /**
     * 部门ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dept_id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 姓名
     */
    private String real_name;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮件地址
     */
    private String email;

    /**
     * 状态
     */
    private String status;

    /**
     * 是否已删除
     */
    private Boolean is_deleted;
}
