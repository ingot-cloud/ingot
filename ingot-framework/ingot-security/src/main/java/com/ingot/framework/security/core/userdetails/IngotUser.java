package com.ingot.framework.security.core.userdetails;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * <p>Description  : IngotUser.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/28.</p>
 * <p>Time         : 12:54 PM.</p>
 */
public class IngotUser extends User {
    @Getter
    private final Long id;
    @Getter
    private final Long tenantId;
    @Getter
    private final String authType;

    public IngotUser(Long id,
                     Long tenantId,
                     String authType,
                     String username,
                     String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
        this.tenantId = tenantId;
        this.authType = authType;
    }

    public IngotUser(Long id,
                     Long tenantId,
                     String authType,
                     String username,
                     String password,
                     boolean enabled,
                     boolean accountNonExpired,
                     boolean credentialsNonExpired,
                     boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.tenantId = tenantId;
        this.authType = authType;
    }
}