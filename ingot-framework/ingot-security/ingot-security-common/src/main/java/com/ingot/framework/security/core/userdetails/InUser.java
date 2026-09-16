package com.ingot.framework.security.core.userdetails;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
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
 * <p>保存认证用户及单一 IAM 成员身份，供登录、会话持久化和资源服务器恢复使用。</p>
 *
 * @author wangchao
 * @since 1.0.0
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
     * <p>key 定义见 {@link InUserMetaKeys}，值由 IAM/Member 在
     * {@code UserDetailsResponse.meta} 中填充，后续请求复用 Token 时此字段为 null。</p>
     */
    @Getter(onMethod_ = @JsonIgnore)
    private final Map<String, Object> meta;
    /**
     * 当前登录租户下用户所属部门 ID 列表
     */
    private final List<Long> deptIds;
    /**
     * 各可访问租户下用户的部门 ID 列表（key=tenantId）。
     * <p>仅在"未知租户登录 → pre_authorization_code 选定租户"短链路中存在，落 OnlineToken / JWT 时为 null。</p>
     */
    private final Map<Long, List<Long>> tenantDeptIds;

    /** 经 IAM 校验的单一成员身份；非 IAM 用户为空，不能由请求头替换。 */
    private final AuthorizationContext authorizationContext;

    /**
     * 构造不携带 IAM 成员身份的既有用户；不会从账号或租户推断成员身份。
     */
    public InUser(Long id, Long tenantId, String clientId, String tokenAuthType, String userType,
                  String username, String password, boolean enabled, boolean accountNonExpired,
                  boolean credentialsNonExpired, boolean accountNonLocked,
                  Collection<? extends GrantedAuthority> authorities, Map<String, Object> meta,
                  List<Long> deptIds, Map<Long, List<Long>> tenantDeptIds) {
        this(id, tenantId, clientId, tokenAuthType, userType, username, password, enabled,
                accountNonExpired, credentialsNonExpired, accountNonLocked, authorities, meta,
                deptIds, tenantDeptIds, null);
    }

    /**
     * 保存已认证的成员身份，并拒绝账号、租户及部门上下文混用。
     * @param authorizationContext 经身份服务验证的唯一身份，非 IAM 用户可为空
     * @throws IllegalArgumentException 身份与账号、租户或部门上下文不一致
     */
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
                  Map<String, Object> meta,
                  List<Long> deptIds,
                  Map<Long, List<Long>> tenantDeptIds,
                  AuthorizationContext authorizationContext) {
        super(username, password, enabled,
                accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.tenantId = tenantId;
        this.tokenAuthType = tokenAuthType;
        this.clientId = clientId;
        this.userType = userType;
        this.meta = meta;
        this.deptIds = authorizationContext != null && deptIds != null ? List.copyOf(deptIds) : deptIds;
        this.tenantDeptIds = authorizationContext != null && tenantDeptIds != null ? Map.copyOf(tenantDeptIds) : tenantDeptIds;
        if (authorizationContext != null) {
            if (id == null || !Objects.equals(id.toString(), authorizationContext.accountId())
                    || !Objects.equals(tenantId == null ? null : tenantId.toString(), authorizationContext.tenantId())
                    || (tenantDeptIds != null && !tenantDeptIds.isEmpty())
                    || (authorizationContext.domain() == AuthorizationDomain.PLATFORM
                        && deptIds != null && !deptIds.isEmpty())) {
                throw new IllegalArgumentException("IAM 身份与账号、租户或部门上下文不一致");
            }
        }
        this.authorizationContext = authorizationContext;
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
                                   Collection<? extends GrantedAuthority> authorities,
                                   List<Long> deptIds, Map<Long, List<Long>> tenantDeptIds) {
        return standard(id, tenantId, clientId, tokenAuthType, userType, username, N_A,
                true, true, true, true,
                authorities, null, deptIds, tenantDeptIds);
    }

    /**
     * 登录态构造：附带当前租户的部门 ID 列表，以及未知租户场景下的"按租户分组的部门"映射
     */
    public static InUser userDetails(Long id, String userType, Long defaultTenant,
                                     String username, String password,
                                     boolean enabled, boolean accountNonExpired,
                                     boolean credentialsNonExpired, boolean accountNonLocked,
                                     Collection<? extends GrantedAuthority> authorities,
                                     Map<String, Object> meta,
                                     List<Long> deptIds,
                                     Map<Long, List<Long>> tenantDeptIds) {
        return standard(id, defaultTenant, N_A, N_A, userType, username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, meta, deptIds, tenantDeptIds);
    }

    /** 按指定账号状态和权限构造既有用户，不推断 IAM 成员身份。 */
    public static InUser standard(Long id, Long tenantId, String clientId,
                                  String tokenAuthType, String userType,
                                  String username, String password,
                                  boolean enabled, boolean accountNonExpired,
                                  boolean credentialsNonExpired, boolean accountNonLocked,
                                  Collection<? extends GrantedAuthority> authorities,
                                  Map<String, Object> meta,
                                  List<Long> deptIds,
                                  Map<Long, List<Long>> tenantDeptIds) {
        return new InUser(id, tenantId, clientId, tokenAuthType, userType,
                username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, meta, deptIds, tenantDeptIds);
    }

    /** 创建保留账号、成员身份及认证状态的构建器。 */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** {@inheritDoc} */
    @JsonIgnore
    @Override
    public String getPassword() {
        return super.getPassword();
    }

    /** {@inheritDoc} */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return super.isAccountNonExpired();
    }

    /** {@inheritDoc} */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return super.isAccountNonLocked();
    }

    /** {@inheritDoc} */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return super.isCredentialsNonExpired();
    }

    /** {@inheritDoc} */
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

    /**
     * <p>复制认证用户并更新协议属性，构造时检查 IAM 身份绑定不变式。</p>
     * @author jy
     * @since 1.0.0
     */
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
        private List<Long> deptIds;
        private Map<Long, List<Long>> tenantDeptIds;
        private AuthorizationContext authorizationContext;

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
            this.deptIds = user.deptIds;
            this.tenantDeptIds = user.tenantDeptIds;
            this.authorizationContext = user.authorizationContext;
        }

        /** 指定会话租户；IAM 成员上下文不匹配时构造失败。 */
        public Builder tenantId(Long id) {
            this.tenantId = id;
            return this;
        }

        /** 指定本次认证客户端。 */
        public Builder clientId(String id) {
            this.clientId = id;
            return this;
        }

        /** 指定令牌认证类型。 */
        public Builder tokenAuthType(String type) {
            this.tokenAuthType = type;
            return this;
        }

        /** 指定用户体系类型。 */
        public Builder userType(String userType) {
            this.userType = userType;
            return this;
        }

        /** 附带仅用于认证的安全状态提示。 */
        public Builder meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        /** 保存当前身份的部门关系，不接受平台成员的租户部门。 */
        public Builder deptIds(List<Long> deptIds) {
            this.deptIds = deptIds;
            return this;
        }

        /** 保存旧预授权流程的部门映射；IAM 身份禁止非空映射。 */
        public Builder tenantDeptIds(Map<Long, List<Long>> tenantDeptIds) {
            this.tenantDeptIds = tenantDeptIds;
            return this;
        }

        /**
         * 绑定经身份服务校验的成员上下文，构造时再次检查与账号、租户的一致性。
         * @param context 可信身份；非 IAM 用户可为空
         * @return 当前构建器
         */
        public Builder authorizationContext(AuthorizationContext context) {
            this.authorizationContext = context;
            return this;
        }

        /** 生成用户并验证 IAM 身份与账号、租户、部门的一致性。 */
        public InUser build() {
            return new InUser(this.id, this.tenantId, this.clientId, this.tokenAuthType,
                    this.userType,
                    this.username, this.password,
                    this.enabled, this.accountNonExpired, this.credentialsNonExpired, this.accountNonLocked,
                    this.authorities, this.meta, this.deptIds, this.tenantDeptIds, this.authorizationContext);
        }
    }
}
