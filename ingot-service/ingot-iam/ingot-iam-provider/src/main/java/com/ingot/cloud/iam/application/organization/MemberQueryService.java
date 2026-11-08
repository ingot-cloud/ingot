package com.ingot.cloud.iam.application.organization;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade;
import com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService;
import com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders;
import com.ingot.framework.authorization.FieldPolicyProcessor;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import com.ingot.cloud.iam.application.authorization.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.GroupRepository;
import com.ingot.cloud.iam.infrastructure.persistence.MemberRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformGroupEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantGroupEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.application.policy.FieldPolicySnapshot;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.cloud.iam.application.organization.model.PlatformMemberContext;
import com.ingot.cloud.iam.application.group.model.GroupRecord;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.organization.MemberDepartmentView;
import com.ingot.cloud.iam.domain.policy.MemberFieldKey;
import com.ingot.cloud.iam.application.organization.model.MemberRecord;
import com.ingot.cloud.iam.application.organization.model.MemberRoleView;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import com.ingot.cloud.iam.domain.organization.MemberTimeFields;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.iam.Selection;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * <p>查询成员、组及角色并按字段策略装配安全投影，不承担资格或资料写入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberQueryService {

    private static final ResourceKey PLATFORM_MEMBER_RESOURCE = BuiltinResourceProviders.PLATFORM_MEMBER_RESOURCE;
    private static final FieldAccess HIDDEN_FIELD = new FieldAccess(FieldVisibility.HIDDEN, false);

    private final RoleFieldPermissionService platformFields;

    private final IamAccess access;

    private final ResourceScopeAccessService scopes;

    private final ObjectCapabilities capabilities;

    private final FieldAccessEvaluator fields;

    private final MemberRepository members;

    private final GroupRepository groups;

    private final PlatformAssignmentFacade assignments;


    /**
     * 读取整份平台成员读策略的列可见性及新成员创建字段上下文，不依赖分页样本。
     * @return 仅用于展示的上下文；提交与逐行展示仍使用真实目标重新校验
     */
    public PlatformMemberContext platformContext() {
        ActiveIdentity actor = access.requireCurrent();
        if (actor.context().domain() != AuthorizationDomain.PLATFORM)
            throw new BizException(IamReasonCode.ACTION_DENIED);
        var view = capabilities.snapshot(actor.context()).view();
        if (!view.actionCodes().contains(IamAction.PLATFORM_MEMBER_READ.getCode())
                && !view.actionCodes().contains(IamAction.PLATFORM_MEMBER_CREATE.getCode()))
            throw new BizException(IamReasonCode.ACTION_DENIED);
        var policies = platformFields.previewAll(PLATFORM_MEMBER_RESOURCE, actor.context(),
                List.of(IamAction.PLATFORM_MEMBER_READ, IamAction.PLATFORM_MEMBER_CREATE, IamAction.PLATFORM_MEMBER_UPDATE));
        var read = policies.get(IamAction.PLATFORM_MEMBER_READ);
        Map<String, FieldVisibility> columns = new LinkedHashMap<>();
        read.defaults().forEach((key, ignored) -> {
            FieldAccess visible = new FieldAccess(FieldVisibility.HIDDEN, false);
            for (var rule : read.rules())
                if (key.equals(rule.fieldKey()) && !rule.scope().isEmpty())
                    visible = FieldPolicyProcessor.broader(visible, rule.access());
            columns.put(key, read.ceilings().containsKey(key)
                    ? FieldPolicyProcessor.stricter(visible, read.ceilings().get(key)).visibility()
                    : FieldVisibility.HIDDEN);
        });
        boolean searchable = false;
        try {
            FieldPolicyProcessor.requireOriginalLookup(read, MemberFieldKey.VALUE_DISPLAY_NAME);
            searchable = true;
        }
        catch (SdkAuthorizationException exception) {
            if (!IamReasonCode.ACTION_DENIED.getCode().equals(exception.getCode()))
                throw exception;
        }
        return new PlatformMemberContext(columns,
                FieldPolicyProcessor.access(policies.get(IamAction.PLATFORM_MEMBER_CREATE),
                        new ScopeTarget(null, null, null, List.of())), searchable,
                policies.entrySet().stream().collect(java.util.stream.Collectors.toMap(entry -> entry.getKey().getCode(),
                        entry -> FieldPolicyProcessor.operations(entry.getValue()))), read.masks());
    }

    /** 返回成员资源的全局操作能力，列表空页也有相同结果。 */
    public com.ingot.framework.commons.model.iam.ResourceFieldContext tenantContext() {
        var actor = access.requireCurrent();
        if (actor.context().domain() != AuthorizationDomain.TENANT) throw new BizException(IamReasonCode.ACTION_DENIED);
        var authorization = capabilities.snapshot(actor.context()).view();
        if (!authorization.actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_READ)
                && !authorization.actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_CREATE))
            throw new BizException(IamReasonCode.ACTION_DENIED);
        long tenantId = IamIds.require(actor.context().tenantId()), viewerId = IamIds.require(actor.context().memberId());
        var snapshot = fields.snapshot(tenantId, com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders.TENANT_MEMBER_RESOURCE);
        var scope = authorization.actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_READ)
                ? scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ) : ObjectScope.none();
        Map<String, Map<String, com.ingot.framework.commons.model.iam.FieldOperations>> operations = new LinkedHashMap<>();
        for (var action : List.of(IamAction.TENANT_MEMBER_READ, IamAction.TENANT_MEMBER_CREATE, IamAction.TENANT_MEMBER_UPDATE))
            operations.put(action.getCode(), authorization.actionCodes().contains(action.getCode())
                    ? fields.operations(snapshot, viewerId, BuiltinResourceProviders.TENANT_MEMBER_RESOURCE, action.getCode(), scope)
                    : snapshot.baseline().keySet().stream().collect(java.util.stream.Collectors.toMap(key -> key, key -> com.ingot.framework.commons.model.iam.FieldOperations.NONE)));
        return new com.ingot.framework.commons.model.iam.ResourceFieldContext(fields.contextVisibility(snapshot, viewerId), operations, snapshot.masks());
    }

    /**
     * 分页列出当前域成员。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        return list(domain, page, pageSize, null, null);
    }

    /**
     * 分页列出平台成员。显示名按包含匹配，资格按稳定字面量精确匹配。
     * @param page 页码
     * @param pageSize 页大小
     * @param name 显示名包含匹配，可空
     * @param status 成员资格，可空；仅接受 ACTIVE、SUSPENDED、REMOVED
     * @param ids 逗号分隔成员 ID，可空；用于已选回显
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listPlatform(int page, int pageSize, String name, String status,
            String ids) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_READ);
        return listProjected(actor, IamAction.PLATFORM_MEMBER_READ, page, pageSize, null, null,
                IamFilters.containsName(name), IamFilters.memberStatusOf(status), IamIds.optionalList(ids));
    }

    /**
     * 分页列出当前域成员；原值筛选仅在字段完整可见时允许。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize,
            String phone, String email) {
        return list(domain, page, pageSize, phone, email, null, null);
    }

    /**
     * 分页列出当前域成员；原值筛选仅在字段完整可见时允许。
     * 租户列表可按直属部门与资格筛选；不传 status 时排除已移出。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param departmentId 直属任职部门，可空
     * @param status 成员资格，可空；仅接受 ACTIVE、SUSPENDED、REMOVED
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize,
            String phone, String email, String departmentId, String status) {
        IamAction action = domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_READ
                : IamAction.TENANT_MEMBER_READ;
        ActiveIdentity actor = access.require(domain, action);
        if (domain == AuthorizationDomain.PLATFORM && (phone != null || email != null || departmentId != null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return listProjected(actor, action, page, pageSize, phone, email, null, IamFilters.memberStatusOf(status),
                List.of(), departmentId);
    }

    /**
     * 已通过 ACTION 校验后按范围与字段策略列出成员，供导出下载重验复用。
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action, int page,
            int pageSize, String phone, String email) {
        return listProjected(actor, action, page, pageSize, phone, email, null, null, List.of());
    }

    /**
     * 已通过 ACTION 校验后按范围、显示名与资格列出成员。
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param name 平台显示名包含匹配，可空；租户列表忽略
     * @param status 成员资格，可空；不传时租户与平台均排除已移出
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action, int page,
            int pageSize, String phone, String email, String name, MemberStatus status) {
        return listProjected(actor, action, page, pageSize, phone, email, name, status, List.of());
    }

    /**
     * 已通过 ACTION 校验后按范围、显示名、资格与成员 ID 列出成员。
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param name 平台显示名包含匹配，可空；租户列表忽略
     * @param status 成员资格，可空；不传时租户与平台均排除已移出
     * @param ids 平台成员 ID，空表示不按 ID 限制
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action, int page,
            int pageSize, String phone, String email, String name, MemberStatus status, List<Long> ids) {
        return listProjected(actor, action, page, pageSize, phone, email, name, status, ids, null);
    }

    /**
     * 已通过 ACTION 校验后按范围、显示名、资格、成员 ID 与直属部门列出成员。
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param name 平台显示名包含匹配，可空；租户列表忽略
     * @param status 成员资格，可空；不传时租户与平台均排除已移出
     * @param ids 平台成员 ID，空表示不按 ID 限制
     * @param departmentId 直属任职部门，可空；平台列表忽略
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action, int page,
            int pageSize, String phone, String email, String name, MemberStatus status, List<Long> ids,
            String departmentId) {
        IamPages.require(page, pageSize);
        AuthorizationDomain domain = actor.context().domain();
        ObjectScope scope = scopes.memberRead(actor.context(), action);
        ObjectCapabilities.Snapshot caps = capabilities.snapshot(actor.context());
        if (domain == AuthorizationDomain.PLATFORM) {
            var fieldPolicies = platformFields.previewAll(PLATFORM_MEMBER_RESOURCE, actor.context(),
                    List.of(action, IamAction.PLATFORM_MEMBER_UPDATE));
            FieldPolicyDecision policy = fieldPolicies.get(action);
            if (name != null && !name.isBlank()) {
                FieldPolicyProcessor.requireOriginalLookup(policy, MemberFieldKey.VALUE_DISPLAY_NAME);
            }
            Page<IamPlatformMemberEntity> result = members.pagePlatform(scope, page, pageSize, name, status, ids);
            Map<BigInteger, IamAccountEntity> contacts = members
                .accountContacts(result.getRecords().stream().map(IamPlatformMemberEntity::getAccountId).toList());
            var rowCapabilities = capabilities.platformMembers(caps,
                    result.getRecords().stream().map(IamPlatformMemberEntity::getId).toList());
            List<ResourceDetail<MemberRecord>> items = result.getRecords()
                .stream()
                .map(row -> platformDetail(policy, fieldPolicies.get(IamAction.PLATFORM_MEMBER_UPDATE),
                        platformMember(row, contacts.get(row.getAccountId())),
                        rowCapabilities.get(row.getId().toString()), version(row.getVersion())))
                .toList();
            return IamPages.details(items, result.getTotal(), page, pageSize);
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        long viewerId = IamIds.require(actor.context().memberId());
        FieldPolicySnapshot snapshot = fields.snapshot(tenantId, com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders.TENANT_MEMBER_RESOURCE);
        Map<String, String> submitted = new LinkedHashMap<>();
        if (phone != null && !phone.isBlank()) submitted.put(MemberFieldKey.VALUE_PHONE, phone);
        if (email != null && !email.isBlank()) submitted.put(MemberFieldKey.VALUE_EMAIL, email);
        fields.requireFilters(snapshot, viewerId, BuiltinResourceProviders.TENANT_MEMBER_RESOURCE, action.getCode(), scope, submitted);
        Page<IamTenantMemberEntity> result = members.pageTenant(tenantId, scope, phone, email,
                IamIds.optional(departmentId), status, page, pageSize);
        Map<BigInteger, IamAccountEntity> accounts = members.accountContacts(result.getRecords().stream()
                .map(IamTenantMemberEntity::getAccountId).filter(java.util.Objects::nonNull).toList());
        Map<BigInteger, List<MemberDepartmentView>> departments = members.departmentViews(tenantId,
                result.getRecords().stream().map(IamTenantMemberEntity::getId).toList());
        var batch = fields.prepare(snapshot, viewerId, result.getRecords().stream().map(row -> row.getId().longValueExact()).toList());
        List<MemberRecord> raws = new ArrayList<>();
        List<String> versions = new ArrayList<>();
        for (IamTenantMemberEntity row : result.getRecords()) {
            raws.add(tenantMember(row, departments.getOrDefault(row.getId(), List.of()),
                    loginPhone(accounts.get(row.getAccountId()))));
            versions.add(version(row.getVersion()));
        }
        return IamPages.details(projectMembers(batch, caps, raws, versions), result.getTotal(), page, pageSize);
    }

    /**
     * 按导出快照 ID 重验范围与字段策略，返回当前仍可见的完整投影。
     * @param actor 当前身份
     * @param action 导出操作
     * @param memberIds 快照成员 ID
     * @return 完整投影结果
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjectedByIds(ActiveIdentity actor, IamAction action,
            List<String> memberIds) {
        if (actor.context().domain() != AuthorizationDomain.TENANT) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        ObjectScope scope = scopes.memberRead(actor.context(), action);
        if (scope.coversNone() || memberIds == null || memberIds.isEmpty()) {
            return IamPages.complete(List.of());
        }
        LinkedHashSet<String> order = new LinkedHashSet<>();
        List<BigInteger> ids = new ArrayList<>();
        for (String memberId : memberIds) {
            if (memberId == null || memberId.isBlank() || !order.add(memberId)) {
                continue;
            }
            try {
                ids.add(BigInteger.valueOf(IamIds.require(memberId)));
            }
            catch (BizException ignored) {
                // 损坏的快照 ID 不能扩大当前可见集合。
            }
        }
        if (ids.isEmpty()) {
            return IamPages.complete(List.of());
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        long viewerId = IamIds.require(actor.context().memberId());
        List<IamTenantMemberEntity> rows = members.listTenantByIds(tenantId, scope, ids);
        Map<String, IamTenantMemberEntity> byId = new LinkedHashMap<>();
        for (IamTenantMemberEntity row : rows) {
            byId.put(row.getId().toString(), row);
        }
        Map<BigInteger, IamAccountEntity> accounts = members.accountContacts(rows.stream()
                .map(IamTenantMemberEntity::getAccountId).filter(java.util.Objects::nonNull).toList());
        Map<BigInteger, List<MemberDepartmentView>> departments = members.departmentViews(tenantId,
                rows.stream().map(IamTenantMemberEntity::getId).toList());
        FieldPolicySnapshot snapshot = fields.snapshot(tenantId, com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders.TENANT_MEMBER_RESOURCE);
        ObjectCapabilities.Snapshot caps = capabilities.snapshot(actor.context());
        var batch = fields.prepare(snapshot, viewerId, rows.stream().map(row -> row.getId().longValueExact()).toList());
        List<MemberRecord> raws = new ArrayList<>();
        List<String> versions = new ArrayList<>();
        for (String memberId : order) {
            IamTenantMemberEntity row = byId.get(memberId);
            if (row == null) {
                continue;
            }
            raws.add(tenantMember(row, departments.getOrDefault(row.getId(), List.of()),
                    loginPhone(accounts.get(row.getAccountId()))));
            versions.add(version(row.getVersion()));
        }
        List<ResourceDetail<MemberRecord>> items = projectMembers(batch, caps, raws, versions);
        return IamPages.complete(items);
    }

    /**
     * 读取当前域成员详情。
     * @param domain 接口管理域
     * @param memberId 成员 ID
     * @return 安全投影
     */
    public ResourceDetail<MemberRecord> get(AuthorizationDomain domain, String memberId) {
        ActiveIdentity actor = access.require(domain,
                domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ);
        long id = IamIds.require(memberId);
        scopes.requireVisibleMember(actor.context(),
                domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ,
                id);
        return load(domain, actor, id);
    }

    /**
     * 分页列出平台用户组的直接成员，按组成员关系过滤，不接受调用方传入成员 ID。
     * @param groupId 平台组 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param name 显示名包含匹配，可空
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listPlatformGroupMembers(String groupId, int page, int pageSize,
            String name) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_GROUP_READ);
        IamPages.require(page, pageSize);
        long id = IamIds.require(groupId);
        scopes.requireVisibleObject(actor.context(), IamAction.PLATFORM_GROUP_READ, id);
        if (groups.findPlatform(id) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        ObjectCapabilities.Snapshot caps = capabilities.snapshot(actor.context());
        var fieldPolicies = platformFields.previewAll(PLATFORM_MEMBER_RESOURCE, actor.context(),
                List.of(IamAction.PLATFORM_MEMBER_READ, IamAction.PLATFORM_MEMBER_UPDATE));
        FieldPolicyDecision policy = fieldPolicies.get(IamAction.PLATFORM_MEMBER_READ);
        if (name != null && !name.isBlank()) {
            FieldPolicyProcessor.requireOriginalLookup(policy, MemberFieldKey.VALUE_DISPLAY_NAME);
        }
        var scope = scopes.memberRead(actor.context(), IamAction.PLATFORM_MEMBER_READ);
        Page<IamPlatformMemberEntity> result = members.pagePlatformByGroup(scope, id, page, pageSize,
                IamFilters.containsName(name));
        Map<BigInteger, IamAccountEntity> contacts = members
            .accountContacts(result.getRecords().stream().map(IamPlatformMemberEntity::getAccountId).toList());
        List<ResourceDetail<MemberRecord>> items = result.getRecords()
            .stream()
            .map(row -> platformDetail(policy, fieldPolicies.get(IamAction.PLATFORM_MEMBER_UPDATE),
                    platformMember(row, contacts.get(row.getAccountId())),
                    capabilities.platformMember(caps, row.getId().toString()), version(row.getVersion())))
            .toList();
        return IamPages.details(items, result.getTotal(), page, pageSize);
    }

    /**
     * 分页列出平台成员所在的静态用户组，只返回组名，不展开成员选择。
     * @param memberId 平台成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 组页
     */
    public PageResponse<ResourceDetail<GroupRecord>> listPlatformGroups(String memberId, int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_READ);
        long id = IamIds.require(memberId);
        IamPages.require(page, pageSize);
        scopes.requireVisibleMember(actor.context(), IamAction.PLATFORM_MEMBER_READ, id);
        if (members.findPlatform(id) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        var scope = scopes.objects(actor.context(), IamAction.PLATFORM_GROUP_READ);
        Page<IamPlatformGroupEntity> rows = groups.pagePlatformByMember(id, page, pageSize, scope);
        List<ResourceDetail<GroupRecord>> items = rows.getRecords()
            .stream()
            .map(row -> ResourceDetailFactory.of(new GroupRecord(row.getId().toString(), row.getName(), row.getDescription(),
                    new Selection(List.of(), List.of()), null, null), version(row.getVersion())))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 分页列出租户成员所在用户组，含直接加入和部门来源，只返回组名。
     *
     * @param memberId 租户成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 组页
     */
    public PageResponse<ResourceDetail<GroupRecord>> listTenantGroups(String memberId, int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ);
        long id = IamIds.require(memberId);
        IamPages.require(page, pageSize);
        scopes.requireVisibleMember(actor.context(), IamAction.TENANT_MEMBER_READ, id);
        long tenantId = IamIds.require(actor.context().tenantId());
        if (members.findTenant(tenantId, id) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        var scope = scopes.objects(actor.context(), IamAction.TENANT_GROUP_READ);
        Page<IamTenantGroupEntity> rows = groups.pageTenantByMember(tenantId, id, page, pageSize, scope);
        List<ResourceDetail<GroupRecord>> items = rows.getRecords()
            .stream()
            .map(row -> ResourceDetailFactory.of(new GroupRecord(row.getId().toString(), row.getName(), row.getDescription(),
                    new Selection(List.of(), List.of()), null, null), version(row.getVersion())))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 列出平台成员的简单直接角色。
     * @param memberId 平台成员 ID
     * @return 按名称排序的直接角色
     */
    public List<MemberRoleView> listDirectRoles(String memberId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_READ);
        long id = IamIds.require(memberId);
        scopes.requireVisibleMember(actor.context(), IamAction.PLATFORM_MEMBER_READ, id);
        return assignments.listDirectRoles(AuthorizationDomain.PLATFORM, memberId);
    }

    ResourceDetail<MemberRecord> load(AuthorizationDomain domain, ActiveIdentity actor, long memberId) {
        if (domain == AuthorizationDomain.PLATFORM) {
            IamPlatformMemberEntity row = members.findPlatform(memberId);
            if (row == null) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            var fieldPolicies = platformFields.previewAll(PLATFORM_MEMBER_RESOURCE, actor.context(),
                    List.of(IamAction.PLATFORM_MEMBER_READ, IamAction.PLATFORM_MEMBER_UPDATE));
            return platformDetail(fieldPolicies.get(IamAction.PLATFORM_MEMBER_READ),
                    fieldPolicies.get(IamAction.PLATFORM_MEMBER_UPDATE),
                    platformMemberDetail(row, row.getAccountId() == null ? null
                            : members.accountLoginInfo(row.getAccountId())),
                    capabilities.platformMember(capabilities.snapshot(actor.context()), row.getId().toString()),
                    version(row.getVersion()));
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        long viewerId = IamIds.require(actor.context().memberId());
        IamTenantMemberEntity row = members.findTenant(tenantId, memberId);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        List<MemberDepartmentView> departments = members.departmentViews(tenantId, List.of(row.getId()))
            .getOrDefault(row.getId(), List.of());
        MemberRecord raw = tenantMemberDetail(row, departments);
        var snapshot = fields.snapshot(tenantId, com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders.TENANT_MEMBER_RESOURCE);
        var caps = capabilities.snapshot(actor.context());
        var batch = fields.prepare(snapshot, viewerId, List.of(row.getId().longValueExact()));
        var rowCapabilities = capabilities.tenantMembers(caps, List.of(raw)).get(raw.id());
        return detail(batch, rowCapabilities == null ? Map.of() : rowCapabilities, raw,
                version(row.getVersion()), true);
    }

    private ResourceDetail<MemberRecord> detail(FieldPolicySnapshot snapshot, ObjectCapabilities.Snapshot caps,
            long viewerId, MemberRecord raw, String version) {
        return detail(fields.prepare(snapshot, viewerId, List.of(IamIds.require(raw.id()))), caps, raw, version);
    }

    private ResourceDetail<MemberRecord> detail(FieldAccessEvaluator.FieldBatch batch, ObjectCapabilities.Snapshot caps,
            MemberRecord raw, String version) {
        return projectMembers(batch, caps, List.of(raw), List.of(version)).getFirst();
    }

    private List<ResourceDetail<MemberRecord>> projectMembers(FieldAccessEvaluator.FieldBatch batch,
            ObjectCapabilities.Snapshot caps, List<MemberRecord> raws, List<String> versions) {
        var rowCapabilities = capabilities.tenantMembers(caps, raws);
        List<ResourceDetail<MemberRecord>> items = new ArrayList<>();
        for (int index = 0; index < raws.size(); index++) {
            items.add(detail(batch, rowCapabilities.get(raws.get(index).id()), raws.get(index), versions.get(index),
                    false));
        }
        return items;
    }

    private ResourceDetail<MemberRecord> detail(FieldAccessEvaluator.FieldBatch batch,
            Map<String, com.ingot.cloud.iam.application.shared.model.ObjectCapability> targetCapabilities, MemberRecord raw,
            String version, boolean retainTimes) {
        var update = targetCapabilities.get(IamAction.VALUE_TENANT_MEMBER_UPDATE);
        var access = fields.memberAccess(batch, IamIds.require(raw.id())).entrySet().stream().collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey, entry -> new com.ingot.framework.commons.model.iam.FieldAccess(entry.getValue().visibility(),
                        entry.getValue().editable() && update != null && update.allowed())));
        if (retainTimes) {
            var readonly = new FieldAccess(FieldVisibility.FULL, false);
            access.put(MemberTimeFields.JOINED_AT, readonly);
            access.put(MemberTimeFields.LAST_LOGIN_AT, readonly);
            access.put(MemberTimeFields.UPDATED_AT, readonly);
        }
        return ResourceDetailFactory.of(fields.project(raw, access, BuiltinResourceProviders.TENANT_MEMBER_RESOURCE, batch.snapshot().masks()),
                access, targetCapabilities, version);
    }

    private ResourceDetail<MemberRecord> platformDetail(FieldPolicyDecision policy, FieldPolicyDecision writePolicy,
            MemberRecord raw, Map<String, com.ingot.cloud.iam.application.shared.model.ObjectCapability> capabilities,
            String version) {
        var target = new ScopeTarget(raw.id(), raw.id(), null, List.of());
        var readable = FieldPolicyProcessor.access(policy, target);
        var writable = FieldPolicyProcessor.access(writePolicy, target);
        Map<String, com.ingot.framework.commons.model.iam.FieldAccess> fieldAccess = new LinkedHashMap<>();
        readable.forEach((key, value) -> {
            var write = writable.get(key);
            fieldAccess.put(key, new com.ingot.framework.commons.model.iam.FieldAccess(value.visibility(),
                    value.visibility() != com.ingot.framework.commons.model.iam.FieldVisibility.HIDDEN && write != null
                            && write.visibility() != com.ingot.framework.commons.model.iam.FieldVisibility.HIDDEN
                            && write.editable()));
        });
        for (var key : MemberFieldKey.values())
            fieldAccess.putIfAbsent(key.getValue(), HIDDEN_FIELD);
        return ResourceDetailFactory.of(fields.project(raw, fieldAccess, PLATFORM_MEMBER_RESOURCE, policy.masks()), fieldAccess, capabilities, version);
    }

    private static MemberRecord platformMember(IamPlatformMemberEntity row, IamAccountEntity account) {
        return new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(),
                row.getPhone(), row.getEmail(),
                account == null ? null : account.getUsername(), loginPhone(account), row.getStatus(), List.of(),
                null, null, null);
    }

    private static MemberRecord platformMemberDetail(IamPlatformMemberEntity row, IamAccountEntity account) {
        return new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(),
                row.getPhone(), row.getEmail(), account == null ? null : account.getUsername(), loginPhone(account),
                row.getStatus(), List.of(), utcInstant(row.getCreatedAt()),
                account == null ? null : utcInstant(account.getLastLoginAt()), utcInstant(row.getUpdatedAt()));
    }

    private static Instant utcInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static MemberRecord tenantMember(IamTenantMemberEntity row, List<MemberDepartmentView> departments,
                                             String loginPhone) {
        return new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(), row.getPhone(),
                row.getEmail(), null, loginPhone, row.getStatus(), departments, null, null, null);
    }

    private MemberRecord tenantMemberDetail(IamTenantMemberEntity row, List<MemberDepartmentView> departments) {
        IamAccountEntity account = row.getAccountId() == null ? null : members.accountLoginInfo(row.getAccountId());
        return new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(), row.getPhone(),
                row.getEmail(), null, loginPhone(account), row.getStatus(), departments, utcInstant(row.getCreatedAt()),
                account == null ? null : utcInstant(account.getLastLoginAt()), utcInstant(row.getUpdatedAt()));
    }

    private static String loginPhone(IamAccountEntity account) {
        return account == null || account.getPhone() == null || account.getPhone().isBlank() ? null : account.getPhone();
    }

    private static String version(BigInteger version) {
        return version == null ? "0" : version.toString();
    }
}
