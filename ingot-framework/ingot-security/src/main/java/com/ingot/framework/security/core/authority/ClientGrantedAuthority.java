package com.ingot.framework.security.core.authority;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**
 * <p>Description  : ClientGrantedAuthority.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 1:38 PM.</p>
 */
public class ClientGrantedAuthority implements IngotGrantedAuthority<String> {
    public static final String PREFIX = "CLIENT_";

    private final String clientId;

    public ClientGrantedAuthority(String clientId) {
        Assert.hasText(clientId, "A granted authority textual representation is required");
        this.clientId = clientId;
    }

    public static GrantedAuthority create(String clientId) {
        return new ClientGrantedAuthority(clientId);
    }

    @Override
    public String getAuthority() {
        return PREFIX + this.clientId;
    }

    @Override
    public String extract() {
        return this.clientId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ClientGrantedAuthority) {
            return this.clientId.equals(((ClientGrantedAuthority) obj).clientId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.clientId.hashCode();
    }

    @Override
    public String toString() {
        return this.clientId;
    }

}
