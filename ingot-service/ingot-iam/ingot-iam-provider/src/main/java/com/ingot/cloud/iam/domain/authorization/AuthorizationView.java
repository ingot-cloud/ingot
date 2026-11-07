package com.ingot.cloud.iam.domain.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * <p>保存一次求值得到的精确操作、范围并集与截止时间。</p>
 *
 * @param actionCodes 有效操作码
 * @param governedCodes 至少来自一条非委派授权的操作码，即操作者的完整治理资格
 * @param scopes 各操作的范围并集；缺省键表示有操作无对象
 * @param platformAdministrator 服务器有效系统直接分配产生的特权
 * @param version 参与求值的版本指纹
 * @param expiresAt 热缓存截止；取热窗口与最近授权/委派/开通边界的较早者
 * @author jy
 * @since 1.0.0
 */
public record AuthorizationView(List<String> actionCodes, List<String> governedCodes,
                                Map<String, ResolvedActionScope> scopes, String version, Instant expiresAt, List<RoleFieldSource> fieldSources, boolean platformAdministrator, boolean tenantOwner) {
    /** 平台旧构造保留；租户拥有者必须由实时身份查询产生。 */
    public AuthorizationView(List<String> actionCodes, List<String> governedCodes, Map<String, ResolvedActionScope> scopes, String version, Instant expiresAt, List<RoleFieldSource> fieldSources, boolean platformAdministrator) {
        this(actionCodes, governedCodes, scopes, version, expiresAt, fieldSources, platformAdministrator, false);
    }
    /** 兼容旧授权快照；新模型不据此扩权。 */
    public AuthorizationView(List<String> actionCodes, List<String> governedCodes,
            Map<String, ResolvedActionScope> scopes, String version, Instant expiresAt) {
        this(actionCodes, governedCodes, scopes, version, expiresAt, List.of(), false);
    }
    /** 普通角色快照不产生系统特权。 */
    public AuthorizationView(List<String> actionCodes, List<String> governedCodes,
            Map<String, ResolvedActionScope> scopes, String version, Instant expiresAt, List<RoleFieldSource> fieldSources) {
        this(actionCodes, governedCodes, scopes, version, expiresAt, fieldSources, false);
    }
    /**
     * 复制操作与范围集合。
     */
    public AuthorizationView {
        fieldSources = fieldSources == null ? List.of() : List.copyOf(fieldSources);
        actionCodes = actionCodes == null ? List.of() : List.copyOf(actionCodes);
        governedCodes = governedCodes == null ? List.of() : List.copyOf(governedCodes);
        scopes = scopes == null ? Map.of() : Map.copyOf(scopes);
    }

    /**
     * 判断快照在给定时刻是否已过期。
     *
     * @param at 判定时刻
     * @return 已到或已过截止时为 {@code true}；无截止的视图视为过期
     */
    public boolean expired(Instant at) {
        return expiresAt == null || !at.isBefore(expiresAt);
    }

    /**
     * 读取指定操作范围；操作不存在时为空。
     *
     * @param actionCode 精确操作码
     * @return 范围；无此操作时为空范围
     */
    public ResolvedActionScope scope(String actionCode) {
        if (actionCode == null || !actionCodes.contains(actionCode)) {
            return ResolvedActionScope.empty();
        }
        ResolvedActionScope resolved = scopes.get(actionCode);
        return resolved == null ? ResolvedActionScope.empty() : resolved;
    }
}
