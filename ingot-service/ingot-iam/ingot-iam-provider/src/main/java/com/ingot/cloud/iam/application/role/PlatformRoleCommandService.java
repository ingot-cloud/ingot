package com.ingot.cloud.iam.application.role;

import java.math.BigInteger;
import java.util.Map;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.cloud.iam.application.shared.model.ConfigurationStatusInput;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.role.model.RoleCreateInput;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.cloud.iam.application.role.model.RoleUpdateInput;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>执行对应平台角色用例，保留固定版本、字段权限和乐观锁规则。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformRoleCommandService {
    private final PlatformRoleOperations operationsContext;

    /**
     * 创建角色并发布首个不可变版本，不能创建系统治理角色。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param input 创建命令
     * @return 新角色 ID
     */
    public CreatedResource create(AuthorizationDomain domain, boolean shared, RoleCreateInput input) {
        ActiveIdentity actor = domain == AuthorizationDomain.PLATFORM
                ? operationsContext.access().requireGoverned(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.CREATE))
                : operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.CREATE));
        RoleKind kind = requireCreatable(domain, shared, input.kind());
        if (domain == AuthorizationDomain.PLATFORM && input.code() != null
                && com.ingot.framework.commons.constants.RoleConstants.ROLE_ADMIN_CODE.equalsIgnoreCase(input.code().strip())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "系统超级管理员编码不可用于自定义角色");
        }
        return operationsContext.transaction().execute(status -> {
            operationsContext.roles().lockAuthorization(domain);
            if (domain == AuthorizationDomain.PLATFORM) {
                operationsContext.access().requireGoverned(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.CREATE));
            }
            long id = operationsContext.access().nextId();
            Long tenantId = kind == RoleKind.TENANT_CUSTOM ? IamIds.require(actor.context().tenantId()) : null;
            IamRoleDefinitionEntity entity = new IamRoleDefinitionEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setDomain(
                    kind == RoleKind.PLATFORM_CUSTOM ? AuthorizationDomain.PLATFORM : AuthorizationDomain.TENANT);
            entity.setTenantId(tenantId == null ? null : BigInteger.valueOf(tenantId));
            entity.setKind(kind);
            entity.setCode(input.code());
            entity.setName(input.name());
            entity.setDescription(input.description());
            entity.setGroupName(input.groupName());
            entity.setEnabled(true);
            try {
                operationsContext.roles().insertDefinition(entity);
            }
            catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "角色编码已存在");
            }
            operationsContext.publishRevision(id, kind, 1, input.baseRevisionId(), input.definition(), input.versionName(), input.remark());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformRoleOperations.ROLE, IamIds.text(id), AuditChangeType.CREATE, Map.of(),
                    Map.of(AuditField.NAME, input.name()), Map.of(PlatformRoleOperations.ROLE, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 启停角色，停用约束所有旧版本。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    public CreatedResource changeStatus(AuthorizationDomain domain, boolean shared, String id,
            ConfigurationStatusInput input) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.STATUS));
        long roleId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            operationsContext.roles().lockAuthorization(domain);
            PlatformRoleOperations.RoleRow current = operationsContext.lock(domain, shared, actor, roleId);
            if (current.kind() == RoleKind.SYSTEM) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            IamIds.requireVersion(input.expectedVersion(), PlatformRoleOperations.version(current.version()));
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            operationsContext.roles().updateEnabled(roleId, enabled, current.version());
            String next = PlatformRoleOperations.nextVersion(current.version());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformRoleOperations.ROLE, id,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, PlatformRoleOperations.statusOf(current.enabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(PlatformRoleOperations.ROLE, next));
            operationsContext.changes().markAll();
            return new CreatedResource(id, next);
        });
    }

    /**
     * 更新角色名称、说明、分组与启停，编码与已发布版本不变。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 待写入的基本信息
     * @return 提交后版本
     */
    public CreatedResource updateProfile(AuthorizationDomain domain, boolean shared, String id, RoleUpdateInput input) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.STATUS));
        long roleId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            operationsContext.roles().lockAuthorization(domain);
            PlatformRoleOperations.RoleRow current = operationsContext.lock(domain, shared, actor, roleId);
            if (current.kind() == RoleKind.SYSTEM) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            IamIds.requireVersion(input.expectedVersion(), PlatformRoleOperations.version(current.version()));
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            String name = input.name().trim();
            String description = blankToNull(input.description());
            String groupName = blankToNull(input.groupName());
            operationsContext.roles().updateProfile(roleId, name, description, groupName, enabled, current.version());
            String next = PlatformRoleOperations.nextVersion(current.version());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformRoleOperations.ROLE, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.name(), AuditField.STATUS, PlatformRoleOperations.statusOf(current.enabled()).name()),
                    Map.of(AuditField.NAME, name, AuditField.STATUS, input.status().name()), Map.of(PlatformRoleOperations.ROLE, next));
            operationsContext.changes().markAll();
            return new CreatedResource(id, next);
        });
    }

    /**
     * 删除未被授权引用的非系统角色。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @return 删除前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, boolean shared, String id) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.DELETE));
        long roleId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            operationsContext.roles().lockAuthorization(domain);
            PlatformRoleOperations.RoleRow current = operationsContext.lock(domain, shared, actor, roleId);
            if (current.kind() == RoleKind.SYSTEM) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            if (operationsContext.roles().countAssignments(roleId) > 0 || current.kind() == RoleKind.PLATFORM_CUSTOM && operationsContext.roles().countApplications(roleId) > 0) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            operationsContext.roles().deleteRevisions(roleId);
            operationsContext.roles().deleteDefinition(roleId);
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformRoleOperations.ROLE, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.name()), Map.of(), Map.of(PlatformRoleOperations.ROLE, PlatformRoleOperations.version(current.version())));
            operationsContext.changes().markAll();
            return new CreatedResource(id, PlatformRoleOperations.version(current.version()));
        });
    }

    private static RoleKind requireCreatable(AuthorizationDomain domain, boolean shared, RoleKind kind) {
        if (kind == RoleKind.SYSTEM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (shared && kind != RoleKind.SHARED) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (!shared && domain == AuthorizationDomain.PLATFORM && kind != RoleKind.PLATFORM_CUSTOM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (!shared && domain == AuthorizationDomain.TENANT && kind != RoleKind.TENANT_CUSTOM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return kind;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
