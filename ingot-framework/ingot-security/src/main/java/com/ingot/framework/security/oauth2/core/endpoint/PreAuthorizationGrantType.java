package com.ingot.framework.security.oauth2.core.endpoint;

import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>Description  : PreAuthorizationGrantType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 3:29 PM.</p>
 */
public record PreAuthorizationGrantType(String value) implements Serializable {
    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    public static final PreAuthorizationGrantType PASSWORD = new PreAuthorizationGrantType("password");
    public static final PreAuthorizationGrantType SOCIAL = new PreAuthorizationGrantType("social");
    public static final PreAuthorizationGrantType SESSION = new PreAuthorizationGrantType("session");

    public PreAuthorizationGrantType {
        Assert.hasText(value, "value cannot be empty");
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
        return this.value().equals(that.value());
    }

    @Override
    public int hashCode() {
        return this.value().hashCode();
    }
}
