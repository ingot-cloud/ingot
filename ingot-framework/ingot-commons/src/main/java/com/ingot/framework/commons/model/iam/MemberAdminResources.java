package com.ingot.framework.commons.model.iam;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;

/**
 * <p>Member 平台应用的可信资源键和精确操作定义。</p>
 * @author jy
 * @since 1.0.0
 */
public final class MemberAdminResources {
    /** App 用户管理平台应用编码。 */
    public static final String APPLICATION = "platform-member";
    /** 用户列表资源键。 */
    public static final ResourceKey USER = key("user");
    /** 查看用户列表。 */
    public static final String USER_QUERY = APPLICATION + ":user:read-list";
    /** 查看用户详情。 */
    public static final String USER_DETAIL = APPLICATION + ":user:read-detail";
    /** 新增用户。 */
    public static final String USER_CREATE = APPLICATION + ":user:create";
    /** 编辑用户。 */
    public static final String USER_UPDATE = APPLICATION + ":user:update";
    /** 删除用户。 */
    public static final String USER_DELETE = APPLICATION + ":user:delete";
    /** 重置用户密码。 */
    public static final String USER_RESET_PWD = APPLICATION + ":user:reset-password";
    /** 查看用户角色。 */
    public static final String USER_ROLES_QUERY = APPLICATION + ":user:read-roles";
    /** 配置用户角色。 */
    public static final String USER_ROLES_SET = APPLICATION + ":user:set-roles";
    /** 锁定用户。 */
    public static final String USER_LOCK = APPLICATION + ":user:lock";

    /** 启用 App 用户。 */
    public static final String USER_ENABLE = APPLICATION + ":user:enable";
    /** 停用 App 用户。 */
    public static final String USER_DISABLE = APPLICATION + ":user:disable";
    /** 解锁 App 用户。 */
    public static final String USER_UNLOCK = APPLICATION + ":user:unlock";

    /** 角色资源键。 */
    public static final ResourceKey ROLE = key("role");
    /** 查看角色。 */
    public static final String ROLE_QUERY = APPLICATION + ":role:read";
    /** 新增角色。 */
    public static final String ROLE_CREATE = APPLICATION + ":role:create";
    /** 编辑角色。 */
    public static final String ROLE_UPDATE = APPLICATION + ":role:update";
    /** 删除角色。 */
    public static final String ROLE_DELETE = APPLICATION + ":role:delete";
    /** 分配角色用户。 */
    public static final String ROLE_USER_ASSIGN = APPLICATION + ":role:assign-users";
    /** 查看角色权限。 */
    public static final String ROLE_PERMISSIONS_QUERY = APPLICATION + ":role:read-permissions";
    /** 配置角色权限。 */
    public static final String ROLE_PERMISSIONS_SET = APPLICATION + ":role:set-permissions";

    /** 权限资源键。 */
    public static final ResourceKey PERMISSION = key("permission");
    /** 查看权限。 */
    public static final String PERMISSION_QUERY = APPLICATION + ":permission:read";
    /** 新增权限。 */
    public static final String PERMISSION_CREATE = APPLICATION + ":permission:create";
    /** 编辑权限。 */
    public static final String PERMISSION_UPDATE = APPLICATION + ":permission:update";
    /** 删除权限。 */
    public static final String PERMISSION_DELETE = APPLICATION + ":permission:delete";

    /** 启停 App 角色。 */
    public static final String ROLE_CHANGE_STATUS = APPLICATION + ":role:change-status";
    /** 启停 App 权限。 */
    public static final String PERMISSION_CHANGE_STATUS = APPLICATION + ":permission:change-status";

    private static final List<ResourceDescriptor> DESCRIPTORS = List.of(
            descriptor(USER, USER_QUERY,
                    read(USER_QUERY),
                    read(USER_DETAIL),
                    write(USER_CREATE),
                    write(USER_UPDATE),
                    write(USER_DELETE),
                    write(USER_RESET_PWD),
                    read(USER_ROLES_QUERY),
                    write(USER_ROLES_SET),
                    write(USER_LOCK),
                    write(USER_ENABLE),
                    write(USER_DISABLE),
                    write(USER_UNLOCK)),
            descriptor(ROLE, ROLE_QUERY,
                    read(ROLE_QUERY),
                    write(ROLE_CREATE),
                    write(ROLE_UPDATE),
                    write(ROLE_CHANGE_STATUS),
                    write(ROLE_DELETE),
                    write(ROLE_USER_ASSIGN),
                    read(ROLE_PERMISSIONS_QUERY),
                    write(ROLE_PERMISSIONS_SET)),
            descriptor(PERMISSION, PERMISSION_QUERY,
                    read(PERMISSION_QUERY),
                    write(PERMISSION_CREATE),
                    write(PERMISSION_UPDATE),
                    write(PERMISSION_CHANGE_STATUS),
                    write(PERMISSION_DELETE)));

    private MemberAdminResources() {
    }

    /** 返回与平台目录一致的 ALL 执行描述。 */
    public static List<ResourceDescriptor> descriptors() {
        return DESCRIPTORS;
    }

    private static ResourceKey key(String resource) {
        return new ResourceKey(AuthorizationDomain.PLATFORM, APPLICATION, resource);
    }

    private static ResourceDescriptor descriptor(ResourceKey key, String read, ActionDescriptor... actions) {
        return new ResourceDescriptor(key, List.of(actions), List.of(ScopeKind.ALL),
                List.of(), Map.of(), read, false, false);
    }

    private static ActionDescriptor read(String code) {
        return new ActionDescriptor(code, ExecutionMode.READ_ONLY);
    }

    private static ActionDescriptor write(String code) {
        return new ActionDescriptor(code, ExecutionMode.MUTATING);
    }
}
