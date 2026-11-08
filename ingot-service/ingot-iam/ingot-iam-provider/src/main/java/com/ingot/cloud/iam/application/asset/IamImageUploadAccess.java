package com.ingot.cloud.iam.application.asset;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.CatalogRepository;
import com.ingot.cloud.iam.infrastructure.persistence.MemberRepository;
import com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.authorization.field.FieldWriteExecutor;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImageUploadPurpose;
import com.ingot.cloud.iam.domain.organization.MemberDepartmentView;
import com.ingot.cloud.iam.domain.policy.MemberFieldKey;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>把图片用途绑定到 IAM 现有的业务写操作和可信目标。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class IamImageUploadAccess {
    private final IamAccess access;
    private final ResourceScopeAccessService scopes;
    private final CatalogRepository catalog;
    private final TenantRepository tenants;
    private final MemberRepository members;
    private final IamTenantMemberMapper tenantMembers;
    private final FieldWriteExecutor fields;

    /**
     * 校验上传准入。创建对象仍须在实际业务提交时按完整草稿复核范围和字段。
     * @param purpose 服务声明的用途
     * @param targetId 编辑或重新加入时的既有目标
     * @return 当前可信上传者
     */
    @Transactional(readOnly = true)
    public ActiveIdentity require(ImageUploadPurpose purpose, String targetId) {
        boolean targeted = switch (purpose) {
            case PLATFORM_MEMBER_UPDATE, TENANT_MEMBER_REJOIN, TENANT_MEMBER_UPDATE,
                    PLATFORM_TENANT_UPDATE, APPLICATION_UPDATE -> true;
            default -> false;
        };
        if (targeted != (targetId != null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var domain = switch (purpose) {
            case TENANT_MEMBER_CREATE, TENANT_MEMBER_REJOIN, TENANT_MEMBER_UPDATE, TENANT_SETTINGS_UPDATE ->
                    AuthorizationDomain.TENANT;
            default -> AuthorizationDomain.PLATFORM;
        };
        IamAction action = switch (purpose) {
            case PLATFORM_MEMBER_CREATE -> IamAction.PLATFORM_MEMBER_CREATE;
            case PLATFORM_MEMBER_UPDATE -> IamAction.PLATFORM_MEMBER_UPDATE;
            case TENANT_MEMBER_CREATE, TENANT_MEMBER_REJOIN -> IamAction.TENANT_MEMBER_CREATE;
            case TENANT_MEMBER_UPDATE -> IamAction.TENANT_MEMBER_UPDATE;
            case PLATFORM_TENANT_CREATE -> IamAction.PLATFORM_TENANT_CREATE;
            case PLATFORM_TENANT_UPDATE -> IamAction.PLATFORM_TENANT_UPDATE;
            case TENANT_SETTINGS_UPDATE -> IamAction.TENANT_SETTINGS_UPDATE;
            case APPLICATION_CREATE -> IamAction.PLATFORM_APPLICATION_CREATE;
            case APPLICATION_UPDATE -> IamAction.PLATFORM_APPLICATION_UPDATE;
            default -> throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        };
        ActiveIdentity actor = access.require(domain, action);
        switch (purpose) {
            case PLATFORM_MEMBER_CREATE -> requireMemberField(actor, action, null, List.of());
            case PLATFORM_MEMBER_UPDATE -> {
                long id = IamIds.require(targetId);
                scopes.requirePlatformMemberUpdate(actor.context(), id);
                requireMemberField(actor, action, IamIds.text(id), List.of());
            }
            case TENANT_MEMBER_UPDATE -> {
                long id = IamIds.require(targetId);
                scopes.requireVisibleMember(actor.context(), action, id);
                long tenant = IamIds.require(actor.context().tenantId());
                BigInteger member = BigInteger.valueOf(id);
                List<String> departments = members.departmentViews(tenant, List.of(member))
                        .getOrDefault(member, List.of()).stream().map(MemberDepartmentView::id).toList();
                requireMemberField(actor, action, IamIds.text(id), departments);
            }
            case TENANT_MEMBER_REJOIN -> {
                if (!tenantMembers.exists(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                        .eq(IamTenantMemberEntity::getTenantId, IamIds.require(actor.context().tenantId()))
                        .eq(IamTenantMemberEntity::getId, IamIds.require(targetId))
                        .eq(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED))) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                }
            }
            case PLATFORM_TENANT_UPDATE, APPLICATION_UPDATE -> {
                long id = IamIds.require(targetId);
                var scope = scopes.objectsForExecution(actor.context(), action);
                if (!ObjectScopeSql.matches(scope, id)
                        || (purpose == ImageUploadPurpose.APPLICATION_UPDATE
                            ? catalog.findApplication(id) == null : tenants.findActive(id) == null)) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                }
            }
            default -> {
                // 新建与当前组织场景没有客户端目标 ID。
            }
        }
        return actor;
    }

    private void requireMemberField(ActiveIdentity actor, IamAction action, String member, List<String> departments) {
        var key = actor.context().domain() == AuthorizationDomain.TENANT
                ? BuiltinResourceProviders.TENANT_MEMBER_RESOURCE : BuiltinResourceProviders.PLATFORM_MEMBER_RESOURCE;
        fields.require(key, action.getCode(), new ScopeTarget(member, member, actor.context().tenantId(), departments),
                Map.of(MemberFieldKey.VALUE_AVATAR, Boolean.TRUE));
    }
}
