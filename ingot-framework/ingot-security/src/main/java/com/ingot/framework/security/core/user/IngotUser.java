package com.ingot.framework.security.core.user;

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
    private String id;
    @Getter
    private String deptId;
    @Getter
    private String tenantId;
    @Getter
    private String authType;

    public IngotUser(String id,
                     String deptId,
                     String tenantId,
                     String authType,
                     String username,
                     String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
        this.deptId = deptId;
        this.tenantId = tenantId;
        this.authType = authType;
    }

    public IngotUser(String id,
                     String deptId,
                     String tenantId,
                     String authType,
                     String username,
                     String password,
                     boolean enabled,
                     boolean accountNonExpired,
                     boolean credentialsNonExpired,
                     boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.deptId = deptId;
        this.tenantId = tenantId;
        this.authType = authType;
    }
}
