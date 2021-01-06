package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * @since 2020-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_social_details")
public class SysSocialDetails extends BaseModel<SysRoleAuthority> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
    private Long id;

    /**
     * 版本号
     */
    @JsonIgnore
    @Version
    private Long version;

    /**
     * 租户ID
     */
    @JsonIgnoreProperties(allowSetters = true)
    private Integer tenantId;

    /**
     * App ID
     */
    private String appId;

    /**
     * App Secret
     */
    private String appSecret;

    /**
     * 重定向地址
     */
    private String redirectUrl;

    /**
     * 社交名称
     */
    private String name;

    /**
     * 类型
     */
    private String type;

    /**
     * 状态, 0:正常，9:禁用
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
    @TableLogic
    private LocalDateTime deletedAt;


}
