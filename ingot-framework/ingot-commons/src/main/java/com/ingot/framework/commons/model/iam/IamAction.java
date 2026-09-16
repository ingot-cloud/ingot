package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;

/**
 * <p>标识管理面精确 ACTION，禁止用角色名、通配符或旧权限码代替。</p>
 *
 * <p>操作语义由码末段派生为 {@link IamActionOperation}，类加载时即拒绝未登记的词汇，
 * 授权热路径据此区分只读与改写。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
public enum IamAction {
    /**
     * 契约字面量 {@code iam-tenant:directory:read}。
     */
    TENANT_DIRECTORY_READ("iam-tenant:directory:read"),
    /**
     * 契约字面量 {@code iam-platform:member:read}。
     */
    PLATFORM_MEMBER_READ("iam-platform:member:read"),
    /**
     * 契约字面量 {@code iam-platform:member:create}。
     */
    PLATFORM_MEMBER_CREATE("iam-platform:member:create"),
    /**
     * 契约字面量 {@code iam-platform:member:update}。
     */
    PLATFORM_MEMBER_UPDATE("iam-platform:member:update"),
    /**
     * 暂停或恢复平台成员资格。
     */
    PLATFORM_MEMBER_STATUS("iam-platform:member:status"),
    /**
     * 移出平台成员，不删除账号。
     */
    PLATFORM_MEMBER_REMOVE("iam-platform:member:remove"),
    /**
     * 受限精确查找全局账号。
     */
    PLATFORM_ACCOUNT_LOOKUP("iam-platform:account:lookup"),
    /**
     * 读取全局账号列表或详情。
     */
    PLATFORM_ACCOUNT_READ("iam-platform:account:read"),
    /**
     * 创建全局账号。
     */
    PLATFORM_ACCOUNT_CREATE("iam-platform:account:create"),
    /**
     * 编辑全局账号资料。
     */
    PLATFORM_ACCOUNT_UPDATE("iam-platform:account:update"),
    /**
     * 删除全局账号。
     */
    PLATFORM_ACCOUNT_DELETE("iam-platform:account:delete"),
    /**
     * 启用全局账号。
     */
    PLATFORM_ACCOUNT_ENABLE("iam-platform:account:enable"),
    /**
     * 停用全局账号。
     */
    PLATFORM_ACCOUNT_DISABLE("iam-platform:account:disable"),
    /**
     * 锁定全局账号。
     */
    PLATFORM_ACCOUNT_LOCK("iam-platform:account:lock"),
    /**
     * 解锁全局账号。
     */
    PLATFORM_ACCOUNT_UNLOCK("iam-platform:account:unlock"),
    /**
     * 重置全局账号密码。
     */
    PLATFORM_ACCOUNT_RESET_PASSWORD("iam-platform:account:reset-password"),
    /**
     * 读取平台字典。
     */
    PLATFORM_DICTIONARY_READ("iam-platform:dictionary:read"),
    /**
     * 创建平台字典。
     */
    PLATFORM_DICTIONARY_CREATE("iam-platform:dictionary:create"),
    /**
     * 更新平台字典。
     */
    PLATFORM_DICTIONARY_UPDATE("iam-platform:dictionary:update"),
    /**
     * 删除平台字典。
     */
    PLATFORM_DICTIONARY_DELETE("iam-platform:dictionary:delete"),
    /**
     * 读取发号配置。
     */
    PLATFORM_ID_ALLOCATION_READ("iam-platform:id-allocation:read"),
    /**
     * 创建发号配置。
     */
    PLATFORM_ID_ALLOCATION_CREATE("iam-platform:id-allocation:create"),
    /**
     * 更新发号配置。
     */
    PLATFORM_ID_ALLOCATION_UPDATE("iam-platform:id-allocation:update"),
    /**
     * 删除发号配置。
     */
    PLATFORM_ID_ALLOCATION_DELETE("iam-platform:id-allocation:delete"),
    /**
     * 读取社会化登录配置。
     */
    PLATFORM_SOCIAL_CONFIG_READ("iam-platform:social-config:read"),
    /**
     * 创建社会化登录配置。
     */
    PLATFORM_SOCIAL_CONFIG_CREATE("iam-platform:social-config:create"),
    /**
     * 更新社会化登录配置。
     */
    PLATFORM_SOCIAL_CONFIG_UPDATE("iam-platform:social-config:update"),
    /**
     * 删除社会化登录配置。
     */
    PLATFORM_SOCIAL_CONFIG_DELETE("iam-platform:social-config:delete"),
    /**
     * 读取账号锁定策略。
     */
    PLATFORM_LOCKOUT_POLICY_READ("iam-platform:lockout-policy:read"),
    /**
     * 更新账号锁定策略。
     */
    PLATFORM_LOCKOUT_POLICY_UPDATE("iam-platform:lockout-policy:update"),
    /**
     * 读取凭证策略。
     */
    PLATFORM_CREDENTIAL_POLICY_READ("iam-platform:credential-policy:read"),
    /**
     * 创建凭证策略。
     */
    PLATFORM_CREDENTIAL_POLICY_CREATE("iam-platform:credential-policy:create"),
    /**
     * 更新凭证策略。
     */
    PLATFORM_CREDENTIAL_POLICY_UPDATE("iam-platform:credential-policy:update"),
    /**
     * 删除凭证策略。
     */
    PLATFORM_CREDENTIAL_POLICY_DELETE("iam-platform:credential-policy:delete"),
    /**
     * 读取登录失败防护策略。
     */
    PLATFORM_LOGIN_FAILURE_POLICY_READ("iam-platform:login-failure-policy:read"),
    /**
     * 更新登录失败防护策略。
     */
    PLATFORM_LOGIN_FAILURE_POLICY_UPDATE("iam-platform:login-failure-policy:update"),
    /**
     * 读取在线会话。
     */
    PLATFORM_SESSION_READ("iam-platform:session:read"),
    /**
     * 撤销在线会话。
     */
    PLATFORM_SESSION_REVOKE("iam-platform:session:revoke"),
    /**
     * 读取会话并发策略。
     */
    PLATFORM_SESSION_POLICY_READ("iam-platform:session-policy:read"),
    /**
     * 更新会话并发策略。
     */
    PLATFORM_SESSION_POLICY_UPDATE("iam-platform:session-policy:update"),
    /**
     * 读取安全策略。
     */
    PLATFORM_SECURITY_POLICY_READ("iam-platform:security-policy:read"),
    /**
     * 创建安全策略。
     */
    PLATFORM_SECURITY_POLICY_CREATE("iam-platform:security-policy:create"),
    /**
     * 更新安全策略。
     */
    PLATFORM_SECURITY_POLICY_UPDATE("iam-platform:security-policy:update"),
    /**
     * 删除安全策略。
     */
    PLATFORM_SECURITY_POLICY_DELETE("iam-platform:security-policy:delete"),
    /**
     * 契约字面量 {@code iam-platform:group:read}。
     */
    PLATFORM_GROUP_READ("iam-platform:group:read"),
    /**
     * 契约字面量 {@code iam-platform:group:create}。
     */
    PLATFORM_GROUP_CREATE("iam-platform:group:create"),
    /**
     * 契约字面量 {@code iam-platform:group:update}。
     */
    PLATFORM_GROUP_UPDATE("iam-platform:group:update"),
    /**
     * 契约字面量 {@code iam-platform:group:delete}。
     */
    PLATFORM_GROUP_DELETE("iam-platform:group:delete"),
    /**
     * 契约字面量 {@code iam-platform:group:preview}。
     */
    PLATFORM_GROUP_PREVIEW("iam-platform:group:preview"),
    /**
     * 契约字面量 {@code iam-platform:tenant:read}。
     */
    PLATFORM_TENANT_READ("iam-platform:tenant:read"),
    /**
     * 原子创建组织及最小引用。
     */
    PLATFORM_TENANT_CREATE("iam-platform:tenant:create"),
    /**
     * 预览组织初始化，无写入。
     */
    PLATFORM_TENANT_PREVIEW("iam-platform:tenant:preview"),
    /**
     * 契约字面量 {@code iam-platform:tenant:update}。
     */
    PLATFORM_TENANT_UPDATE("iam-platform:tenant:update"),
    /**
     * 契约字面量 {@code iam-platform:entitlement:read}。
     */
    PLATFORM_ENTITLEMENT_READ("iam-platform:entitlement:read"),
    /**
     * 契约字面量 {@code iam-platform:entitlement:update}。
     */
    PLATFORM_ENTITLEMENT_UPDATE("iam-platform:entitlement:update"),
    /**
     * 契约字面量 {@code iam-platform:entitlement:preview}。
     */
    PLATFORM_ENTITLEMENT_PREVIEW("iam-platform:entitlement:preview"),
    /**
     * 契约字面量 {@code iam-platform:application:read}。
     */
    PLATFORM_APPLICATION_READ("iam-platform:application:read"),
    /**
     * 契约字面量 {@code iam-platform:application:create}。
     */
    PLATFORM_APPLICATION_CREATE("iam-platform:application:create"),
    /**
     * 契约字面量 {@code iam-platform:application:update}。
     */
    PLATFORM_APPLICATION_UPDATE("iam-platform:application:update"),
    /**
     * 契约字面量 {@code iam-platform:application:status}。
     */
    PLATFORM_APPLICATION_STATUS("iam-platform:application:status"),
    /**
     * 契约字面量 {@code iam-platform:application:delete}。
     */
    PLATFORM_APPLICATION_DELETE("iam-platform:application:delete"),
    /**
     * 契约字面量 {@code iam-platform:resource:read}。
     */
    PLATFORM_RESOURCE_READ("iam-platform:resource:read"),
    /**
     * 契约字面量 {@code iam-platform:resource:create}。
     */
    PLATFORM_RESOURCE_CREATE("iam-platform:resource:create"),
    /**
     * 契约字面量 {@code iam-platform:resource:update}。
     */
    PLATFORM_RESOURCE_UPDATE("iam-platform:resource:update"),
    /**
     * 契约字面量 {@code iam-platform:resource:delete}。
     */
    PLATFORM_RESOURCE_DELETE("iam-platform:resource:delete"),
    /**
     * 契约字面量 {@code iam-platform:action:read}。
     */
    PLATFORM_ACTION_READ("iam-platform:action:read"),
    /**
     * 契约字面量 {@code iam-platform:action:create}。
     */
    PLATFORM_ACTION_CREATE("iam-platform:action:create"),
    /**
     * 契约字面量 {@code iam-platform:action:update}。
     */
    PLATFORM_ACTION_UPDATE("iam-platform:action:update"),
    /**
     * 契约字面量 {@code iam-platform:action:status}。
     */
    PLATFORM_ACTION_STATUS("iam-platform:action:status"),
    /**
     * 契约字面量 {@code iam-platform:action:delete}。
     */
    PLATFORM_ACTION_DELETE("iam-platform:action:delete"),
    /**
     * 契约字面量 {@code iam-platform:menu:read}。
     */
    PLATFORM_MENU_READ("iam-platform:menu:read"),
    /**
     * 契约字面量 {@code iam-platform:menu:create}。
     */
    PLATFORM_MENU_CREATE("iam-platform:menu:create"),
    /**
     * 契约字面量 {@code iam-platform:menu:update}。
     */
    PLATFORM_MENU_UPDATE("iam-platform:menu:update"),
    /**
     * 契约字面量 {@code iam-platform:menu:delete}。
     */
    PLATFORM_MENU_DELETE("iam-platform:menu:delete"),
    /**
     * 契约字面量 {@code iam-platform:plan:read}。
     */
    PLATFORM_PLAN_READ("iam-platform:plan:read"),
    /**
     * 契约字面量 {@code iam-platform:plan:create}。
     */
    PLATFORM_PLAN_CREATE("iam-platform:plan:create"),
    /**
     * 契约字面量 {@code iam-platform:plan:update}。
     */
    PLATFORM_PLAN_UPDATE("iam-platform:plan:update"),
    /**
     * 契约字面量 {@code iam-platform:role:read}。
     */
    PLATFORM_ROLE_READ("iam-platform:role:read"),
    /**
     * 契约字面量 {@code iam-platform:role:create}。
     */
    PLATFORM_ROLE_CREATE("iam-platform:role:create"),
    /**
     * 契约字面量 {@code iam-platform:role:status}。
     */
    PLATFORM_ROLE_STATUS("iam-platform:role:status"),
    /**
     * 契约字面量 {@code iam-platform:role:delete}。
     */
    PLATFORM_ROLE_DELETE("iam-platform:role:delete"),
    /**
     * 契约字面量 {@code iam-platform:role:publish}。
     */
    PLATFORM_ROLE_PUBLISH("iam-platform:role:publish"),
    /**
     * 契约字面量 {@code iam-platform:role:preview}。
     */
    PLATFORM_ROLE_PREVIEW("iam-platform:role:preview"),
    /**
     * 契约字面量 {@code iam-platform:shared-role:read}。
     */
    PLATFORM_SHARED_ROLE_READ("iam-platform:shared-role:read"),
    /**
     * 契约字面量 {@code iam-platform:shared-role:create}。
     */
    PLATFORM_SHARED_ROLE_CREATE("iam-platform:shared-role:create"),
    /**
     * 契约字面量 {@code iam-platform:shared-role:status}。
     */
    PLATFORM_SHARED_ROLE_STATUS("iam-platform:shared-role:status"),
    /**
     * 契约字面量 {@code iam-platform:shared-role:delete}。
     */
    PLATFORM_SHARED_ROLE_DELETE("iam-platform:shared-role:delete"),
    /**
     * 契约字面量 {@code iam-platform:shared-role:publish}。
     */
    PLATFORM_SHARED_ROLE_PUBLISH("iam-platform:shared-role:publish"),
    /**
     * 契约字面量 {@code iam-platform:shared-role:preview}。
     */
    PLATFORM_SHARED_ROLE_PREVIEW("iam-platform:shared-role:preview"),
    /**
     * 契约字面量 {@code iam-platform:assignment:read}。
     */
    PLATFORM_ASSIGNMENT_READ("iam-platform:assignment:read"),
    /**
     * 契约字面量 {@code iam-platform:assignment:create}。
     */
    PLATFORM_ASSIGNMENT_CREATE("iam-platform:assignment:create"),
    /**
     * 契约字面量 {@code iam-platform:assignment:update}。
     */
    PLATFORM_ASSIGNMENT_UPDATE("iam-platform:assignment:update"),
    /**
     * 契约字面量 {@code iam-platform:assignment:delete}。
     */
    PLATFORM_ASSIGNMENT_DELETE("iam-platform:assignment:delete"),
    /**
     * 契约字面量 {@code iam-platform:delegation:read}。
     */
    PLATFORM_DELEGATION_READ("iam-platform:delegation:read"),
    /**
     * 契约字面量 {@code iam-platform:delegation:create}。
     */
    PLATFORM_DELEGATION_CREATE("iam-platform:delegation:create"),
    /**
     * 契约字面量 {@code iam-platform:delegation:update}。
     */
    PLATFORM_DELEGATION_UPDATE("iam-platform:delegation:update"),
    /**
     * 契约字面量 {@code iam-platform:delegation:delete}。
     */
    PLATFORM_DELEGATION_DELETE("iam-platform:delegation:delete"),
    /**
     * 契约字面量 {@code iam-platform:delegation:preview}。
     */
    PLATFORM_DELEGATION_PREVIEW("iam-platform:delegation:preview"),
    /**
     * 契约字面量 {@code iam-platform:authorization:diagnose}。
     */
    PLATFORM_AUTHORIZATION_DIAGNOSE("iam-platform:authorization:diagnose"),
    /**
     * 契约字面量 {@code iam-platform:audit:read}。
     */
    PLATFORM_AUDIT_READ("iam-platform:audit:read"),
    /**
     * 契约字面量 {@code iam-tenant:member:read}。
     */
    TENANT_MEMBER_READ("iam-tenant:member:read"),
    /**
     * 契约字面量 {@code iam-tenant:member:create}。
     */
    TENANT_MEMBER_CREATE("iam-tenant:member:create"),
    /**
     * 契约字面量 {@code iam-tenant:member:update}。
     */
    TENANT_MEMBER_UPDATE("iam-tenant:member:update"),
    /**
     * 暂停或恢复租户成员资格。
     */
    TENANT_MEMBER_STATUS("iam-tenant:member:status"),
    /**
     * 移出租户成员，不删除账号。
     */
    TENANT_MEMBER_REMOVE("iam-tenant:member:remove"),
    /**
     * 整体替换租户任职关系。
     */
    TENANT_MEMBER_DEPARTMENTS("iam-tenant:member:departments"),
    /**
     * 契约字面量 {@code iam-tenant:member:export}。
     */
    TENANT_MEMBER_EXPORT("iam-tenant:member:export"),
    /**
     * 契约字面量 {@code iam-tenant:department:read}。
     */
    TENANT_DEPARTMENT_READ("iam-tenant:department:read"),
    /**
     * 契约字面量 {@code iam-tenant:department:create}。
     */
    TENANT_DEPARTMENT_CREATE("iam-tenant:department:create"),
    /**
     * 契约字面量 {@code iam-tenant:department:update}。
     */
    TENANT_DEPARTMENT_UPDATE("iam-tenant:department:update"),
    /**
     * 契约字面量 {@code iam-tenant:department:delete}。
     */
    TENANT_DEPARTMENT_DELETE("iam-tenant:department:delete"),
    /**
     * 契约字面量 {@code iam-tenant:settings:read}。
     */
    TENANT_SETTINGS_READ("iam-tenant:settings:read"),
    /**
     * 契约字面量 {@code iam-tenant:settings:update}。
     */
    TENANT_SETTINGS_UPDATE("iam-tenant:settings:update"),
    /**
     * 转交组织所有者，独立于设置更新。
     */
    TENANT_SETTINGS_OWNER_TRANSFER("iam-tenant:settings:owner-transfer"),
    /**
     * 契约字面量 {@code iam-tenant:application:read}。
     */
    TENANT_APPLICATION_READ("iam-tenant:application:read"),
    /**
     * 契约字面量 {@code iam-tenant:audience:read}。
     */
    TENANT_AUDIENCE_READ("iam-tenant:audience:read"),
    /**
     * 契约字面量 {@code iam-tenant:audience:update}。
     */
    TENANT_AUDIENCE_UPDATE("iam-tenant:audience:update"),
    /**
     * 契约字面量 {@code iam-tenant:group:read}。
     */
    TENANT_GROUP_READ("iam-tenant:group:read"),
    /**
     * 契约字面量 {@code iam-tenant:group:create}。
     */
    TENANT_GROUP_CREATE("iam-tenant:group:create"),
    /**
     * 契约字面量 {@code iam-tenant:group:update}。
     */
    TENANT_GROUP_UPDATE("iam-tenant:group:update"),
    /**
     * 契约字面量 {@code iam-tenant:group:delete}。
     */
    TENANT_GROUP_DELETE("iam-tenant:group:delete"),
    /**
     * 契约字面量 {@code iam-tenant:group:preview}。
     */
    TENANT_GROUP_PREVIEW("iam-tenant:group:preview"),
    /**
     * 契约字面量 {@code iam-tenant:role:read}。
     */
    TENANT_ROLE_READ("iam-tenant:role:read"),
    /**
     * 契约字面量 {@code iam-tenant:role:create}。
     */
    TENANT_ROLE_CREATE("iam-tenant:role:create"),
    /**
     * 契约字面量 {@code iam-tenant:role:status}。
     */
    TENANT_ROLE_STATUS("iam-tenant:role:status"),
    /**
     * 契约字面量 {@code iam-tenant:role:delete}。
     */
    TENANT_ROLE_DELETE("iam-tenant:role:delete"),
    /**
     * 契约字面量 {@code iam-tenant:role:publish}。
     */
    TENANT_ROLE_PUBLISH("iam-tenant:role:publish"),
    /**
     * 契约字面量 {@code iam-tenant:role:preview}。
     */
    TENANT_ROLE_PREVIEW("iam-tenant:role:preview"),
    /**
     * 契约字面量 {@code iam-tenant:role:upgrade}。
     */
    TENANT_ROLE_UPGRADE("iam-tenant:role:upgrade"),
    /**
     * 契约字面量 {@code iam-tenant:assignment:read}。
     */
    TENANT_ASSIGNMENT_READ("iam-tenant:assignment:read"),
    /**
     * 契约字面量 {@code iam-tenant:assignment:create}。
     */
    TENANT_ASSIGNMENT_CREATE("iam-tenant:assignment:create"),
    /**
     * 契约字面量 {@code iam-tenant:assignment:update}。
     */
    TENANT_ASSIGNMENT_UPDATE("iam-tenant:assignment:update"),
    /**
     * 契约字面量 {@code iam-tenant:assignment:delete}。
     */
    TENANT_ASSIGNMENT_DELETE("iam-tenant:assignment:delete"),
    /**
     * 契约字面量 {@code iam-tenant:directory-policy:read}。
     */
    TENANT_DIRECTORY_POLICY_READ("iam-tenant:directory-policy:read"),
    /**
     * 契约字面量 {@code iam-tenant:directory-policy:update}。
     */
    TENANT_DIRECTORY_POLICY_UPDATE("iam-tenant:directory-policy:update"),
    /**
     * 契约字面量 {@code iam-tenant:field-policy:read}。
     */
    TENANT_FIELD_POLICY_READ("iam-tenant:field-policy:read"),
    /**
     * 契约字面量 {@code iam-tenant:field-policy:update}。
     */
    TENANT_FIELD_POLICY_UPDATE("iam-tenant:field-policy:update"),
    /**
     * 契约字面量 {@code iam-tenant:policy:preview}。
     */
    TENANT_POLICY_PREVIEW("iam-tenant:policy:preview"),
    /**
     * 契约字面量 {@code iam-tenant:authorization:diagnose}。
     */
    TENANT_AUTHORIZATION_DIAGNOSE("iam-tenant:authorization:diagnose"),
    /**
     * 契约字面量 {@code iam-tenant:audit:read}。
     */
    TENANT_AUDIT_READ("iam-tenant:audit:read"),
    /**
     * 契约字面量 {@code iam-tenant:delegation:read}。
     */
    TENANT_DELEGATION_READ("iam-tenant:delegation:read"),
    /**
     * 契约字面量 {@code iam-tenant:delegation:create}。
     */
    TENANT_DELEGATION_CREATE("iam-tenant:delegation:create"),
    /**
     * 契约字面量 {@code iam-tenant:delegation:update}。
     */
    TENANT_DELEGATION_UPDATE("iam-tenant:delegation:update"),
    /**
     * 契约字面量 {@code iam-tenant:delegation:delete}。
     */
    TENANT_DELEGATION_DELETE("iam-tenant:delegation:delete"),
    /**
     * 契约字面量 {@code iam-tenant:delegation:preview}。
     */
    TENANT_DELEGATION_PREVIEW("iam-tenant:delegation:preview");

    /**
     * 契约字面量 {@code iam-tenant:directory:read}。
     */
    public static final String VALUE_TENANT_DIRECTORY_READ = "iam-tenant:directory:read";
    /**
     * 契约字面量 {@code iam-platform:member:read}。
     */
    public static final String VALUE_PLATFORM_MEMBER_READ = "iam-platform:member:read";
    /**
     * 契约字面量 {@code iam-platform:member:create}。
     */
    public static final String VALUE_PLATFORM_MEMBER_CREATE = "iam-platform:member:create";
    /**
     * 契约字面量 {@code iam-platform:member:update}。
     */
    public static final String VALUE_PLATFORM_MEMBER_UPDATE = "iam-platform:member:update";
    /**
     * 契约字面量 {@code iam-platform:member:status}。
     */
    public static final String VALUE_PLATFORM_MEMBER_STATUS = "iam-platform:member:status";
    /**
     * 契约字面量 {@code iam-platform:member:remove}。
     */
    public static final String VALUE_PLATFORM_MEMBER_REMOVE = "iam-platform:member:remove";
    /**
     * 契约字面量 {@code iam-platform:account:lookup}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_LOOKUP = "iam-platform:account:lookup";
    /**
     * 契约字面量 {@code iam-platform:account:read}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_READ = "iam-platform:account:read";
    /**
     * 契约字面量 {@code iam-platform:account:create}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_CREATE = "iam-platform:account:create";
    /**
     * 契约字面量 {@code iam-platform:account:update}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_UPDATE = "iam-platform:account:update";
    /**
     * 契约字面量 {@code iam-platform:account:delete}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_DELETE = "iam-platform:account:delete";
    /**
     * 契约字面量 {@code iam-platform:account:enable}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_ENABLE = "iam-platform:account:enable";
    /**
     * 契约字面量 {@code iam-platform:account:disable}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_DISABLE = "iam-platform:account:disable";
    /**
     * 契约字面量 {@code iam-platform:account:lock}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_LOCK = "iam-platform:account:lock";
    /**
     * 契约字面量 {@code iam-platform:account:unlock}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_UNLOCK = "iam-platform:account:unlock";
    /**
     * 契约字面量 {@code iam-platform:account:reset-password}。
     */
    public static final String VALUE_PLATFORM_ACCOUNT_RESET_PASSWORD = "iam-platform:account:reset-password";
    /**
     * 契约字面量 {@code iam-platform:dictionary:read}。
     */
    public static final String VALUE_PLATFORM_DICTIONARY_READ = "iam-platform:dictionary:read";
    /**
     * 契约字面量 {@code iam-platform:dictionary:create}。
     */
    public static final String VALUE_PLATFORM_DICTIONARY_CREATE = "iam-platform:dictionary:create";
    /**
     * 契约字面量 {@code iam-platform:dictionary:update}。
     */
    public static final String VALUE_PLATFORM_DICTIONARY_UPDATE = "iam-platform:dictionary:update";
    /**
     * 契约字面量 {@code iam-platform:dictionary:delete}。
     */
    public static final String VALUE_PLATFORM_DICTIONARY_DELETE = "iam-platform:dictionary:delete";
    /**
     * 契约字面量 {@code iam-platform:id-allocation:read}。
     */
    public static final String VALUE_PLATFORM_ID_ALLOCATION_READ = "iam-platform:id-allocation:read";
    /**
     * 契约字面量 {@code iam-platform:id-allocation:create}。
     */
    public static final String VALUE_PLATFORM_ID_ALLOCATION_CREATE = "iam-platform:id-allocation:create";
    /**
     * 契约字面量 {@code iam-platform:id-allocation:update}。
     */
    public static final String VALUE_PLATFORM_ID_ALLOCATION_UPDATE = "iam-platform:id-allocation:update";
    /**
     * 契约字面量 {@code iam-platform:id-allocation:delete}。
     */
    public static final String VALUE_PLATFORM_ID_ALLOCATION_DELETE = "iam-platform:id-allocation:delete";
    /**
     * 契约字面量 {@code iam-platform:social-config:read}。
     */
    public static final String VALUE_PLATFORM_SOCIAL_CONFIG_READ = "iam-platform:social-config:read";
    /**
     * 契约字面量 {@code iam-platform:social-config:create}。
     */
    public static final String VALUE_PLATFORM_SOCIAL_CONFIG_CREATE = "iam-platform:social-config:create";
    /**
     * 契约字面量 {@code iam-platform:social-config:update}。
     */
    public static final String VALUE_PLATFORM_SOCIAL_CONFIG_UPDATE = "iam-platform:social-config:update";
    /**
     * 契约字面量 {@code iam-platform:social-config:delete}。
     */
    public static final String VALUE_PLATFORM_SOCIAL_CONFIG_DELETE = "iam-platform:social-config:delete";
    /**
     * 契约字面量 {@code iam-platform:lockout-policy:read}。
     */
    public static final String VALUE_PLATFORM_LOCKOUT_POLICY_READ = "iam-platform:lockout-policy:read";
    /**
     * 契约字面量 {@code iam-platform:lockout-policy:update}。
     */
    public static final String VALUE_PLATFORM_LOCKOUT_POLICY_UPDATE = "iam-platform:lockout-policy:update";
    /**
     * 契约字面量 {@code iam-platform:credential-policy:read}。
     */
    public static final String VALUE_PLATFORM_CREDENTIAL_POLICY_READ = "iam-platform:credential-policy:read";
    /**
     * 契约字面量 {@code iam-platform:credential-policy:create}。
     */
    public static final String VALUE_PLATFORM_CREDENTIAL_POLICY_CREATE = "iam-platform:credential-policy:create";
    /**
     * 契约字面量 {@code iam-platform:credential-policy:update}。
     */
    public static final String VALUE_PLATFORM_CREDENTIAL_POLICY_UPDATE = "iam-platform:credential-policy:update";
    /**
     * 契约字面量 {@code iam-platform:credential-policy:delete}。
     */
    public static final String VALUE_PLATFORM_CREDENTIAL_POLICY_DELETE = "iam-platform:credential-policy:delete";
    /**
     * 契约字面量 {@code iam-platform:login-failure-policy:read}。
     */
    public static final String VALUE_PLATFORM_LOGIN_FAILURE_POLICY_READ = "iam-platform:login-failure-policy:read";
    /**
     * 契约字面量 {@code iam-platform:login-failure-policy:update}。
     */
    public static final String VALUE_PLATFORM_LOGIN_FAILURE_POLICY_UPDATE = "iam-platform:login-failure-policy:update";
    /**
     * 契约字面量 {@code iam-platform:session:read}。
     */
    public static final String VALUE_PLATFORM_SESSION_READ = "iam-platform:session:read";
    /**
     * 契约字面量 {@code iam-platform:session:revoke}。
     */
    public static final String VALUE_PLATFORM_SESSION_REVOKE = "iam-platform:session:revoke";
    /**
     * 契约字面量 {@code iam-platform:session-policy:read}。
     */
    public static final String VALUE_PLATFORM_SESSION_POLICY_READ = "iam-platform:session-policy:read";
    /**
     * 契约字面量 {@code iam-platform:session-policy:update}。
     */
    public static final String VALUE_PLATFORM_SESSION_POLICY_UPDATE = "iam-platform:session-policy:update";
    /**
     * 契约字面量 {@code iam-platform:security-policy:read}。
     */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_READ = "iam-platform:security-policy:read";
    /**
     * 契约字面量 {@code iam-platform:security-policy:create}。
     */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_CREATE = "iam-platform:security-policy:create";
    /**
     * 契约字面量 {@code iam-platform:security-policy:update}。
     */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_UPDATE = "iam-platform:security-policy:update";
    /**
     * 契约字面量 {@code iam-platform:security-policy:delete}。
     */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_DELETE = "iam-platform:security-policy:delete";
    /**
     * 契约字面量 {@code iam-platform:group:read}。
     */
    public static final String VALUE_PLATFORM_GROUP_READ = "iam-platform:group:read";
    /**
     * 契约字面量 {@code iam-platform:group:create}。
     */
    public static final String VALUE_PLATFORM_GROUP_CREATE = "iam-platform:group:create";
    /**
     * 契约字面量 {@code iam-platform:group:update}。
     */
    public static final String VALUE_PLATFORM_GROUP_UPDATE = "iam-platform:group:update";
    /**
     * 契约字面量 {@code iam-platform:group:delete}。
     */
    public static final String VALUE_PLATFORM_GROUP_DELETE = "iam-platform:group:delete";
    /**
     * 契约字面量 {@code iam-platform:group:preview}。
     */
    public static final String VALUE_PLATFORM_GROUP_PREVIEW = "iam-platform:group:preview";
    /**
     * 契约字面量 {@code iam-platform:tenant:read}。
     */
    public static final String VALUE_PLATFORM_TENANT_READ = "iam-platform:tenant:read";
    /**
     * 契约字面量 {@code iam-platform:tenant:create}。
     */
    public static final String VALUE_PLATFORM_TENANT_CREATE = "iam-platform:tenant:create";
    /**
     * 契约字面量 {@code iam-platform:tenant:preview}。
     */
    public static final String VALUE_PLATFORM_TENANT_PREVIEW = "iam-platform:tenant:preview";
    /**
     * 契约字面量 {@code iam-platform:tenant:update}。
     */
    public static final String VALUE_PLATFORM_TENANT_UPDATE = "iam-platform:tenant:update";
    /**
     * 契约字面量 {@code iam-platform:entitlement:read}。
     */
    public static final String VALUE_PLATFORM_ENTITLEMENT_READ = "iam-platform:entitlement:read";
    /**
     * 契约字面量 {@code iam-platform:entitlement:update}。
     */
    public static final String VALUE_PLATFORM_ENTITLEMENT_UPDATE = "iam-platform:entitlement:update";
    /**
     * 契约字面量 {@code iam-platform:entitlement:preview}。
     */
    public static final String VALUE_PLATFORM_ENTITLEMENT_PREVIEW = "iam-platform:entitlement:preview";
    /**
     * 契约字面量 {@code iam-platform:application:read}。
     */
    public static final String VALUE_PLATFORM_APPLICATION_READ = "iam-platform:application:read";
    /**
     * 契约字面量 {@code iam-platform:application:create}。
     */
    public static final String VALUE_PLATFORM_APPLICATION_CREATE = "iam-platform:application:create";
    /**
     * 契约字面量 {@code iam-platform:application:update}。
     */
    public static final String VALUE_PLATFORM_APPLICATION_UPDATE = "iam-platform:application:update";
    /**
     * 契约字面量 {@code iam-platform:application:status}。
     */
    public static final String VALUE_PLATFORM_APPLICATION_STATUS = "iam-platform:application:status";
    /**
     * 契约字面量 {@code iam-platform:application:delete}。
     */
    public static final String VALUE_PLATFORM_APPLICATION_DELETE = "iam-platform:application:delete";
    /**
     * 契约字面量 {@code iam-platform:resource:read}。
     */
    public static final String VALUE_PLATFORM_RESOURCE_READ = "iam-platform:resource:read";
    /**
     * 契约字面量 {@code iam-platform:resource:create}。
     */
    public static final String VALUE_PLATFORM_RESOURCE_CREATE = "iam-platform:resource:create";
    /**
     * 契约字面量 {@code iam-platform:resource:update}。
     */
    public static final String VALUE_PLATFORM_RESOURCE_UPDATE = "iam-platform:resource:update";
    /**
     * 契约字面量 {@code iam-platform:resource:delete}。
     */
    public static final String VALUE_PLATFORM_RESOURCE_DELETE = "iam-platform:resource:delete";
    /**
     * 契约字面量 {@code iam-platform:action:read}。
     */
    public static final String VALUE_PLATFORM_ACTION_READ = "iam-platform:action:read";
    /**
     * 契约字面量 {@code iam-platform:action:create}。
     */
    public static final String VALUE_PLATFORM_ACTION_CREATE = "iam-platform:action:create";
    /**
     * 契约字面量 {@code iam-platform:action:update}。
     */
    public static final String VALUE_PLATFORM_ACTION_UPDATE = "iam-platform:action:update";
    /**
     * 契约字面量 {@code iam-platform:action:status}。
     */
    public static final String VALUE_PLATFORM_ACTION_STATUS = "iam-platform:action:status";
    /**
     * 契约字面量 {@code iam-platform:action:delete}。
     */
    public static final String VALUE_PLATFORM_ACTION_DELETE = "iam-platform:action:delete";
    /**
     * 契约字面量 {@code iam-platform:menu:read}。
     */
    public static final String VALUE_PLATFORM_MENU_READ = "iam-platform:menu:read";
    /**
     * 契约字面量 {@code iam-platform:menu:create}。
     */
    public static final String VALUE_PLATFORM_MENU_CREATE = "iam-platform:menu:create";
    /**
     * 契约字面量 {@code iam-platform:menu:update}。
     */
    public static final String VALUE_PLATFORM_MENU_UPDATE = "iam-platform:menu:update";
    /**
     * 契约字面量 {@code iam-platform:menu:delete}。
     */
    public static final String VALUE_PLATFORM_MENU_DELETE = "iam-platform:menu:delete";
    /**
     * 契约字面量 {@code iam-platform:plan:read}。
     */
    public static final String VALUE_PLATFORM_PLAN_READ = "iam-platform:plan:read";
    /**
     * 契约字面量 {@code iam-platform:plan:create}。
     */
    public static final String VALUE_PLATFORM_PLAN_CREATE = "iam-platform:plan:create";
    /**
     * 契约字面量 {@code iam-platform:plan:update}。
     */
    public static final String VALUE_PLATFORM_PLAN_UPDATE = "iam-platform:plan:update";
    /**
     * 契约字面量 {@code iam-platform:role:read}。
     */
    public static final String VALUE_PLATFORM_ROLE_READ = "iam-platform:role:read";
    /**
     * 契约字面量 {@code iam-platform:role:create}。
     */
    public static final String VALUE_PLATFORM_ROLE_CREATE = "iam-platform:role:create";
    /**
     * 契约字面量 {@code iam-platform:role:status}。
     */
    public static final String VALUE_PLATFORM_ROLE_STATUS = "iam-platform:role:status";
    /**
     * 契约字面量 {@code iam-platform:role:delete}。
     */
    public static final String VALUE_PLATFORM_ROLE_DELETE = "iam-platform:role:delete";
    /**
     * 契约字面量 {@code iam-platform:role:publish}。
     */
    public static final String VALUE_PLATFORM_ROLE_PUBLISH = "iam-platform:role:publish";
    /**
     * 契约字面量 {@code iam-platform:role:preview}。
     */
    public static final String VALUE_PLATFORM_ROLE_PREVIEW = "iam-platform:role:preview";
    /**
     * 契约字面量 {@code iam-platform:shared-role:read}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_READ = "iam-platform:shared-role:read";
    /**
     * 契约字面量 {@code iam-platform:shared-role:create}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_CREATE = "iam-platform:shared-role:create";
    /**
     * 契约字面量 {@code iam-platform:shared-role:status}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_STATUS = "iam-platform:shared-role:status";
    /**
     * 契约字面量 {@code iam-platform:shared-role:delete}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_DELETE = "iam-platform:shared-role:delete";
    /**
     * 契约字面量 {@code iam-platform:shared-role:publish}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_PUBLISH = "iam-platform:shared-role:publish";
    /**
     * 契约字面量 {@code iam-platform:shared-role:preview}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_PREVIEW = "iam-platform:shared-role:preview";
    /**
     * 契约字面量 {@code iam-platform:assignment:read}。
     */
    public static final String VALUE_PLATFORM_ASSIGNMENT_READ = "iam-platform:assignment:read";
    /**
     * 契约字面量 {@code iam-platform:assignment:create}。
     */
    public static final String VALUE_PLATFORM_ASSIGNMENT_CREATE = "iam-platform:assignment:create";
    /**
     * 契约字面量 {@code iam-platform:assignment:update}。
     */
    public static final String VALUE_PLATFORM_ASSIGNMENT_UPDATE = "iam-platform:assignment:update";
    /**
     * 契约字面量 {@code iam-platform:assignment:delete}。
     */
    public static final String VALUE_PLATFORM_ASSIGNMENT_DELETE = "iam-platform:assignment:delete";
    /**
     * 契约字面量 {@code iam-platform:delegation:read}。
     */
    public static final String VALUE_PLATFORM_DELEGATION_READ = "iam-platform:delegation:read";
    /**
     * 契约字面量 {@code iam-platform:delegation:create}。
     */
    public static final String VALUE_PLATFORM_DELEGATION_CREATE = "iam-platform:delegation:create";
    /**
     * 契约字面量 {@code iam-platform:delegation:update}。
     */
    public static final String VALUE_PLATFORM_DELEGATION_UPDATE = "iam-platform:delegation:update";
    /**
     * 契约字面量 {@code iam-platform:delegation:delete}。
     */
    public static final String VALUE_PLATFORM_DELEGATION_DELETE = "iam-platform:delegation:delete";
    /**
     * 契约字面量 {@code iam-platform:delegation:preview}。
     */
    public static final String VALUE_PLATFORM_DELEGATION_PREVIEW = "iam-platform:delegation:preview";
    /**
     * 契约字面量 {@code iam-platform:authorization:diagnose}。
     */
    public static final String VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE = "iam-platform:authorization:diagnose";
    /**
     * 契约字面量 {@code iam-platform:audit:read}。
     */
    public static final String VALUE_PLATFORM_AUDIT_READ = "iam-platform:audit:read";
    /**
     * 契约字面量 {@code iam-tenant:member:read}。
     */
    public static final String VALUE_TENANT_MEMBER_READ = "iam-tenant:member:read";
    /**
     * 契约字面量 {@code iam-tenant:member:create}。
     */
    public static final String VALUE_TENANT_MEMBER_CREATE = "iam-tenant:member:create";
    /**
     * 契约字面量 {@code iam-tenant:member:update}。
     */
    public static final String VALUE_TENANT_MEMBER_UPDATE = "iam-tenant:member:update";
    /**
     * 契约字面量 {@code iam-tenant:member:status}。
     */
    public static final String VALUE_TENANT_MEMBER_STATUS = "iam-tenant:member:status";
    /**
     * 契约字面量 {@code iam-tenant:member:remove}。
     */
    public static final String VALUE_TENANT_MEMBER_REMOVE = "iam-tenant:member:remove";
    /**
     * 契约字面量 {@code iam-tenant:member:departments}。
     */
    public static final String VALUE_TENANT_MEMBER_DEPARTMENTS = "iam-tenant:member:departments";
    /**
     * 契约字面量 {@code iam-tenant:member:export}。
     */
    public static final String VALUE_TENANT_MEMBER_EXPORT = "iam-tenant:member:export";
    /**
     * 契约字面量 {@code iam-tenant:department:read}。
     */
    public static final String VALUE_TENANT_DEPARTMENT_READ = "iam-tenant:department:read";
    /**
     * 契约字面量 {@code iam-tenant:department:create}。
     */
    public static final String VALUE_TENANT_DEPARTMENT_CREATE = "iam-tenant:department:create";
    /**
     * 契约字面量 {@code iam-tenant:department:update}。
     */
    public static final String VALUE_TENANT_DEPARTMENT_UPDATE = "iam-tenant:department:update";
    /**
     * 契约字面量 {@code iam-tenant:department:delete}。
     */
    public static final String VALUE_TENANT_DEPARTMENT_DELETE = "iam-tenant:department:delete";
    /**
     * 契约字面量 {@code iam-tenant:settings:read}。
     */
    public static final String VALUE_TENANT_SETTINGS_READ = "iam-tenant:settings:read";
    /**
     * 契约字面量 {@code iam-tenant:settings:update}。
     */
    public static final String VALUE_TENANT_SETTINGS_UPDATE = "iam-tenant:settings:update";
    /**
     * 契约字面量 {@code iam-tenant:settings:owner-transfer}。
     */
    public static final String VALUE_TENANT_SETTINGS_OWNER_TRANSFER = "iam-tenant:settings:owner-transfer";
    /**
     * 契约字面量 {@code iam-tenant:application:read}。
     */
    public static final String VALUE_TENANT_APPLICATION_READ = "iam-tenant:application:read";
    /**
     * 契约字面量 {@code iam-tenant:audience:read}。
     */
    public static final String VALUE_TENANT_AUDIENCE_READ = "iam-tenant:audience:read";
    /**
     * 契约字面量 {@code iam-tenant:audience:update}。
     */
    public static final String VALUE_TENANT_AUDIENCE_UPDATE = "iam-tenant:audience:update";
    /**
     * 契约字面量 {@code iam-tenant:group:read}。
     */
    public static final String VALUE_TENANT_GROUP_READ = "iam-tenant:group:read";
    /**
     * 契约字面量 {@code iam-tenant:group:create}。
     */
    public static final String VALUE_TENANT_GROUP_CREATE = "iam-tenant:group:create";
    /**
     * 契约字面量 {@code iam-tenant:group:update}。
     */
    public static final String VALUE_TENANT_GROUP_UPDATE = "iam-tenant:group:update";
    /**
     * 契约字面量 {@code iam-tenant:group:delete}。
     */
    public static final String VALUE_TENANT_GROUP_DELETE = "iam-tenant:group:delete";
    /**
     * 契约字面量 {@code iam-tenant:group:preview}。
     */
    public static final String VALUE_TENANT_GROUP_PREVIEW = "iam-tenant:group:preview";
    /**
     * 契约字面量 {@code iam-tenant:role:read}。
     */
    public static final String VALUE_TENANT_ROLE_READ = "iam-tenant:role:read";
    /**
     * 契约字面量 {@code iam-tenant:role:create}。
     */
    public static final String VALUE_TENANT_ROLE_CREATE = "iam-tenant:role:create";
    /**
     * 契约字面量 {@code iam-tenant:role:status}。
     */
    public static final String VALUE_TENANT_ROLE_STATUS = "iam-tenant:role:status";
    /**
     * 契约字面量 {@code iam-tenant:role:delete}。
     */
    public static final String VALUE_TENANT_ROLE_DELETE = "iam-tenant:role:delete";
    /**
     * 契约字面量 {@code iam-tenant:role:publish}。
     */
    public static final String VALUE_TENANT_ROLE_PUBLISH = "iam-tenant:role:publish";
    /**
     * 契约字面量 {@code iam-tenant:role:preview}。
     */
    public static final String VALUE_TENANT_ROLE_PREVIEW = "iam-tenant:role:preview";
    /**
     * 契约字面量 {@code iam-tenant:role:upgrade}。
     */
    public static final String VALUE_TENANT_ROLE_UPGRADE = "iam-tenant:role:upgrade";
    /**
     * 契约字面量 {@code iam-tenant:assignment:read}。
     */
    public static final String VALUE_TENANT_ASSIGNMENT_READ = "iam-tenant:assignment:read";
    /**
     * 契约字面量 {@code iam-tenant:assignment:create}。
     */
    public static final String VALUE_TENANT_ASSIGNMENT_CREATE = "iam-tenant:assignment:create";
    /**
     * 契约字面量 {@code iam-tenant:assignment:update}。
     */
    public static final String VALUE_TENANT_ASSIGNMENT_UPDATE = "iam-tenant:assignment:update";
    /**
     * 契约字面量 {@code iam-tenant:assignment:delete}。
     */
    public static final String VALUE_TENANT_ASSIGNMENT_DELETE = "iam-tenant:assignment:delete";
    /**
     * 契约字面量 {@code iam-tenant:directory-policy:read}。
     */
    public static final String VALUE_TENANT_DIRECTORY_POLICY_READ = "iam-tenant:directory-policy:read";
    /**
     * 契约字面量 {@code iam-tenant:directory-policy:update}。
     */
    public static final String VALUE_TENANT_DIRECTORY_POLICY_UPDATE = "iam-tenant:directory-policy:update";
    /**
     * 契约字面量 {@code iam-tenant:field-policy:read}。
     */
    public static final String VALUE_TENANT_FIELD_POLICY_READ = "iam-tenant:field-policy:read";
    /**
     * 契约字面量 {@code iam-tenant:field-policy:update}。
     */
    public static final String VALUE_TENANT_FIELD_POLICY_UPDATE = "iam-tenant:field-policy:update";
    /**
     * 契约字面量 {@code iam-tenant:policy:preview}。
     */
    public static final String VALUE_TENANT_POLICY_PREVIEW = "iam-tenant:policy:preview";
    /**
     * 契约字面量 {@code iam-tenant:authorization:diagnose}。
     */
    public static final String VALUE_TENANT_AUTHORIZATION_DIAGNOSE = "iam-tenant:authorization:diagnose";
    /**
     * 契约字面量 {@code iam-tenant:audit:read}。
     */
    public static final String VALUE_TENANT_AUDIT_READ = "iam-tenant:audit:read";
    /**
     * 契约字面量 {@code iam-tenant:delegation:read}。
     */
    public static final String VALUE_TENANT_DELEGATION_READ = "iam-tenant:delegation:read";
    /**
     * 契约字面量 {@code iam-tenant:delegation:create}。
     */
    public static final String VALUE_TENANT_DELEGATION_CREATE = "iam-tenant:delegation:create";
    /**
     * 契约字面量 {@code iam-tenant:delegation:update}。
     */
    public static final String VALUE_TENANT_DELEGATION_UPDATE = "iam-tenant:delegation:update";
    /**
     * 契约字面量 {@code iam-tenant:delegation:delete}。
     */
    public static final String VALUE_TENANT_DELEGATION_DELETE = "iam-tenant:delegation:delete";
    /**
     * 契约字面量 {@code iam-tenant:delegation:preview}。
     */
    public static final String VALUE_TENANT_DELEGATION_PREVIEW = "iam-tenant:delegation:preview";

    /**
     * JSON、数据库与授权求值使用的稳定操作码。
     */
    @JsonValue
    @EnumValue
    private final String code;
    /**
     * 由操作码末段派生的操作语义。
     */
    private final IamActionOperation operation;

    IamAction(String code) {
        this.code = code;
        this.operation = IamActionOperation.ofActionCode(code);
    }

    private static final Map<String, IamAction> BY_CODE = EnumUtils.index(values(), IamAction::getCode);

    /**
     * 按契约字面量解析 ACTION。
     *
     * @param code 精确操作码；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static IamAction getEnum(String code) {
        return EnumUtils.require(BY_CODE, code);
    }
}
