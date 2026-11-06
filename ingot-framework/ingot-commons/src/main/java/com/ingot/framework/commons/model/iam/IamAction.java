package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
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
     * 契约字面量 {@code iam-platform:member:read}。
     */
    PLATFORM_MEMBER_READ("iam-platform:member:read", Resources.IAM_PLATFORM_MEMBER),
    /**
     * 契约字面量 {@code iam-platform:member:create}。
     */
    PLATFORM_MEMBER_CREATE("iam-platform:member:create", Resources.IAM_PLATFORM_MEMBER),
    /**
     * 契约字面量 {@code iam-platform:member:update}。
     */
    PLATFORM_MEMBER_UPDATE("iam-platform:member:update", Resources.IAM_PLATFORM_MEMBER),
    /**
     * 暂停或恢复平台成员资格。
     */
    PLATFORM_MEMBER_STATUS("iam-platform:member:change-status", Resources.IAM_PLATFORM_MEMBER),
    /**
     * 移出平台成员，不删除账号。
     */
    PLATFORM_MEMBER_REMOVE("iam-platform:member:remove", Resources.IAM_PLATFORM_MEMBER),
    /**
     * 受限精确查找全局账号。
     */
    PLATFORM_ACCOUNT_LOOKUP("iam-platform:account:lookup", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 读取全局账号列表或详情。
     */
    PLATFORM_ACCOUNT_READ("iam-platform:account:read", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 创建全局账号。
     */
    PLATFORM_ACCOUNT_CREATE("iam-platform:account:create", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 编辑全局账号资料。
     */
    PLATFORM_ACCOUNT_UPDATE("iam-platform:account:update", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 删除全局账号。
     */
    PLATFORM_ACCOUNT_DELETE("iam-platform:account:delete", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 启用全局账号。
     */
    PLATFORM_ACCOUNT_ENABLE("iam-platform:account:enable", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 停用全局账号。
     */
    PLATFORM_ACCOUNT_DISABLE("iam-platform:account:disable", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 锁定全局账号。
     */
    PLATFORM_ACCOUNT_LOCK("iam-platform:account:lock", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 解锁全局账号。
     */
    PLATFORM_ACCOUNT_UNLOCK("iam-platform:account:unlock", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 重置全局账号密码。
     */
    PLATFORM_ACCOUNT_RESET_PASSWORD("iam-platform:account:reset-password", Resources.IAM_PLATFORM_ACCOUNT),
    /**
     * 读取平台字典。
     */
    PLATFORM_DICTIONARY_READ("iam-platform:dictionary:read", Resources.IAM_PLATFORM_DICTIONARY),
    /**
     * 创建平台字典。
     */
    PLATFORM_DICTIONARY_CREATE("iam-platform:dictionary:create", Resources.IAM_PLATFORM_DICTIONARY),
    /**
     * 更新平台字典。
     */
    PLATFORM_DICTIONARY_UPDATE("iam-platform:dictionary:update", Resources.IAM_PLATFORM_DICTIONARY),
    /**
     * 删除平台字典。
     */
    PLATFORM_DICTIONARY_DELETE("iam-platform:dictionary:delete", Resources.IAM_PLATFORM_DICTIONARY),
    /**
     * 读取发号配置。
     */
    PLATFORM_ID_ALLOCATION_READ("platform-developer:id-allocation:read", Resources.PLATFORM_DEVELOPER_ID_ALLOCATION),
    /**
     * 创建发号配置。
     */
    PLATFORM_ID_ALLOCATION_CREATE("platform-developer:id-allocation:create", Resources.PLATFORM_DEVELOPER_ID_ALLOCATION),
    /**
     * 更新发号配置。
     */
    PLATFORM_ID_ALLOCATION_UPDATE("platform-developer:id-allocation:update", Resources.PLATFORM_DEVELOPER_ID_ALLOCATION),
    /**
     * 删除发号配置。
     */
    PLATFORM_ID_ALLOCATION_DELETE("platform-developer:id-allocation:delete", Resources.PLATFORM_DEVELOPER_ID_ALLOCATION),
    /**
     * 读取社会化登录配置。
     */
    PLATFORM_SOCIAL_CONFIG_READ("platform-developer:social-config:read", Resources.PLATFORM_DEVELOPER_SOCIAL_CONFIG),
    /**
     * 创建社会化登录配置。
     */
    PLATFORM_SOCIAL_CONFIG_CREATE("platform-developer:social-config:create", Resources.PLATFORM_DEVELOPER_SOCIAL_CONFIG),
    /**
     * 更新社会化登录配置。
     */
    PLATFORM_SOCIAL_CONFIG_UPDATE("platform-developer:social-config:update", Resources.PLATFORM_DEVELOPER_SOCIAL_CONFIG),
    /**
     * 删除社会化登录配置。
     */
    PLATFORM_SOCIAL_CONFIG_DELETE("platform-developer:social-config:delete", Resources.PLATFORM_DEVELOPER_SOCIAL_CONFIG),
    /**
     * 读取账号锁定策略。
     */
    PLATFORM_LOCKOUT_POLICY_READ("platform-security:account-lockout-policy:read", Resources.PLATFORM_SECURITY_ACCOUNT_LOCKOUT_POLICY),
    /**
     * 更新账号锁定策略。
     */
    PLATFORM_LOCKOUT_POLICY_UPDATE("platform-security:account-lockout-policy:update", Resources.PLATFORM_SECURITY_ACCOUNT_LOCKOUT_POLICY),
    /**
     * 读取凭证策略。
     */
    PLATFORM_CREDENTIAL_POLICY_READ("platform-security:credential-policy:read", Resources.PLATFORM_SECURITY_CREDENTIAL_POLICY),
    /**
     * 创建凭证策略。
     */
    PLATFORM_CREDENTIAL_POLICY_CREATE("platform-security:credential-policy:create", Resources.PLATFORM_SECURITY_CREDENTIAL_POLICY),
    /**
     * 更新凭证策略。
     */
    PLATFORM_CREDENTIAL_POLICY_UPDATE("platform-security:credential-policy:update", Resources.PLATFORM_SECURITY_CREDENTIAL_POLICY),
    /**
     * 删除凭证策略。
     */
    PLATFORM_CREDENTIAL_POLICY_DELETE("platform-security:credential-policy:delete", Resources.PLATFORM_SECURITY_CREDENTIAL_POLICY),
    /**
     * 读取登录失败防护策略。
     */
    PLATFORM_LOGIN_FAILURE_POLICY_READ("platform-security:login-failure-policy:read", Resources.PLATFORM_SECURITY_LOGIN_FAILURE_POLICY),
    /**
     * 更新登录失败防护策略。
     */
    PLATFORM_LOGIN_FAILURE_POLICY_UPDATE("platform-security:login-failure-policy:update", Resources.PLATFORM_SECURITY_LOGIN_FAILURE_POLICY),
    /**
     * 读取在线会话。
     */
    PLATFORM_SESSION_READ("platform-security:session:read", Resources.PLATFORM_SECURITY_SESSION),
    /**
     * 撤销在线会话。
     */
    PLATFORM_SESSION_REVOKE("platform-security:session:revoke", Resources.PLATFORM_SECURITY_SESSION),
    /**
     * 读取会话并发策略。
     */
    PLATFORM_SESSION_POLICY_READ("platform-security:session-concurrency-policy:read", Resources.PLATFORM_SECURITY_SESSION_CONCURRENCY_POLICY),
    /**
     * 更新会话并发策略。
     */
    PLATFORM_SESSION_POLICY_UPDATE("platform-security:session-concurrency-policy:update", Resources.PLATFORM_SECURITY_SESSION_CONCURRENCY_POLICY),
    /** 安全中心的独立操作。 */
    PLATFORM_SECURITY_POLICY_REFRESH("platform-security:access-policy:refresh", Resources.PLATFORM_SECURITY_ACCESS_POLICY),
    /** 安全中心的独立操作。 */
    PLATFORM_BLOCK_EVENT_READ("platform-security:block-event:read", Resources.PLATFORM_SECURITY_BLOCK_EVENT),
    /** 安全中心的独立操作。 */
    PLATFORM_SESSION_POLICY_CREATE("platform-security:session-concurrency-policy:create", Resources.PLATFORM_SECURITY_SESSION_CONCURRENCY_POLICY),
    /** 安全中心的独立操作。 */
    PLATFORM_SESSION_POLICY_DELETE("platform-security:session-concurrency-policy:delete", Resources.PLATFORM_SECURITY_SESSION_CONCURRENCY_POLICY),
    /**
     * 读取安全策略。
     */
    PLATFORM_SECURITY_POLICY_READ("platform-security:access-policy:read", Resources.PLATFORM_SECURITY_ACCESS_POLICY),
    /**
     * 创建安全策略。
     */
    PLATFORM_SECURITY_POLICY_CREATE("platform-security:access-policy:create", Resources.PLATFORM_SECURITY_ACCESS_POLICY),
    /**
     * 更新安全策略。
     */
    PLATFORM_SECURITY_POLICY_UPDATE("platform-security:access-policy:update", Resources.PLATFORM_SECURITY_ACCESS_POLICY),
    /**
     * 删除安全策略。
     */
    PLATFORM_SECURITY_POLICY_DELETE("platform-security:access-policy:delete", Resources.PLATFORM_SECURITY_ACCESS_POLICY),
    /**
     * 契约字面量 {@code iam-platform:group:read}。
     */
    PLATFORM_GROUP_READ("iam-platform:group:read", Resources.IAM_PLATFORM_GROUP),
    /**
     * 契约字面量 {@code iam-platform:group:create}。
     */
    PLATFORM_GROUP_CREATE("iam-platform:group:create", Resources.IAM_PLATFORM_GROUP),
    /**
     * 契约字面量 {@code iam-platform:group:update}。
     */
    PLATFORM_GROUP_UPDATE("iam-platform:group:update", Resources.IAM_PLATFORM_GROUP),
    /**
     * 契约字面量 {@code iam-platform:group:delete}。
     */
    PLATFORM_GROUP_DELETE("iam-platform:group:delete", Resources.IAM_PLATFORM_GROUP),
    /**
     * 契约字面量 {@code iam-platform:group:preview}。
     */
    PLATFORM_GROUP_PREVIEW("iam-platform:group:preview", Resources.IAM_PLATFORM_GROUP),
    /**
     * 契约字面量 {@code iam-platform:tenant:read}。
     */
    PLATFORM_TENANT_READ("iam-platform:tenant:read", Resources.IAM_PLATFORM_TENANT),
    /**
     * 原子创建组织及最小引用。
     */
    PLATFORM_TENANT_CREATE("iam-platform:tenant:create", Resources.IAM_PLATFORM_TENANT),
    /**
     * 预览组织初始化，无写入。
     */
    PLATFORM_TENANT_PREVIEW("iam-platform:tenant:preview", Resources.IAM_PLATFORM_TENANT),
    /**
     * 契约字面量 {@code iam-platform:tenant:update}。
     */
    PLATFORM_TENANT_UPDATE("iam-platform:tenant:update", Resources.IAM_PLATFORM_TENANT),
    /**
     * 契约字面量 {@code iam-platform:application-entitlement:read}。
     */
    PLATFORM_ENTITLEMENT_READ("iam-platform:application-entitlement:read", Resources.IAM_PLATFORM_APPLICATION_ENTITLEMENT),
    /**
     * 契约字面量 {@code iam-platform:application-entitlement:update}。
     */
    PLATFORM_ENTITLEMENT_UPDATE("iam-platform:application-entitlement:update", Resources.IAM_PLATFORM_APPLICATION_ENTITLEMENT),
    /**
     * 契约字面量 {@code iam-platform:application-entitlement:preview}。
     */
    PLATFORM_ENTITLEMENT_PREVIEW("iam-platform:application-entitlement:preview", Resources.IAM_PLATFORM_APPLICATION_ENTITLEMENT),
    /**
     * 契约字面量 {@code iam-platform:application:read}。
     */
    PLATFORM_APPLICATION_READ("iam-platform:application:read", Resources.IAM_PLATFORM_APPLICATION),
    /**
     * 契约字面量 {@code iam-platform:application:create}。
     */
    PLATFORM_APPLICATION_CREATE("iam-platform:application:create", Resources.IAM_PLATFORM_APPLICATION),
    /**
     * 契约字面量 {@code iam-platform:application:update}。
     */
    PLATFORM_APPLICATION_UPDATE("iam-platform:application:update", Resources.IAM_PLATFORM_APPLICATION),
    /**
     * 契约字面量 {@code iam-platform:application:change-status}。
     */
    PLATFORM_APPLICATION_STATUS("iam-platform:application:change-status", Resources.IAM_PLATFORM_APPLICATION),
    /**
     * 契约字面量 {@code iam-platform:application:delete}。
     */
    PLATFORM_APPLICATION_DELETE("iam-platform:application:delete", Resources.IAM_PLATFORM_APPLICATION),
    /**
     * 契约字面量 {@code iam-platform:application:purge}。
     */
    PLATFORM_APPLICATION_PURGE("iam-platform:application:purge", Resources.IAM_PLATFORM_APPLICATION),
    /**
     * 契约字面量 {@code iam-platform:resource:read}。
     */
    PLATFORM_RESOURCE_READ("iam-platform:resource:read", Resources.IAM_PLATFORM_RESOURCE),
    /**
     * 契约字面量 {@code iam-platform:resource:create}。
     */
    PLATFORM_RESOURCE_CREATE("iam-platform:resource:create", Resources.IAM_PLATFORM_RESOURCE),
    /**
     * 契约字面量 {@code iam-platform:resource:update}。
     */
    PLATFORM_RESOURCE_UPDATE("iam-platform:resource:update", Resources.IAM_PLATFORM_RESOURCE),
    /**
     * 契约字面量 {@code iam-platform:resource:delete}。
     */
    PLATFORM_RESOURCE_DELETE("iam-platform:resource:delete", Resources.IAM_PLATFORM_RESOURCE),
    /**
     * 契约字面量 {@code iam-platform:action:read}。
     */
    PLATFORM_ACTION_READ("iam-platform:action:read", Resources.IAM_PLATFORM_ACTION),
    /**
     * 契约字面量 {@code iam-platform:action:create}。
     */
    PLATFORM_ACTION_CREATE("iam-platform:action:create", Resources.IAM_PLATFORM_ACTION),
    /**
     * 契约字面量 {@code iam-platform:action:update}。
     */
    PLATFORM_ACTION_UPDATE("iam-platform:action:update", Resources.IAM_PLATFORM_ACTION),
    /**
     * 契约字面量 {@code iam-platform:action:change-status}。
     */
    PLATFORM_ACTION_STATUS("iam-platform:action:change-status", Resources.IAM_PLATFORM_ACTION),
    /**
     * 契约字面量 {@code iam-platform:action:delete}。
     */
    PLATFORM_ACTION_DELETE("iam-platform:action:delete", Resources.IAM_PLATFORM_ACTION),
    /**
     * 契约字面量 {@code iam-platform:menu:read}。
     */
    PLATFORM_MENU_READ("iam-platform:menu:read", Resources.IAM_PLATFORM_MENU),
    /**
     * 契约字面量 {@code iam-platform:menu:create}。
     */
    PLATFORM_MENU_CREATE("iam-platform:menu:create", Resources.IAM_PLATFORM_MENU),
    /**
     * 契约字面量 {@code iam-platform:menu:update}。
     */
    PLATFORM_MENU_UPDATE("iam-platform:menu:update", Resources.IAM_PLATFORM_MENU),
    /**
     * 契约字面量 {@code iam-platform:menu:delete}。
     */
    PLATFORM_MENU_DELETE("iam-platform:menu:delete", Resources.IAM_PLATFORM_MENU),
    /**
     * 契约字面量 {@code iam-platform:plan:read}。
     */
    PLATFORM_PLAN_READ("iam-platform:plan:read", Resources.IAM_PLATFORM_PLAN),
    /**
     * 契约字面量 {@code iam-platform:plan:create}。
     */
    PLATFORM_PLAN_CREATE("iam-platform:plan:create", Resources.IAM_PLATFORM_PLAN),
    /**
     * 契约字面量 {@code iam-platform:plan:update}。
     */
    PLATFORM_PLAN_UPDATE("iam-platform:plan:update", Resources.IAM_PLATFORM_PLAN),
    /**
     * 契约字面量 {@code iam-platform:role:read}。
     */
    PLATFORM_ROLE_READ("iam-platform:role:read", Resources.IAM_PLATFORM_ROLE),
    /**
     * 契约字面量 {@code iam-platform:role:create}。
     */
    PLATFORM_ROLE_CREATE("iam-platform:role:create", Resources.IAM_PLATFORM_ROLE),
    /**
     * 契约字面量 {@code iam-platform:role:change-status}。
     */
    PLATFORM_ROLE_STATUS("iam-platform:role:change-status", Resources.IAM_PLATFORM_ROLE),
    /**
     * 契约字面量 {@code iam-platform:role:delete}。
     */
    PLATFORM_ROLE_DELETE("iam-platform:role:delete", Resources.IAM_PLATFORM_ROLE),
    /**
     * 契约字面量 {@code iam-platform:role:publish-version}。
     */
    PLATFORM_ROLE_PUBLISH("iam-platform:role:publish-version", Resources.IAM_PLATFORM_ROLE),
    /**
     * 契约字面量 {@code iam-platform:role:preview}。
     */
    PLATFORM_ROLE_PREVIEW("iam-platform:role:preview", Resources.IAM_PLATFORM_ROLE),
    /**
     * 契约字面量 {@code iam-platform:shared-role:read}。
     */
    PLATFORM_SHARED_ROLE_READ("iam-platform:shared-role:read", Resources.IAM_PLATFORM_SHARED_ROLE),
    /**
     * 契约字面量 {@code iam-platform:shared-role:create}。
     */
    PLATFORM_SHARED_ROLE_CREATE("iam-platform:shared-role:create", Resources.IAM_PLATFORM_SHARED_ROLE),
    /**
     * 契约字面量 {@code iam-platform:shared-role:change-status}。
     */
    PLATFORM_SHARED_ROLE_STATUS("iam-platform:shared-role:change-status", Resources.IAM_PLATFORM_SHARED_ROLE),
    /**
     * 契约字面量 {@code iam-platform:shared-role:delete}。
     */
    PLATFORM_SHARED_ROLE_DELETE("iam-platform:shared-role:delete", Resources.IAM_PLATFORM_SHARED_ROLE),
    /**
     * 契约字面量 {@code iam-platform:shared-role:update}。
     */
    PLATFORM_SHARED_ROLE_UPDATE("iam-platform:shared-role:update", Resources.IAM_PLATFORM_SHARED_ROLE),
    /**
     * 契约字面量 {@code iam-platform:shared-role:preview}。
     */
    PLATFORM_SHARED_ROLE_PREVIEW("iam-platform:shared-role:preview", Resources.IAM_PLATFORM_SHARED_ROLE),
    /**
     * 契约字面量 {@code iam-platform:role-assignment:read}。
     */
    PLATFORM_ASSIGNMENT_READ("iam-platform:role-assignment:read", Resources.IAM_PLATFORM_ROLE_ASSIGNMENT),
    /**
     * 契约字面量 {@code iam-platform:role-assignment:create}。
     */
    PLATFORM_ASSIGNMENT_CREATE("iam-platform:role-assignment:create", Resources.IAM_PLATFORM_ROLE_ASSIGNMENT),
    /**
     * 契约字面量 {@code iam-platform:role-assignment:update}。
     */
    PLATFORM_ASSIGNMENT_UPDATE("iam-platform:role-assignment:update", Resources.IAM_PLATFORM_ROLE_ASSIGNMENT),
    /**
     * 契约字面量 {@code iam-platform:role-assignment:revoke}。
     */
    PLATFORM_ASSIGNMENT_DELETE("iam-platform:role-assignment:revoke", Resources.IAM_PLATFORM_ROLE_ASSIGNMENT),
    /** 显式升级已有分配的固定版本。 */
    PLATFORM_ASSIGNMENT_UPGRADE("iam-platform:role-assignment:apply-version", Resources.IAM_PLATFORM_ROLE_ASSIGNMENT),
    /**
     * 契约字面量 {@code iam-platform:delegation:read}。
     */
    PLATFORM_DELEGATION_READ("iam-platform:delegation:read", Resources.IAM_PLATFORM_DELEGATION),
    /**
     * 契约字面量 {@code iam-platform:delegation:create}。
     */
    PLATFORM_DELEGATION_CREATE("iam-platform:delegation:create", Resources.IAM_PLATFORM_DELEGATION),
    /**
     * 契约字面量 {@code iam-platform:delegation:update}。
     */
    PLATFORM_DELEGATION_UPDATE("iam-platform:delegation:update", Resources.IAM_PLATFORM_DELEGATION),
    /**
     * 契约字面量 {@code iam-platform:delegation:revoke}。
     */
    PLATFORM_DELEGATION_DELETE("iam-platform:delegation:revoke", Resources.IAM_PLATFORM_DELEGATION),
    /**
     * 契约字面量 {@code iam-platform:delegation:preview}。
     */
    PLATFORM_DELEGATION_PREVIEW("iam-platform:delegation:preview", Resources.IAM_PLATFORM_DELEGATION),
    /**
     * 契约字面量 {@code iam-platform:authorization-diagnosis:run}。
     */
    PLATFORM_AUTHORIZATION_DIAGNOSE("iam-platform:authorization-diagnosis:run", Resources.IAM_PLATFORM_AUTHORIZATION_DIAGNOSIS),
    /**
     * 契约字面量 {@code iam-platform:authorization-audit:read}。
     */
    PLATFORM_AUDIT_READ("iam-platform:authorization-audit:read", Resources.IAM_PLATFORM_AUTHORIZATION_AUDIT),
    /**
     * 契约字面量 {@code iam-tenant:member:read}。
     */
    TENANT_MEMBER_READ("iam-tenant:member:read", Resources.IAM_TENANT_MEMBER),
    /**
     * 契约字面量 {@code iam-tenant:member:create}。
     */
    TENANT_MEMBER_CREATE("iam-tenant:member:create", Resources.IAM_TENANT_MEMBER),
    /**
     * 契约字面量 {@code iam-tenant:member:update}。
     */
    TENANT_MEMBER_UPDATE("iam-tenant:member:update", Resources.IAM_TENANT_MEMBER),
    /**
     * 暂停或恢复租户成员资格。
     */
    TENANT_MEMBER_STATUS("iam-tenant:member:change-status", Resources.IAM_TENANT_MEMBER),
    /**
     * 移出租户成员，不删除账号。
     */
    TENANT_MEMBER_REMOVE("iam-tenant:member:remove", Resources.IAM_TENANT_MEMBER),
    /**
     * 整体替换租户任职关系。
     */
    TENANT_MEMBER_DEPARTMENTS("iam-tenant:member:set-departments", Resources.IAM_TENANT_MEMBER),
    /**
     * 契约字面量 {@code iam-tenant:member:export}。
     */
    TENANT_MEMBER_EXPORT("iam-tenant:member:export", Resources.IAM_TENANT_MEMBER),
    /**
     * 契约字面量 {@code iam-tenant:department:read}。
     */
    TENANT_DEPARTMENT_READ("iam-tenant:department:read", Resources.IAM_TENANT_DEPARTMENT),
    /**
     * 契约字面量 {@code iam-tenant:department:create}。
     */
    TENANT_DEPARTMENT_CREATE("iam-tenant:department:create", Resources.IAM_TENANT_DEPARTMENT),
    /**
     * 契约字面量 {@code iam-tenant:department:update}。
     */
    TENANT_DEPARTMENT_UPDATE("iam-tenant:department:update", Resources.IAM_TENANT_DEPARTMENT),
    /**
     * 契约字面量 {@code iam-tenant:department:delete}。
     */
    TENANT_DEPARTMENT_DELETE("iam-tenant:department:delete", Resources.IAM_TENANT_DEPARTMENT),
    /** 独立维护部门负责人，不产生业务授权。 */
    TENANT_DEPARTMENT_LEADERS("iam-tenant:department:set-leaders", Resources.IAM_TENANT_DEPARTMENT),
    /**
     * 契约字面量 {@code iam-tenant:department:export}。
     */
    TENANT_DEPARTMENT_EXPORT("iam-tenant:department:export", Resources.IAM_TENANT_DEPARTMENT),
    /**
     * 契约字面量 {@code iam-tenant:settings:read}。
     */
    TENANT_SETTINGS_READ("iam-tenant:settings:read", Resources.IAM_TENANT_SETTINGS),
    /**
     * 契约字面量 {@code iam-tenant:settings:update}。
     */
    TENANT_SETTINGS_UPDATE("iam-tenant:settings:update", Resources.IAM_TENANT_SETTINGS),
    /**
     * 转交组织所有者，独立于设置更新。
     */
    TENANT_SETTINGS_OWNER_TRANSFER("iam-tenant:settings:transfer-owner", Resources.IAM_TENANT_SETTINGS),
    /**
     * 契约字面量 {@code iam-tenant:application:read}。
     */
    TENANT_APPLICATION_READ("iam-tenant:application:read", Resources.IAM_TENANT_APPLICATION),
    /** 查看当前组织工作台。 */
    TENANT_WORKBENCH_READ("iam-tenant:workbench:read", Resources.IAM_TENANT_WORKBENCH),
    /**
     * 契约字面量 {@code iam-tenant:application-audience:read}。
     */
    TENANT_AUDIENCE_READ("iam-tenant:application-audience:read", Resources.IAM_TENANT_APPLICATION_AUDIENCE),
    /**
     * 契约字面量 {@code iam-tenant:application-audience:update}。
     */
    TENANT_AUDIENCE_UPDATE("iam-tenant:application-audience:update", Resources.IAM_TENANT_APPLICATION_AUDIENCE),
    /**
     * 契约字面量 {@code iam-tenant:group:read}。
     */
    TENANT_GROUP_READ("iam-tenant:group:read", Resources.IAM_TENANT_GROUP),
    /**
     * 契约字面量 {@code iam-tenant:group:create}。
     */
    TENANT_GROUP_CREATE("iam-tenant:group:create", Resources.IAM_TENANT_GROUP),
    /**
     * 契约字面量 {@code iam-tenant:group:update}。
     */
    TENANT_GROUP_UPDATE("iam-tenant:group:update", Resources.IAM_TENANT_GROUP),
    /**
     * 契约字面量 {@code iam-tenant:group:delete}。
     */
    TENANT_GROUP_DELETE("iam-tenant:group:delete", Resources.IAM_TENANT_GROUP),
    /**
     * 契约字面量 {@code iam-tenant:group:preview}。
     */
    TENANT_GROUP_PREVIEW("iam-tenant:group:preview", Resources.IAM_TENANT_GROUP),
    /**
     * 契约字面量 {@code iam-tenant:role:read}。
     */
    TENANT_ROLE_READ("iam-tenant:role:read", Resources.IAM_TENANT_ROLE),
    /**
     * 契约字面量 {@code iam-tenant:role:create}。
     */
    TENANT_ROLE_CREATE("iam-tenant:role:create", Resources.IAM_TENANT_ROLE),
    /**
     * 契约字面量 {@code iam-tenant:role:change-status}。
     */
    TENANT_ROLE_STATUS("iam-tenant:role:change-status", Resources.IAM_TENANT_ROLE),
    /**
     * 契约字面量 {@code iam-tenant:role:delete}。
     */
    TENANT_ROLE_DELETE("iam-tenant:role:delete", Resources.IAM_TENANT_ROLE),
    /**
     * 契约字面量 {@code iam-tenant:role:update}。
     */
    TENANT_ROLE_UPDATE("iam-tenant:role:update", Resources.IAM_TENANT_ROLE),
    /**
     * 契约字面量 {@code iam-tenant:role:preview}。
     */
    TENANT_ROLE_PREVIEW("iam-tenant:role:preview", Resources.IAM_TENANT_ROLE),
    /**
     * 契约字面量 {@code iam-tenant:role-assignment:read}。
     */
    TENANT_ASSIGNMENT_READ("iam-tenant:role-assignment:read", Resources.IAM_TENANT_ROLE_ASSIGNMENT),
    /**
     * 契约字面量 {@code iam-tenant:role-assignment:create}。
     */
    TENANT_ASSIGNMENT_CREATE("iam-tenant:role-assignment:create", Resources.IAM_TENANT_ROLE_ASSIGNMENT),
    /**
     * 契约字面量 {@code iam-tenant:role-assignment:update}。
     */
    TENANT_ASSIGNMENT_UPDATE("iam-tenant:role-assignment:update", Resources.IAM_TENANT_ROLE_ASSIGNMENT),
    /**
     * 契约字面量 {@code iam-tenant:role-assignment:revoke}。
     */
    TENANT_ASSIGNMENT_DELETE("iam-tenant:role-assignment:revoke", Resources.IAM_TENANT_ROLE_ASSIGNMENT),
    /**
     * 契约字面量 {@code iam-tenant:field-policy:read}。
     */
    TENANT_FIELD_POLICY_READ("iam-tenant:field-policy:read", Resources.IAM_TENANT_FIELD_POLICY),
    /**
     * 契约字面量 {@code iam-tenant:field-policy:update}。
     */
    TENANT_FIELD_POLICY_UPDATE("iam-tenant:field-policy:update", Resources.IAM_TENANT_FIELD_POLICY),
    /**
     * 契约字面量 {@code iam-tenant:authorization-diagnosis:run}。
     */
    TENANT_AUTHORIZATION_DIAGNOSE("iam-tenant:authorization-diagnosis:run", Resources.IAM_TENANT_AUTHORIZATION_DIAGNOSIS),
    /**
     * 契约字面量 {@code iam-tenant:authorization-audit:read}。
     */
    TENANT_AUDIT_READ("iam-tenant:authorization-audit:read", Resources.IAM_TENANT_AUTHORIZATION_AUDIT),
    /**
     * 契约字面量 {@code iam-tenant:delegation:read}。
     */
    TENANT_DELEGATION_READ("iam-tenant:delegation:read", Resources.IAM_TENANT_DELEGATION),
    /**
     * 契约字面量 {@code iam-tenant:delegation:create}。
     */
    TENANT_DELEGATION_CREATE("iam-tenant:delegation:create", Resources.IAM_TENANT_DELEGATION),
    /**
     * 契约字面量 {@code iam-tenant:delegation:update}。
     */
    TENANT_DELEGATION_UPDATE("iam-tenant:delegation:update", Resources.IAM_TENANT_DELEGATION),
    /**
     * 契约字面量 {@code iam-tenant:delegation:revoke}。
     */
    TENANT_DELEGATION_DELETE("iam-tenant:delegation:revoke", Resources.IAM_TENANT_DELEGATION),
    /**
     * 契约字面量 {@code iam-tenant:delegation:preview}。
     */
    TENANT_DELEGATION_PREVIEW("iam-tenant:delegation:preview", Resources.IAM_TENANT_DELEGATION);

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
     * 契约字面量 {@code iam-platform:member:change-status}。
     */
    public static final String VALUE_PLATFORM_MEMBER_STATUS = "iam-platform:member:change-status";
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
     * 契约字面量 {@code platform-developer:id-allocation:read}。
     */
    public static final String VALUE_PLATFORM_ID_ALLOCATION_READ = "platform-developer:id-allocation:read";
    /**
     * 契约字面量 {@code platform-developer:id-allocation:create}。
     */
    public static final String VALUE_PLATFORM_ID_ALLOCATION_CREATE = "platform-developer:id-allocation:create";
    /**
     * 契约字面量 {@code platform-developer:id-allocation:update}。
     */
    public static final String VALUE_PLATFORM_ID_ALLOCATION_UPDATE = "platform-developer:id-allocation:update";
    /**
     * 契约字面量 {@code platform-developer:id-allocation:delete}。
     */
    public static final String VALUE_PLATFORM_ID_ALLOCATION_DELETE = "platform-developer:id-allocation:delete";
    /**
     * 契约字面量 {@code platform-developer:social-config:read}。
     */
    public static final String VALUE_PLATFORM_SOCIAL_CONFIG_READ = "platform-developer:social-config:read";
    /**
     * 契约字面量 {@code platform-developer:social-config:create}。
     */
    public static final String VALUE_PLATFORM_SOCIAL_CONFIG_CREATE = "platform-developer:social-config:create";
    /**
     * 契约字面量 {@code platform-developer:social-config:update}。
     */
    public static final String VALUE_PLATFORM_SOCIAL_CONFIG_UPDATE = "platform-developer:social-config:update";
    /**
     * 契约字面量 {@code platform-developer:social-config:delete}。
     */
    public static final String VALUE_PLATFORM_SOCIAL_CONFIG_DELETE = "platform-developer:social-config:delete";
    /**
     * 契约字面量 {@code platform-security:account-lockout-policy:read}。
     */
    public static final String VALUE_PLATFORM_LOCKOUT_POLICY_READ = "platform-security:account-lockout-policy:read";
    /**
     * 契约字面量 {@code platform-security:account-lockout-policy:update}。
     */
    public static final String VALUE_PLATFORM_LOCKOUT_POLICY_UPDATE = "platform-security:account-lockout-policy:update";
    /**
     * 契约字面量 {@code platform-security:credential-policy:read}。
     */
    public static final String VALUE_PLATFORM_CREDENTIAL_POLICY_READ = "platform-security:credential-policy:read";
    /**
     * 契约字面量 {@code platform-security:credential-policy:create}。
     */
    public static final String VALUE_PLATFORM_CREDENTIAL_POLICY_CREATE = "platform-security:credential-policy:create";
    /**
     * 契约字面量 {@code platform-security:credential-policy:update}。
     */
    public static final String VALUE_PLATFORM_CREDENTIAL_POLICY_UPDATE = "platform-security:credential-policy:update";
    /**
     * 契约字面量 {@code platform-security:credential-policy:delete}。
     */
    public static final String VALUE_PLATFORM_CREDENTIAL_POLICY_DELETE = "platform-security:credential-policy:delete";
    /**
     * 契约字面量 {@code platform-security:login-failure-policy:read}。
     */
    public static final String VALUE_PLATFORM_LOGIN_FAILURE_POLICY_READ = "platform-security:login-failure-policy:read";
    /**
     * 契约字面量 {@code platform-security:login-failure-policy:update}。
     */
    public static final String VALUE_PLATFORM_LOGIN_FAILURE_POLICY_UPDATE = "platform-security:login-failure-policy:update";
    /**
     * 契约字面量 {@code platform-security:session:read}。
     */
    public static final String VALUE_PLATFORM_SESSION_READ = "platform-security:session:read";
    /**
     * 契约字面量 {@code platform-security:session:revoke}。
     */
    public static final String VALUE_PLATFORM_SESSION_REVOKE = "platform-security:session:revoke";
    /**
     * 契约字面量 {@code platform-security:session-concurrency-policy:read}。
     */
    public static final String VALUE_PLATFORM_SESSION_POLICY_READ = "platform-security:session-concurrency-policy:read";
    /**
     * 契约字面量 {@code platform-security:session-concurrency-policy:update}。
     */
    public static final String VALUE_PLATFORM_SESSION_POLICY_UPDATE = "platform-security:session-concurrency-policy:update";
    /** 对应精确操作的编译期字面量。 */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_REFRESH = "platform-security:access-policy:refresh";
    /** 对应精确操作的编译期字面量。 */
    public static final String VALUE_PLATFORM_BLOCK_EVENT_READ = "platform-security:block-event:read";
    /** 对应精确操作的编译期字面量。 */
    public static final String VALUE_PLATFORM_SESSION_POLICY_CREATE = "platform-security:session-concurrency-policy:create";
    /** 对应精确操作的编译期字面量。 */
    public static final String VALUE_PLATFORM_SESSION_POLICY_DELETE = "platform-security:session-concurrency-policy:delete";
    /**
     * 契约字面量 {@code platform-security:access-policy:read}。
     */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_READ = "platform-security:access-policy:read";
    /**
     * 契约字面量 {@code platform-security:access-policy:create}。
     */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_CREATE = "platform-security:access-policy:create";
    /**
     * 契约字面量 {@code platform-security:access-policy:update}。
     */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_UPDATE = "platform-security:access-policy:update";
    /**
     * 契约字面量 {@code platform-security:access-policy:delete}。
     */
    public static final String VALUE_PLATFORM_SECURITY_POLICY_DELETE = "platform-security:access-policy:delete";
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
     * 契约字面量 {@code iam-platform:application-entitlement:read}。
     */
    public static final String VALUE_PLATFORM_ENTITLEMENT_READ = "iam-platform:application-entitlement:read";
    /**
     * 契约字面量 {@code iam-platform:application-entitlement:update}。
     */
    public static final String VALUE_PLATFORM_ENTITLEMENT_UPDATE = "iam-platform:application-entitlement:update";
    /**
     * 契约字面量 {@code iam-platform:application-entitlement:preview}。
     */
    public static final String VALUE_PLATFORM_ENTITLEMENT_PREVIEW = "iam-platform:application-entitlement:preview";
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
     * 契约字面量 {@code iam-platform:application:change-status}。
     */
    public static final String VALUE_PLATFORM_APPLICATION_STATUS = "iam-platform:application:change-status";
    /**
     * 契约字面量 {@code iam-platform:application:delete}。
     */
    public static final String VALUE_PLATFORM_APPLICATION_DELETE = "iam-platform:application:delete";
    /**
     * 契约字面量 {@code iam-platform:application:purge}。
     */
    public static final String VALUE_PLATFORM_APPLICATION_PURGE = "iam-platform:application:purge";
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
     * 契约字面量 {@code iam-platform:action:change-status}。
     */
    public static final String VALUE_PLATFORM_ACTION_STATUS = "iam-platform:action:change-status";
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
     * 契约字面量 {@code iam-platform:role:change-status}。
     */
    public static final String VALUE_PLATFORM_ROLE_STATUS = "iam-platform:role:change-status";
    /**
     * 契约字面量 {@code iam-platform:role:delete}。
     */
    public static final String VALUE_PLATFORM_ROLE_DELETE = "iam-platform:role:delete";
    /**
     * 契约字面量 {@code iam-platform:role:publish-version}。
     */
    public static final String VALUE_PLATFORM_ROLE_PUBLISH = "iam-platform:role:publish-version";
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
     * 契约字面量 {@code iam-platform:shared-role:change-status}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_STATUS = "iam-platform:shared-role:change-status";
    /**
     * 契约字面量 {@code iam-platform:shared-role:delete}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_DELETE = "iam-platform:shared-role:delete";
    /**
     * 契约字面量 {@code iam-platform:shared-role:update}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_UPDATE = "iam-platform:shared-role:update";
    /**
     * 契约字面量 {@code iam-platform:shared-role:preview}。
     */
    public static final String VALUE_PLATFORM_SHARED_ROLE_PREVIEW = "iam-platform:shared-role:preview";
    /**
     * 契约字面量 {@code iam-platform:role-assignment:read}。
     */
    public static final String VALUE_PLATFORM_ASSIGNMENT_READ = "iam-platform:role-assignment:read";
    /**
     * 契约字面量 {@code iam-platform:role-assignment:create}。
     */
    public static final String VALUE_PLATFORM_ASSIGNMENT_CREATE = "iam-platform:role-assignment:create";
    /**
     * 契约字面量 {@code iam-platform:role-assignment:update}。
     */
    public static final String VALUE_PLATFORM_ASSIGNMENT_UPDATE = "iam-platform:role-assignment:update";
    /**
     * 契约字面量 {@code iam-platform:role-assignment:revoke}。
     */
    public static final String VALUE_PLATFORM_ASSIGNMENT_DELETE = "iam-platform:role-assignment:revoke";
    /** 升级操作契约常量。 */
    public static final String VALUE_PLATFORM_ASSIGNMENT_UPGRADE = "iam-platform:role-assignment:apply-version";
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
     * 契约字面量 {@code iam-platform:delegation:revoke}。
     */
    public static final String VALUE_PLATFORM_DELEGATION_DELETE = "iam-platform:delegation:revoke";
    /**
     * 契约字面量 {@code iam-platform:delegation:preview}。
     */
    public static final String VALUE_PLATFORM_DELEGATION_PREVIEW = "iam-platform:delegation:preview";
    /**
     * 契约字面量 {@code iam-platform:authorization-diagnosis:run}。
     */
    public static final String VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE = "iam-platform:authorization-diagnosis:run";
    /**
     * 契约字面量 {@code iam-platform:authorization-audit:read}。
     */
    public static final String VALUE_PLATFORM_AUDIT_READ = "iam-platform:authorization-audit:read";
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
     * 契约字面量 {@code iam-tenant:member:change-status}。
     */
    public static final String VALUE_TENANT_MEMBER_STATUS = "iam-tenant:member:change-status";
    /**
     * 契约字面量 {@code iam-tenant:member:remove}。
     */
    public static final String VALUE_TENANT_MEMBER_REMOVE = "iam-tenant:member:remove";
    /**
     * 契约字面量 {@code iam-tenant:member:set-departments}。
     */
    public static final String VALUE_TENANT_MEMBER_DEPARTMENTS = "iam-tenant:member:set-departments";
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
    /** 部门负责人维护操作码。 */
    public static final String VALUE_TENANT_DEPARTMENT_LEADERS = "iam-tenant:department:set-leaders";
    /**
     * 契约字面量 {@code iam-tenant:department:export}。
     */
    public static final String VALUE_TENANT_DEPARTMENT_EXPORT = "iam-tenant:department:export";
    /**
     * 契约字面量 {@code iam-tenant:settings:read}。
     */
    public static final String VALUE_TENANT_SETTINGS_READ = "iam-tenant:settings:read";
    /**
     * 契约字面量 {@code iam-tenant:settings:update}。
     */
    public static final String VALUE_TENANT_SETTINGS_UPDATE = "iam-tenant:settings:update";
    /**
     * 契约字面量 {@code iam-tenant:settings:transfer-owner}。
     */
    public static final String VALUE_TENANT_SETTINGS_OWNER_TRANSFER = "iam-tenant:settings:transfer-owner";
    /**
     * 契约字面量 {@code iam-tenant:application:read}。
     */
    public static final String VALUE_TENANT_APPLICATION_READ = "iam-tenant:application:read";
    /** 工作台查看操作的编译期编码。 */
    public static final String VALUE_TENANT_WORKBENCH_READ = "iam-tenant:workbench:read";
    /**
     * 契约字面量 {@code iam-tenant:application-audience:read}。
     */
    public static final String VALUE_TENANT_AUDIENCE_READ = "iam-tenant:application-audience:read";
    /**
     * 契约字面量 {@code iam-tenant:application-audience:update}。
     */
    public static final String VALUE_TENANT_AUDIENCE_UPDATE = "iam-tenant:application-audience:update";
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
     * 契约字面量 {@code iam-tenant:role:change-status}。
     */
    public static final String VALUE_TENANT_ROLE_STATUS = "iam-tenant:role:change-status";
    /**
     * 契约字面量 {@code iam-tenant:role:delete}。
     */
    public static final String VALUE_TENANT_ROLE_DELETE = "iam-tenant:role:delete";
    /**
     * 契约字面量 {@code iam-tenant:role:update}。
     */
    public static final String VALUE_TENANT_ROLE_UPDATE = "iam-tenant:role:update";
    /**
     * 契约字面量 {@code iam-tenant:role:preview}。
     */
    public static final String VALUE_TENANT_ROLE_PREVIEW = "iam-tenant:role:preview";
    /**
     * 契约字面量 {@code iam-tenant:role-assignment:read}。
     */
    public static final String VALUE_TENANT_ASSIGNMENT_READ = "iam-tenant:role-assignment:read";
    /**
     * 契约字面量 {@code iam-tenant:role-assignment:create}。
     */
    public static final String VALUE_TENANT_ASSIGNMENT_CREATE = "iam-tenant:role-assignment:create";
    /**
     * 契约字面量 {@code iam-tenant:role-assignment:update}。
     */
    public static final String VALUE_TENANT_ASSIGNMENT_UPDATE = "iam-tenant:role-assignment:update";
    /**
     * 契约字面量 {@code iam-tenant:role-assignment:revoke}。
     */
    public static final String VALUE_TENANT_ASSIGNMENT_DELETE = "iam-tenant:role-assignment:revoke";
    /**
     * 契约字面量 {@code iam-tenant:field-policy:read}。
     */
    public static final String VALUE_TENANT_FIELD_POLICY_READ = "iam-tenant:field-policy:read";
    /**
     * 契约字面量 {@code iam-tenant:field-policy:update}。
     */
    public static final String VALUE_TENANT_FIELD_POLICY_UPDATE = "iam-tenant:field-policy:update";
    /**
     * 契约字面量 {@code iam-tenant:authorization-diagnosis:run}。
     */
    public static final String VALUE_TENANT_AUTHORIZATION_DIAGNOSE = "iam-tenant:authorization-diagnosis:run";
    /**
     * 契约字面量 {@code iam-tenant:authorization-audit:read}。
     */
    public static final String VALUE_TENANT_AUDIT_READ = "iam-tenant:authorization-audit:read";
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
     * 契约字面量 {@code iam-tenant:delegation:revoke}。
     */
    public static final String VALUE_TENANT_DELEGATION_DELETE = "iam-tenant:delegation:revoke";
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

    /** 服务器注册的完整资源身份，与数据库目录关系核对。 */
    private final ResourceKey resource;

    IamAction(String code, ResourceKey resource) {
        this.resource = resource;
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
    /** <p>为内置操作显式登记归属，权限码文本不承担资源解析。</p> */
    private static final class Resources {
        private static final ResourceKey IAM_PLATFORM_ACCOUNT =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "account");
        private static final ResourceKey IAM_PLATFORM_ACTION =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "action");
        private static final ResourceKey IAM_PLATFORM_APPLICATION =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "application");
        private static final ResourceKey IAM_PLATFORM_APPLICATION_ENTITLEMENT =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "application-entitlement");
        private static final ResourceKey IAM_PLATFORM_AUTHORIZATION_AUDIT =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "authorization-audit");
        private static final ResourceKey IAM_PLATFORM_AUTHORIZATION_DIAGNOSIS =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "authorization-diagnosis");
        private static final ResourceKey IAM_PLATFORM_DELEGATION =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "delegation");
        private static final ResourceKey IAM_PLATFORM_DICTIONARY =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "dictionary");
        private static final ResourceKey IAM_PLATFORM_GROUP =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "group");
        private static final ResourceKey IAM_PLATFORM_MEMBER =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "member");
        private static final ResourceKey IAM_PLATFORM_MENU =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "menu");
        private static final ResourceKey IAM_PLATFORM_PLAN =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "plan");
        private static final ResourceKey IAM_PLATFORM_RESOURCE =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "resource");
        private static final ResourceKey IAM_PLATFORM_ROLE =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "role");
        private static final ResourceKey IAM_PLATFORM_ROLE_ASSIGNMENT =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "role-assignment");
        private static final ResourceKey IAM_PLATFORM_SHARED_ROLE =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "shared-role");
        private static final ResourceKey IAM_PLATFORM_TENANT =
                new ResourceKey(AuthorizationDomain.PLATFORM, "iam-platform", "tenant");
        private static final ResourceKey IAM_TENANT_APPLICATION =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "application");
        private static final ResourceKey IAM_TENANT_APPLICATION_AUDIENCE =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "application-audience");
        private static final ResourceKey IAM_TENANT_AUTHORIZATION_AUDIT =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "authorization-audit");
        private static final ResourceKey IAM_TENANT_AUTHORIZATION_DIAGNOSIS =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "authorization-diagnosis");
        private static final ResourceKey IAM_TENANT_DELEGATION =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "delegation");
        private static final ResourceKey IAM_TENANT_DEPARTMENT =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "department");
        private static final ResourceKey IAM_TENANT_FIELD_POLICY =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "field-policy");
        private static final ResourceKey IAM_TENANT_GROUP =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "group");
        private static final ResourceKey IAM_TENANT_MEMBER =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "member");
        private static final ResourceKey IAM_TENANT_ROLE =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "role");
        private static final ResourceKey IAM_TENANT_ROLE_ASSIGNMENT =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "role-assignment");
        private static final ResourceKey IAM_TENANT_SETTINGS =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "settings");
        private static final ResourceKey IAM_TENANT_WORKBENCH =
                new ResourceKey(AuthorizationDomain.TENANT, "iam-tenant", "workbench");
        private static final ResourceKey PLATFORM_DEVELOPER_ID_ALLOCATION =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-developer", "id-allocation");
        private static final ResourceKey PLATFORM_DEVELOPER_SOCIAL_CONFIG =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-developer", "social-config");
        private static final ResourceKey PLATFORM_SECURITY_ACCESS_POLICY =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-security", "access-policy");
        private static final ResourceKey PLATFORM_SECURITY_ACCOUNT_LOCKOUT_POLICY =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-security", "account-lockout-policy");
        private static final ResourceKey PLATFORM_SECURITY_BLOCK_EVENT =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-security", "block-event");
        private static final ResourceKey PLATFORM_SECURITY_CREDENTIAL_POLICY =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-security", "credential-policy");
        private static final ResourceKey PLATFORM_SECURITY_LOGIN_FAILURE_POLICY =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-security", "login-failure-policy");
        private static final ResourceKey PLATFORM_SECURITY_SESSION =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-security", "session");
        private static final ResourceKey PLATFORM_SECURITY_SESSION_CONCURRENCY_POLICY =
                new ResourceKey(AuthorizationDomain.PLATFORM, "platform-security", "session-concurrency-policy");
    }
}
