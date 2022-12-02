package com.ingot.cloud.pms.api.model.domain;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.pms.api.mybatisplus.extension.handlers.IngotOAuth2TypeHandler;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.store.mybatis.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2021-09-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "oauth2_registered_client", autoResultMap = true)
public class Oauth2RegisteredClient extends BaseModel<Oauth2RegisteredClient> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
    private String id;

    /**
     * 客户端ID
     */
    @NotBlank(message = "{Oauth2RegisteredClient.clientId}", groups = Group.Create.class)
    private String clientId;

    /**
     * client id 发布时间
     */
    private LocalDateTime clientIdIssuedAt;

    /**
     * 客户端秘钥
     */
    @NotBlank(message = "{Oauth2RegisteredClient.clientSecret}", groups = Group.Create.class)
    private String clientSecret;

    /**
     * 秘钥过期时间
     */
    private LocalDateTime clientSecretExpiresAt;

    /**
     * 客户端名称
     */
    @NotBlank(message = "{Oauth2RegisteredClient.clientName}", groups = Group.Create.class)
    private String clientName;

    /**
     * 客户端认证方法
     */
    private String clientAuthenticationMethods;

    /**
     * 客户端可以使用的授权类型
     */
    private String authorizationGrantTypes;

    /**
     * 重定向URL
     */
    private String redirectUris;

    /**
     * 客户端的访问范围
     */
    private String scopes;

    /**
     * 客户端设置
     */
    @TableField(typeHandler = IngotOAuth2TypeHandler.class)
    private ClientSettings clientSettings;

    /**
     * token设置
     */
    @TableField(typeHandler = IngotOAuth2TypeHandler.class)
    private TokenSettings tokenSettings;

    /**
     * token认证方法
     */
    private String tokenAuthenticationMethod;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    private LocalDateTime deletedAt;


}
