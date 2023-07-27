package com.ingot.framework.security.oauth2.core.endpoint;

import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * <p>Description  : OAuth2PreAuthorizationType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 3:29 PM.</p>
 */
public class OAuth2PreAuthorizationType implements Serializable {
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    public static final OAuth2PreAuthorizationType PASSWORD_CODE = new OAuth2PreAuthorizationType("password_code");
    public static final OAuth2PreAuthorizationType SOCIAL_CODE = new OAuth2PreAuthorizationType("social_code");

    private final String value;

    public OAuth2PreAuthorizationType(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        OAuth2PreAuthorizationType that = (OAuth2PreAuthorizationType) obj;
        return this.getValue().equals(that.getValue());
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }
}
