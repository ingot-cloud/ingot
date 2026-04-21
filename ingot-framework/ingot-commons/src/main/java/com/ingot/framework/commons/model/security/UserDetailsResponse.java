package com.ingot.framework.commons.model.security;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.common.TenantMainDTO;
import lombok.Data;

/**
 * <p>Description  : UserDetailsResponse.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:25 下午.</p>
 */
@Data
public class UserDetailsResponse implements Serializable {
    /**
     * 用户类型 {@link UserTypeEnum}
     */
    private String userType;
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 默认登录tenant
     */
    private Long tenant;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 是否启用（true-启用 false-禁用）
     */
    private Boolean enabled;
    /**
     * 是否锁定（true-锁定 false-正常）
     */
    private Boolean locked;
    /**
     * 凭证是否未过期（true-未过期 false-已过期/硬过期）
     * <p>为 null 时等同于 true</p>
     * <p>仅表示"硬过期"，用于阻断登录；宽限期/即将过期属于软提示，由登录后的用户信息接口单独返回</p>
     */
    private Boolean credentialsNonExpired;
    /**
     * 认证上下文元数据（可为 null）
     * <p>由 PMS/Member 填充、随响应经 Feign/Jackson 传到 Auth，用于登录流程的精细化决策，
     * 不序列化进 JWT。</p>
     * <p>已定义的 key 参见
     * {@code com.ingot.framework.security.core.userdetails.InUserMetaKeys}：</p>
     * <ul>
     *   <li>{@code lockedUntil}（ISO-8601 字符串）：临时锁定到期时间，接收端通过
     *       {@code InUser.getMetaValue(LOCKED_UNTIL, LocalDateTime.class)} 还原</li>
     *   <li>{@code failedLoginCount}（Integer）：当前已连续失败次数</li>
     *   <li>{@code maxFailedAttempts}（Integer）：触发自动锁定的阈值</li>
     *   <li>{@code hintAfterAttempts}（Integer）：从第几次失败开始给出详细提示</li>
     * </ul>
     * <p>注意：{@code Map<String, Object>} 经 Jackson 反序列化会丢失具体类型信息
     * （{@code LocalDateTime}→字符串、{@code Integer}→{@code Long} 等），
     * 填充端应尽量使用字符串/基本数值类型，读取端请统一走 {@code InUser#getMetaValue}。</p>
     */
    private Map<String, Object> meta;
    /**
     * 权限列表，包含roleCode以及authorityCode
     */
    private List<String> scopes;
    /**
     * 可以访问的租户列表
     */
    private List<TenantMainDTO> allows;
}
