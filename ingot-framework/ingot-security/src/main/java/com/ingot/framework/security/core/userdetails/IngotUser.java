package com.ingot.framework.security.core.userdetails;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * <p>Description  : IngotUser.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/28.</p>
 * <p>Time         : 12:54 PM.</p>
 */
public class IngotUser extends User {
    private static final String N_A = "N/A";

    @Getter
    @JsonSerialize(using = ToStringSerializer.class)
    private final Long id;
    @Getter
    @JsonSerialize(using = ToStringSerializer.class)
    private final Long deptId;
    @Getter
    @JsonSerialize(using = ToStringSerializer.class)
    private final Integer tenantId;
    @Getter
    private final String tokenAuthenticationMethod;
    @Setter
    @Getter
    private final String clientId;

    public IngotUser(Long id,
                     Long deptId,
                     Integer tenantId,
                     String tokenAuthenticationMethod,
                     String username,
                     String clientId) {
        this(id, deptId, tenantId, tokenAuthenticationMethod,
                username, clientId, Collections.emptyList());
    }

    public IngotUser(Long id,
                     Long deptId,
                     Integer tenantId,
                     String tokenAuthenticationMethod,
                     String username,
                     String clientId,
                     Collection<? extends GrantedAuthority> authorities) {
        this(id, deptId, tenantId, tokenAuthenticationMethod, username, N_A, clientId,
                true, true, true, true,
                authorities);
    }

    public IngotUser(Long id,
                     Long deptId,
                     Integer tenantId,
                     String tokenAuthenticationMethod,
                     String username,
                     String password,
                     String clientId,
                     boolean enabled,
                     boolean accountNonExpired,
                     boolean credentialsNonExpired,
                     boolean accountNonLocked,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled,
                accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.deptId = deptId;
        this.tenantId = tenantId;
        this.tokenAuthenticationMethod = tokenAuthenticationMethod;
        this.clientId = clientId;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return super.isAccountNonExpired();
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return super.isAccountNonLocked();
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return super.isCredentialsNonExpired();
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }
}
