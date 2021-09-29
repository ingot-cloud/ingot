package com.ingot.cloud.pms.api.model.domain;

import com.ingot.framework.store.mybatis.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@TableName("oauth2_authorization_consent")
public class Oauth2AuthorizationConsent extends BaseModel<Oauth2AuthorizationConsent> {

    private static final long serialVersionUID = 1L;

    private String registeredClientId;

    private String principalName;

    private String authorities;

    /**
     * 租户ID
     */
    private Integer tenantId;


}
