package com.ingot.cloud.member.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.model.BaseModel;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Getter
@Setter
@ToString
@TableName("member_role")
public class MemberRole extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 组ID
     */
    private Long pid;

    /**
     * 角色名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 角色编码
     */
    @TableField("`code`")
    private String code;

    /**
     * 角色类型
     */
    @TableField("`type`")
    private String type;

    /**
     * 状态, 0:正常，9:禁用
     */
    @TableField("`status`")
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
