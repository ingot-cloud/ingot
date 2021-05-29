package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.validation.Group;
import com.ingot.framework.store.mybatis.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
@TableName("sys_oauth_client_details")
public class SysOauthClientDetails extends BaseModel<SysOauthClientDetails> {

    private static final long serialVersionUID = 1L;

    @TableId
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    private Long id;

    /**
     * 客户端ID
     */
    @NotBlank(message = "{SysOauthClientDetails.clientId}", groups = Group.Create.class)
    private String clientId;

    /**
     * 客户端秘钥
     */
    @NotBlank(message = "{SysOauthClientDetails.clientSecret}", groups = Group.Create.class)
    private String clientSecret;

    /**
     * 资源ID
     */
    @NotBlank(message = "{SysOauthClientDetails.resourceId}", groups = Group.Create.class)
    private String resourceId;

    /**
     * 授权的资源ID
     */
    private String resourceIds;

    /**
     * 客户端的访问范围，如果为空（默认）的话，那么客户端拥有全部的访问范围
     */
    private String scope;

    /**
     * 客户端可以使用的授权类型
     */
    private String authorizedGrantTypes;

    /**
     * 重定向URL
     */
    private String webServerRedirectUri;

    /**
     * 客户端可以使用的权限
     */
    private String authorities;

    /**
     * 令牌有效时间/秒
     */
    private Integer accessTokenValidity;

    /**
     * 刷新令牌有效时间/秒
     */
    private Integer refreshTokenValidity;

    /**
     * 额外参数
     */
    private String additionalInformation;

    /**
     * 授权码模式是否跳过授权
     */
    private String autoapprove;

    /**
     * 授权类型，默认standard
     */
    private String authType;

    /**
     * client类型
     */
    private String type;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 备注
     */
    private String remark;

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
