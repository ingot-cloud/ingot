package com.ingot.framework.security.core.userdetails;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.RoleUtil;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * <p>Description  : 自定义User.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/28.</p>
 * <p>Time         : 12:54 PM.</p>
 */
@Slf4j
@Getter
public class InUser extends User implements InUserDetails {
    private static final String N_A = "N/A";

    /**
     * 用户ID
     */
    private final Long id;
    /**
     * 租户ID
     */
    private final Long tenantId;
    /**
     * 登录客户端ID
     */
    private final String clientId;
    /**
     * Token认证类型 {@link TokenAuthTypeEnum}
     */
    private final String tokenAuthType;
    /**
     * 用户类型 {@link UserTypeEnum}
     */
    private final String userType;
    /**
     * 认证上下文元数据（仅登录流程使用，不序列化进 JWT）
     * <p>key 定义见 {@link InUserMetaKeys}，值由 PMS/Member 在
     * {@code UserDetailsResponse.meta} 中填充，后续请求复用 Token 时此字段为 null。</p>
     */
    @Getter(onMethod_ = @JsonIgnore)
    private final Map<String, Object> meta;

    @JsonCreator
    public InUser(Long id,
                  Long tenantId,
                  String clientId,
                  String tokenAuthType,
                  String userType,
                  String username,
                  String password,
                  boolean enabled,
                  boolean accountNonExpired,
                  boolean credentialsNonExpired,
                  boolean accountNonLocked,
                  Collection<? extends GrantedAuthority> authorities,
                  Map<String, Object> meta) {
        super(username, password, enabled,
                accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.tenantId = tenantId;
        this.tokenAuthType = tokenAuthType;
        this.clientId = clientId;
        this.userType = userType;
        this.meta = meta;
    }

    /**
     * 从 {@link #meta} 中按类型读取值，缺省返回 {@code null}。
     * <p>
     * 考虑到 meta 经过 RPC(Feign/Jackson) 反序列化后，{@code LocalDateTime} 会退化为 ISO 字符串、
     * {@code Integer} 可能被识别为 {@code Long} 等情况，这里做了一层类型兼容转换：
     * </p>
     * <ul>
     *   <li>目标类型已匹配：直接返回</li>
     *   <li>{@link LocalDateTime} / {@link LocalDate} / {@link Instant}：支持从 ISO 字符串或 epoch 毫秒解析</li>
     *   <li>数值类（Integer/Long/Double/Float/Short/Byte）：在 {@link Number} 之间互转</li>
     *   <li>{@link Boolean}：支持 {@code "true"/"false"} 字符串</li>
     *   <li>{@link String}：直接 {@link Object#toString()}</li>
     * </ul>
     * 其他类型或转换失败时返回 {@code null}。
     *
     * @param key   参见 {@link InUserMetaKeys}
     * @param clazz 目标类型
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public <T> T getMetaValue(String key, Class<T> clazz) {
        if (meta == null) {
            return null;
        }
        Object value = meta.get(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        try {
            return (T) convert(value, clazz);
        } catch (Exception e) {
            log.warn("[InUser] meta 值类型转换失败, key={}, value={}, targetType={}, reason={}",
                    key, value, clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private static Object convert(Object value, Class<?> clazz) {
        if (clazz == String.class) {
            return value.toString();
        }
        if (clazz == LocalDateTime.class) {
            if (value instanceof CharSequence cs) {
                return LocalDateTime.parse(cs);
            }
            if (value instanceof Number n) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(n.longValue()), ZoneId.systemDefault());
            }
            throw unsupported(value, clazz);
        }
        if (clazz == LocalDate.class) {
            if (value instanceof CharSequence cs) {
                return LocalDate.parse(cs);
            }
            throw unsupported(value, clazz);
        }
        if (clazz == Instant.class) {
            if (value instanceof CharSequence cs) {
                return Instant.parse(cs);
            }
            if (value instanceof Number n) {
                return Instant.ofEpochMilli(n.longValue());
            }
            throw unsupported(value, clazz);
        }
        if (Number.class.isAssignableFrom(clazz)) {
            Number number = toNumber(value);
            if (clazz == Integer.class) return number.intValue();
            if (clazz == Long.class) return number.longValue();
            if (clazz == Double.class) return number.doubleValue();
            if (clazz == Float.class) return number.floatValue();
            if (clazz == Short.class) return number.shortValue();
            if (clazz == Byte.class) return number.byteValue();
            throw unsupported(value, clazz);
        }
        if (clazz == Boolean.class) {
            if (value instanceof CharSequence cs) {
                return Boolean.parseBoolean(cs.toString());
            }
            throw unsupported(value, clazz);
        }
        throw unsupported(value, clazz);
    }

    private static Number toNumber(Object value) {
        if (value instanceof Number n) {
            return n;
        }
        if (value instanceof CharSequence cs) {
            String s = cs.toString();
            if (s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
                return Double.parseDouble(s);
            }
            return Long.parseLong(s);
        }
        throw new IllegalArgumentException("cannot convert to Number: " + value.getClass());
    }

    private static IllegalArgumentException unsupported(Object value, Class<?> clazz) {
        return new IllegalArgumentException(
                "unsupported conversion from " + value.getClass().getName() + " to " + clazz.getName());
    }

    /**
     * 无状态 UserDetails
     *
     * @return {@link InUser}
     */
    public static InUser stateless(Long id, Long tenantId, String clientId,
                                   String tokenAuthType, String userType, String username,
                                   Collection<? extends GrantedAuthority> authorities) {
        return standard(id, tenantId, clientId, tokenAuthType, userType, username, N_A,
                true, true, true, true,
                authorities, null);
    }

    /**
     * 无客户端信息({@link #clientId}, {@link #tokenAuthType})，
     * 如果可以访问的租户列表中存在主要租户，那么将TenantId设置为主要租户
     *
     * @return {@link InUser}
     */
    public static InUser userDetails(Long id, String userType, Long defaultTenant,
                                     String username, String password,
                                     boolean enabled, boolean accountNonExpired,
                                     boolean credentialsNonExpired, boolean accountNonLocked,
                                     Collection<? extends GrantedAuthority> authorities) {
        return standard(id, defaultTenant, N_A, N_A, userType, username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, null);
    }

    public static InUser userDetails(Long id, String userType, Long defaultTenant,
                                     String username, String password,
                                     boolean enabled, boolean accountNonExpired,
                                     boolean credentialsNonExpired, boolean accountNonLocked,
                                     Collection<? extends GrantedAuthority> authorities,
                                     Map<String, Object> meta) {
        return standard(id, defaultTenant, N_A, N_A, userType, username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, meta);
    }

    /**
     * 标准实例化
     *
     * @return {@link InUser}
     */
    public static InUser standard(Long id, Long tenantId, String clientId,
                                  String tokenAuthType, String userType,
                                  String username, String password,
                                  boolean enabled, boolean accountNonExpired,
                                  boolean credentialsNonExpired, boolean accountNonLocked,
                                  Collection<? extends GrantedAuthority> authorities) {
        return new InUser(id, tenantId, clientId, tokenAuthType, userType,
                username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, null);
    }

    public static InUser standard(Long id, Long tenantId, String clientId,
                                  String tokenAuthType, String userType,
                                  String username, String password,
                                  boolean enabled, boolean accountNonExpired,
                                  boolean credentialsNonExpired, boolean accountNonLocked,
                                  Collection<? extends GrantedAuthority> authorities,
                                  Map<String, Object> meta) {
        return new InUser(id, tenantId, clientId, tokenAuthType, userType,
                username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, meta);
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

    /**
     * {@link SecurityAuthContext#getRoles()}
     *
     * @return 角色编码列表
     */
    @JsonIgnore
    public List<String> getRoleCodeList() {
        Collection<? extends GrantedAuthority> authorities = SecurityAuthContext.getAuthentication().getAuthorities();
        return InAuthorityUtils.authorityListToScopes(authorities)
                .stream()
                .filter(RoleUtil::isRoleCode)
                .toList();
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
        private Long tenantId;
        private String clientId;
        private String tokenAuthType;
        private String userType;
        private Map<String, Object> meta;

        private Builder(InUser user) {
            this.password = user.getPassword();
            this.username = user.getUsername();
            this.authorities = user.getAuthorities();
            this.accountNonExpired = user.isAccountNonExpired();
            this.accountNonLocked = user.isAccountNonLocked();
            this.credentialsNonExpired = user.isCredentialsNonExpired();
            this.enabled = user.isEnabled();

            this.id = user.getId();
            this.tenantId = user.getTenantId();
            this.clientId = user.getClientId();
            this.tokenAuthType = user.getTokenAuthType();
            this.userType = user.getUserType();
            this.meta = user.meta;
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

        public Builder meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public InUser build() {
            return InUser.standard(this.id, this.tenantId, this.clientId, this.tokenAuthType,
                    this.userType,
                    this.username, this.password,
                    this.enabled, this.accountNonExpired, this.credentialsNonExpired, this.accountNonLocked,
                    this.authorities, this.meta);
        }
    }
}
