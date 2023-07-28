package com.ingot.framework.security.oauth2.core.endpoint;

import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * <p>Description  : PreAuthorizationGrantType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 3:29 PM.</p>
 */
public class PreAuthorizationGrantType implements Serializable {
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    public static final PreAuthorizationGrantType PASSWORD = new PreAuthorizationGrantType("password");
    public static final PreAuthorizationGrantType SOCIAL = new PreAuthorizationGrantType("social");

    private final String value;

    public PreAuthorizationGrantType(String value) {
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
        PreAuthorizationGrantType that = (PreAuthorizationGrantType) obj;
        return this.getValue().equals(that.getValue());
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }
}
