package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author jymot
 * @since 2023-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TenantTable
@TableName("sys_user_social")
public class SysUserSocial extends BaseModel<SysUserSocial> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 渠道类型
     */
    private SocialTypeEnum type;

    /**
     * 渠道唯一ID
     */
    private String uniqueId;

    /**
     * 绑定时间
     */
    private LocalDateTime bindAt;


}
