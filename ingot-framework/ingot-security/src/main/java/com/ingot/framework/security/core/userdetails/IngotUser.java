package com.ingot.framework.security.core.userdetails;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ingot.framework.security.common.constants.TokenAuthType;
import lombok.Getter;
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

    /**
     * 用户ID
     */
    @Getter
    @JsonSerialize(using = ToStringSerializer.class)
    private final Integer id;
    /**
     * 部门ID
     */
    @Getter
    @JsonSerialize(using = ToStringSerializer.class)
    private final Integer deptId;
    /**
     * 租户ID
     */
    @Getter
    @JsonSerialize(using = ToStringSerializer.class)
    private final Integer tenantId;
    /**
     * 登录客户端ID
     */
    @Getter
    private final String clientId;
    /**
     * Token认证类型 {@link TokenAuthType}
     */
    @Getter
    private final String tokenAuthType;

    public IngotUser(Integer id,
                     Integer deptId,
                     Integer tenantId,
                     String clientId,
                     String tokenAuthType,
                     String username,
                     String password,
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
        this.tokenAuthType = tokenAuthType;
        this.clientId = clientId;
    }

    /**
     * 无敏感信息，无权限信息 UserDetails
     *
     * @return {@link IngotUser}
     */
    public static IngotUser simple(Integer id, Integer deptId, Integer tenantId, String clientId,
                                   String tokenAuthType, String username) {
        return stateless(id, deptId, tenantId, clientId,
                tokenAuthType, username, Collections.emptyList());
    }

    /**
     * 无状态 UserDetails
     *
     * @return {@link IngotUser}
     */
    public static IngotUser stateless(Integer id, Integer deptId, Integer tenantId, String clientId,
                                      String tokenAuthType, String username,
                                      Collection<? extends GrantedAuthority> authorities) {
        return standard(id, deptId, tenantId, clientId, tokenAuthType, username, N_A,
                true, true, true, true,
                authorities);
    }

    /**
     * 无客户端信息({@link #clientId}, {@link #tokenAuthType})
     *
     * @return {@link IngotUser}
     */
    public static IngotUser noClientInfo(Integer id, Integer deptId, Integer tenantId,
                                         String username, String password,
                                         boolean enabled, boolean accountNonExpired,
                                         boolean credentialsNonExpired, boolean accountNonLocked,
                                         Collection<? extends GrantedAuthority> authorities) {
        return standard(id, deptId, tenantId, N_A, N_A, username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities);
    }

    /**
     * 填充客户端信息
     *
     * @return {@link IngotUser}
     */
    public IngotUser fillClientInfo(IngotUser current, String clientId, String tokenAuthType) {
        return standard(current.getId(), current.getDeptId(), current.getTenantId(),
                clientId, tokenAuthType, current.getUsername(), current.getPassword(),
                current.isEnabled(), current.isAccountNonExpired(),
                current.isCredentialsNonExpired(), current.isAccountNonLocked(),
                current.getAuthorities());
    }

    /**
     * 标准实例化
     *
     * @return {@link IngotUser}
     */
    public static IngotUser standard(Integer id, Integer deptId, Integer tenantId, String clientId,
                                     String tokenAuthType, String username, String password,
                                     boolean enabled, boolean accountNonExpired,
                                     boolean credentialsNonExpired, boolean accountNonLocked,
                                     Collection<? extends GrantedAuthority> authorities) {
        return new IngotUser(id, deptId, tenantId, clientId, tokenAuthType, username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities);
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
