package com.ingot.cloud.iam.application.organization;

import com.ingot.cloud.iam.application.asset.IamImageBinding;
import com.ingot.framework.commons.model.iam.ImageUploadPurpose;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade;
import com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService;
import com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders;
import com.ingot.framework.authorization.FieldPolicyProcessor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.group.GroupService;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.MemberRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.organization.MemberCreateInput;
import com.ingot.cloud.iam.domain.organization.MemberDepartmentBinding;
import com.ingot.cloud.iam.domain.organization.MemberDepartmentView;
import com.ingot.cloud.iam.domain.policy.MemberFieldKey;
import com.ingot.cloud.iam.application.organization.model.PlatformMemberEditInput;
import com.ingot.cloud.iam.application.organization.model.PlatformMemberEditPreview;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.shared.model.ImpactSummary;
import com.ingot.cloud.iam.application.organization.model.MemberProfileInput;
import com.ingot.cloud.iam.application.organization.model.MemberRecord;
import com.ingot.cloud.iam.application.organization.model.MemberRoleReplaceInput;
import com.ingot.cloud.iam.application.organization.model.MemberRoleView;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


/**
 * <p>在既有事务和授权锁内创建成员、更新资料及维护角色，草稿预览不替代提交重验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class MemberProfileCommandService {

    private static final ResourceKey PLATFORM_MEMBER_RESOURCE = BuiltinResourceProviders.PLATFORM_MEMBER_RESOURCE;

    private final RoleFieldPermissionService platformFields;
    private final com.ingot.framework.authorization.field.FieldProjectionEngine projection;
    private final com.ingot.framework.authorization.field.FieldWriteExecutor fieldWrites;

    private final IamAccess access;

    private final ResourceScopeAccessService scopes;

    private final AuthorizationAuditWriter audits;

    private final MemberRepository members;

    private final PlatformAssignmentFacade assignments;

    private final GroupService groupCommands;

    private final TransactionTemplate transaction;
    private final IamImageBinding images;
    private final MemberQueryService queries;


    /**
     * 用同一数据源的事务管理器建立事务模板，保持原成员写入的事务选择语义。
     * <p>事务模板需要初始化，使用显式构造器而不依赖额外 TransactionTemplate Bean。</p>
     */
    public MemberProfileCommandService(RoleFieldPermissionService platformFields,
            com.ingot.framework.authorization.field.FieldProjectionEngine projection,
            com.ingot.framework.authorization.field.FieldWriteExecutor fieldWrites,
            IamAccess access,
            ResourceScopeAccessService scopes,
            AuthorizationAuditWriter audits,
            MemberRepository members,
            PlatformAssignmentFacade assignments,
            GroupService groupCommands,
            PlatformTransactionManager transactionManager,
            IamImageBinding images,
            MemberQueryService queries) {
        this.platformFields = platformFields;
        this.projection = projection;
        this.fieldWrites = fieldWrites;
        this.access = access;
        this.scopes = scopes;
        this.audits = audits;
        this.members = members;
        this.assignments = assignments;
        this.groupCommands = groupCommands;
        this.transaction = new TransactionTemplate(transactionManager);
        this.images = images;
        this.queries = queries;
    }


    /**
     * 替换平台成员的简单直接角色。
     * @param memberId 平台成员 ID
     * @param input 目标角色 ID
     * @return 替换后的直接角色
     */
    public List<MemberRoleView> replaceDirectRoles(String memberId, MemberRoleReplaceInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_UPDATE);
        long id = IamIds.require(memberId);
        scopes.requireVisibleMember(actor.context(), IamAction.PLATFORM_MEMBER_UPDATE, id);
        return assignments.replaceDirectRoles(AuthorizationDomain.PLATFORM, memberId, input.roleIds());
    }

    /**
     * 把已有账号关联为当前域成员。
     * @param domain 接口管理域
     * @param input 账号与任职
     * @return 新成员 ID
     */
    public CreatedResource create(AuthorizationDomain domain, MemberCreateInput input) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_CREATE : IamAction.TENANT_MEMBER_CREATE);
        if (domain == AuthorizationDomain.PLATFORM && (input.hasUnknownFields() || !input.departments().isEmpty())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (domain != AuthorizationDomain.PLATFORM
                && (!input.roleIds().isEmpty() || !input.groupIds().isEmpty() || !input.roleAssignments().isEmpty())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        Set<String> selectedRoleIds = new LinkedHashSet<>(input.roleIds());
        if (input.roleAssignments().stream().anyMatch(draft -> draft == null || !selectedRoleIds.add(draft.roleId()))) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "角色不能重复选择");
        }
        Set<String> departmentIds = new LinkedHashSet<>();
        Map<String, Boolean> departmentBindings = new LinkedHashMap<>();
        if (input.departments() != null) {
            for (MemberDepartmentBinding binding : input.departments()) {
                departmentIds.add(binding.id());
                departmentBindings.put(binding.id(), binding.primary());
            }
        }
        long accountId = IamIds.require(input.accountId());
        return transaction.execute(status -> {
            assignments.lockAuthorization(domain);
            access.require(domain, domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_CREATE
                    : IamAction.TENANT_MEMBER_CREATE);
            scopes.requireCreate(actor.context(), domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_MEMBER_CREATE : IamAction.TENANT_MEMBER_CREATE, departmentIds);
            if (!members.activeAccount(accountId)) {
                throw new BizException(IamReasonCode.IDENTITY_INVALID);
            }
            long id = access.nextId();
            var resource = domain == AuthorizationDomain.PLATFORM ? PLATFORM_MEMBER_RESOURCE : BuiltinResourceProviders.TENANT_MEMBER_RESOURCE;
            var submitted = projection.submitted(input, input.suppliedFields(), resource).getOrDefault(resource, Map.of());
            fieldWrites.require(resource, domain == AuthorizationDomain.PLATFORM ? IamAction.VALUE_PLATFORM_MEMBER_CREATE : IamAction.VALUE_TENANT_MEMBER_CREATE,
                    new ScopeTarget(IamIds.text(id), IamIds.text(id), actor.context().tenantId(), List.copyOf(departmentIds)), submitted);
            String displayName = input.displayName() == null || input.displayName().isBlank() ? "成员"
                    : input.displayName().trim();
            String avatar = images.bind(actor.context(), domain == AuthorizationDomain.PLATFORM
                    ? ImageUploadPurpose.PLATFORM_MEMBER_CREATE : ImageUploadPurpose.TENANT_MEMBER_CREATE,
                    null, input.avatar(), null);
            try {
                if (domain == AuthorizationDomain.PLATFORM) {
                    members.insertPlatform(id, accountId, displayName, avatar);
                }
                else {
                    long tenantId = IamIds.require(actor.context().tenantId());
                    members.insertTenant(id, tenantId, accountId, displayName, avatar);
                    replaceDepartments(tenantId, id, departmentBindings);
                }
            }
            catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "成员资格已存在");
            }
            audits.write(actor.context(), access.nextId(), "member", IamIds.text(id), AuditChangeType.CREATE, Map.of(),
                    Map.of(AuditField.NAME, displayName), Map.of("member", "0"));
            if (domain == AuthorizationDomain.PLATFORM) {
                if (!input.roleIds().isEmpty()) {
                    assignments.grantDirectRoles(domain, IamIds.text(id), input.roleIds());
                }
                if (!input.roleAssignments().isEmpty()) {
                    assignments.grantMemberRoleAssignments(IamIds.text(id), input.roleAssignments());
                }
                if (!input.groupIds().isEmpty()) {
                    access.require(domain, IamAction.PLATFORM_GROUP_UPDATE);
                    groupCommands.addPlatformMemberToGroups(id, input.groupIds());
                }
            }
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新当前域成员资料，不修改全局账号或成员资格。手机号和邮箱写入当前域成员的联系字段，空引用表示不修改，空白表示清空。不可编辑字段仍拒绝写入。
     * @param domain 接口管理域
     * @param memberId 成员 ID
     * @param input 资料
     * @return 更新后投影
     */
    public ResourceDetail<MemberRecord> patch(AuthorizationDomain domain, String memberId, MemberProfileInput input) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_UPDATE : IamAction.TENANT_MEMBER_UPDATE);
        long id = IamIds.require(memberId);
        return transaction.execute(status -> {
            assignments.lockAuthorization(domain);
            access.require(domain, domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_UPDATE
                    : IamAction.TENANT_MEMBER_UPDATE);
            BigInteger version = lock(domain, actor, id);
            if (domain == AuthorizationDomain.PLATFORM)
                scopes.requirePlatformMemberUpdate(actor.context(), id);
            else
                scopes.requireVisibleMember(actor.context(), IamAction.TENANT_MEMBER_UPDATE, id);
            ResourceDetail<MemberRecord> current = queries.load(domain, actor, id);
            IamIds.requireVersion(input.expectedVersion(), version.toString());
            var resource = domain == AuthorizationDomain.PLATFORM ? PLATFORM_MEMBER_RESOURCE
                    : BuiltinResourceProviders.TENANT_MEMBER_RESOURCE;
            var submitted = projection.submitted(input, input.suppliedFields(), resource).getOrDefault(resource, Map.of());
            if (input.suppliedFields().contains(MemberFieldKey.VALUE_DISPLAY_NAME)
                    && (input.displayName() == null || input.displayName().isBlank()))
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            var actualDepartments = domain == AuthorizationDomain.PLATFORM ? List.<String>of()
                    : members.departmentViews(IamIds.require(actor.context().tenantId()), List.of(BigInteger.valueOf(id)))
                        .getOrDefault(BigInteger.valueOf(id), List.of()).stream().map(MemberDepartmentView::id).toList();
            fieldWrites.require(resource, domain == AuthorizationDomain.PLATFORM ? IamAction.VALUE_PLATFORM_MEMBER_UPDATE
                    : IamAction.VALUE_TENANT_MEMBER_UPDATE,
                    new ScopeTarget(memberId, memberId, actor.context().tenantId(), actualDepartments), submitted);
            String avatar = images.bind(actor.context(), domain == AuthorizationDomain.PLATFORM
                    ? ImageUploadPurpose.PLATFORM_MEMBER_UPDATE : ImageUploadPurpose.TENANT_MEMBER_UPDATE,
                    IamIds.text(id), input.avatar(), current.record().avatar());
            if (domain == AuthorizationDomain.PLATFORM) {
                requireApplied(members.updatePlatform(id, input.displayName(), avatar, input.phone(),
                        input.email(), version, input.suppliedFields()));
            }
            else {
                long tenantId = IamIds.require(actor.context().tenantId());
                long viewerId = IamIds.require(actor.context().memberId());
                requireApplied(members.updateTenant(tenantId, id, input.displayName(), avatar, input.phone(),
                        input.email(), version, input.suppliedFields()));
            }
            ResourceDetail<MemberRecord> next = queries.load(domain, actor, id);
            audits.write(actor.context(), access.nextId(), "member", memberId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, String.valueOf(current.record().displayName())),
                    Map.of(AuditField.NAME, String.valueOf(next.record().displayName())),
                    Map.of("member", next.version()));
            return next;
        });
    }

    private void validatePlatformProfile(ActiveIdentity actor, String memberId, MemberProfileInput input) {
        var submitted = projection.submitted(input, input.suppliedFields(), PLATFORM_MEMBER_RESOURCE)
                .getOrDefault(PLATFORM_MEMBER_RESOURCE, Map.of());
        var writePolicy = platformFields.previewAll(PLATFORM_MEMBER_RESOURCE, actor.context(),
                List.of(IamAction.PLATFORM_MEMBER_UPDATE)).get(IamAction.PLATFORM_MEMBER_UPDATE);
        FieldPolicyProcessor.requireWritable(submitted,
                FieldPolicyProcessor.access(writePolicy, new ScopeTarget(memberId, memberId, null, List.of())));
    }
    /**
     * 只读预览成员资料及角色差量，预览不替代最终事务内重验。
     * @param memberId 成员
     * @param input 平台编辑草稿
     * @return 变化摘要与阻断问题
     */
    public Preview<PlatformMemberEditPreview>
            previewPlatformEdit(String memberId, PlatformMemberEditInput input) {
        var actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_UPDATE);
        scopes.requirePlatformMemberUpdate(actor.context(), IamIds.require(memberId));
        var current = queries.load(AuthorizationDomain.PLATFORM, actor, IamIds.require(memberId));
        IamIds.requireVersion(input.expectedVersion(), current.version());
        validatePlatformProfile(actor, memberId, input.profile());
        var errors = assignments.previewMemberRoleChanges(memberId, input.roleChanges());
        var changes = input.roleChanges();
        var result = new PlatformMemberEditPreview(
                input.suppliedFields().stream().filter(key -> !"expectedVersion".equals(key)).sorted().toList(),
                changes == null ? 0 : changes.additions().size(), changes == null ? 0 : changes.updates().size(),
                changes == null ? 0 : changes.removals().size());
        return new Preview<>(current.version(), errors.isEmpty(), errors,
                List.of(), new ImpactSummary(null,
                (long) result.additions() + result.updates() + result.removals(), null, false), result);
    }

    /**
     * 同一事务保存资料和角色差量，失败整批回滚。
     * @param memberId 成员
     * @param input 平台编辑草稿
     * @return 更新后的安全投影
     */
    public ResourceDetail<MemberRecord> patchPlatform(String memberId,
            PlatformMemberEditInput input) {
        return transaction.execute(status -> {
            var actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_UPDATE);
            assignments.lockAuthorization(AuthorizationDomain.PLATFORM);
            // patch 先取得成员行锁并校验字段，所有分配写入参与此事务。
            patch(AuthorizationDomain.PLATFORM, memberId, input.profile());
            assignments.applyMemberRoleChanges(memberId, input.roleChanges());
            return queries.load(AuthorizationDomain.PLATFORM, actor, IamIds.require(memberId));
        });
    }

    /**
     * 在事务内锁定成员行，后续可见性、字段策略与条件更新都基于这一份状态。
     */
    private BigInteger lock(AuthorizationDomain domain, ActiveIdentity actor, long memberId) {
        BigInteger version = domain == AuthorizationDomain.PLATFORM ? version(members.lockPlatform(memberId))
                : version(members.lockTenant(IamIds.require(actor.context().tenantId()), memberId));
        if (version == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return version;
    }

    private static BigInteger version(IamPlatformMemberEntity row) {
        return row == null ? null : row.getVersion() == null ? BigInteger.ZERO : row.getVersion();
    }

    private static BigInteger version(IamTenantMemberEntity row) {
        return row == null ? null : row.getVersion() == null ? BigInteger.ZERO : row.getVersion();
    }

    private static void requireApplied(int rows) {
        if (rows != 1) {
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
        }
    }

    private void replaceDepartments(long tenantId, long memberId, Map<String, Boolean> departments) {
        for (String departmentId : departments.keySet()) {
            if (!members.lockDepartment(tenantId, IamIds.require(departmentId))) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
        members.replaceDepartments(tenantId, memberId, departments);
    }
}
