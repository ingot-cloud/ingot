package com.ingot.framework.security.core.userdetails;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.security.common.constants.TokenAuthType;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p>Description  : IngotUser.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/28.</p>
 * <p>Time         : 12:54 PM.</p>
 */
@Getter
public class IngotUser extends User implements IngotUserDetails {
    private static final String N_A = "N/A";

    /**
     * 用户ID
     */
    private final Long id;
    /**
     * 部门ID
     */
    private final Long deptId;
    /**
     * 租户ID
     */
    private final Long tenantId;
    /**
     * 登录客户端ID
     */
    private final String clientId;
    /**
     * Token认证类型 {@link TokenAuthType}
     */
    private final String tokenAuthType;
    /**
     * 用户类型 {@link com.ingot.framework.security.common.constants.UserType}
     */
    private final String userType;
    /**
     * 可以访问的租户
     */
    private final List<AllowTenantDTO> allows;

    @JsonCreator
    public IngotUser(Long id,
                     Long deptId,
                     Long tenantId,
                     String clientId,
                     String tokenAuthType,
                     String userType,
                     String username,
                     String password,
                     List<AllowTenantDTO> allows,
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
        this.userType = userType;
        this.allows = Collections.unmodifiableList(
                allows != null ? allows : ListUtil.empty());
    }

    /**
     * 无敏感信息，无权限信息 UserDetails
     *
     * @return {@link IngotUser}
     */
    public static IngotUser simple(Long id, Long deptId, Long tenantId, String clientId,
                                   String tokenAuthType, String userType, String username) {
        return stateless(id, deptId, tenantId, clientId,
                tokenAuthType, userType, username, Collections.emptyList());
    }

    /**
     * 无状态 UserDetails
     *
     * @return {@link IngotUser}
     */
    public static IngotUser stateless(Long id, Long deptId, Long tenantId, String clientId,
                                      String tokenAuthType, String userType, String username,
                                      Collection<? extends GrantedAuthority> authorities) {
        return standard(id, deptId, tenantId, clientId, tokenAuthType, username, N_A, userType, null,
                true, true, true, true,
                authorities);
    }

    /**
     * 无客户端信息({@link #clientId}, {@link #tokenAuthType})，
     * 如果可以访问的租户列表中存在主要租户，那么将TenantId设置为主要租户
     *
     * @return {@link IngotUser}
     */
    public static IngotUser userDetails(Long id, Long deptId, String userType,
                                        String username, String password,
                                        List<AllowTenantDTO> allows,
                                        boolean enabled, boolean accountNonExpired,
                                        boolean credentialsNonExpired, boolean accountNonLocked,
                                        Collection<? extends GrantedAuthority> authorities) {
        Long tenantId = CollUtil.emptyIfNull(allows)
                .stream()
                .filter(AllowTenantDTO::getMain)
                .map(AllowTenantDTO::getId)
                .findFirst()
                .orElse(null);
        return standard(id, deptId, tenantId, N_A, N_A, userType, username, password, allows,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities);
    }

    /**
     * 标准实例化
     *
     * @return {@link IngotUser}
     */
    public static IngotUser standard(Long id, Long deptId, Long tenantId, String clientId,
                                     String tokenAuthType, String userType,
                                     String username, String password,
                                     List<AllowTenantDTO> allows,
                                     boolean enabled, boolean accountNonExpired,
                                     boolean credentialsNonExpired, boolean accountNonLocked,
                                     Collection<? extends GrantedAuthority> authorities) {
        return new IngotUser(id, deptId, tenantId, clientId, tokenAuthType, userType,
                username, password, allows,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities);
    }

    public Builder toBuilder() {
        return new Builder(this);
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

    public static class Builder {
        private final String password;
        private final String username;
        private final Collection<GrantedAuthority> authorities;
        private final boolean accountNonExpired;
        private final boolean accountNonLocked;
        private final boolean credentialsNonExpired;
        private final boolean enabled;

        private final Long id;
        private Long deptId;
        private Long tenantId;
        private String clientId;
        private String tokenAuthType;
        private String userType;
        private List<AllowTenantDTO> allows;

        private Builder(IngotUser user) {
            this.password = user.getPassword();
            this.username = user.getUsername();
            this.authorities = user.getAuthorities();
            this.accountNonExpired = user.isAccountNonExpired();
            this.accountNonLocked = user.isAccountNonLocked();
            this.credentialsNonExpired = user.isCredentialsNonExpired();
            this.enabled = user.isEnabled();

            this.id = user.getId();
            this.deptId = user.getDeptId();
            this.tenantId = user.getTenantId();
            this.clientId = user.getClientId();
            this.tokenAuthType = user.getTokenAuthType();
            this.userType = user.getUserType();
            this.allows = user.getAllows();
        }

        public Builder deptId(Long id) {
            this.deptId = id;
            return this;
        }

        public Builder tenantId(Long id) {
            this.tenantId = id;
            return this;
        }

        public Builder clientId(String id) {
            this.clientId = id;
            return this;
        }

        public Builder tokenAuthType(String type) {
            this.tokenAuthType = type;
            return this;
        }

        public Builder userType(String userType) {
            this.userType = userType;
            return this;
        }

        public Builder allows(List<AllowTenantDTO> allows) {
            this.allows = allows;
            return this;
        }

        public IngotUser build() {
            return IngotUser.standard(this.id, this.deptId, this.tenantId, this.clientId, this.tokenAuthType,
                    this.userType,
                    this.username, this.password, this.allows,
                    this.enabled, this.accountNonExpired, this.credentialsNonExpired, this.accountNonLocked,
                    this.authorities);
        }
    }
}
