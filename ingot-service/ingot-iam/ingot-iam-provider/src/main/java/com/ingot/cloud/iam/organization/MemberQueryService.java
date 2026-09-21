package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.policy.FieldPolicySnapshot;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberCreateInput;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberProfileInput;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>查询并维护当前域成员资格，按字段策略投影资料并拒绝不可编辑字段写入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class MemberQueryService {
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final ObjectCapabilities capabilities;
    private final FieldAccessEvaluator fields;
    private final IamAuditWriter audits;
    private final MemberQueryRepository members;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、范围、字段策略与成员表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param capabilities 对象展示能力
     * @param fields 字段访问
     * @param audits 同事务审计
     * @param members 成员持久化
     * @param transactionManager 同一数据源事务
     */
    public MemberQueryService(IamAccess access, ResourceAccess scopes, ObjectCapabilities capabilities,
                              FieldAccessEvaluator fields, IamAuditWriter audits, MemberQueryRepository members,
                              PlatformTransactionManager transactionManager) {
        this.access = access;
        this.scopes = scopes;
        this.capabilities = capabilities;
        this.fields = fields;
        this.audits = audits;
        this.members = members;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出当前域成员。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        return list(domain, page, pageSize, null, null);
    }

    /**
     * 分页列出当前域成员；原值筛选仅在字段完整可见时允许。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize,
                                                           String phone, String email) {
        IamAction action = domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ;
        ActiveIdentity actor = access.require(domain, action);
        if (domain == AuthorizationDomain.PLATFORM && (phone != null || email != null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return listProjected(actor, action, page, pageSize, phone, email);
    }

    /**
     * 已通过 ACTION 校验后按范围与字段策略列出成员，供导出下载重验复用。
     *
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action,
                                                                    int page, int pageSize, String phone,
                                                                    String email) {
        IamPages.require(page, pageSize);
        AuthorizationDomain domain = actor.context().domain();
        ObjectScope scope = scopes.memberRead(actor.context(), action);
        ObjectCapabilities.Snapshot caps = capabilities.snapshot(actor.context());
        if (domain == AuthorizationDomain.PLATFORM) {
            Page<IamPlatformMemberEntity> result = members.pagePlatform(scope, page, pageSize);
            List<ResourceDetail<MemberRecord>> items = result.getRecords().stream()
                    .map(row -> IamDetails.of(platformMember(row),
                            capabilities.platformMember(caps, row.getId().toString()), version(row.getVersion())))
                    .toList();
            return IamPages.details(items, result.getTotal(), page, pageSize);
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        long viewerId = IamIds.require(actor.context().memberId());
        FieldPolicySnapshot snapshot = fields.snapshot(tenantId, PolicyScenario.MANAGEMENT);
        fields.requireOriginalLookup(snapshot, viewerId, MemberFieldKey.VALUE_PHONE, phone, scope);
        fields.requireOriginalLookup(snapshot, viewerId, MemberFieldKey.VALUE_EMAIL, email, scope);
        Page<IamTenantMemberEntity> result = members.pageTenant(tenantId, scope, phone, email, page, pageSize);
        Map<BigInteger, List<MemberDepartmentView>> departments = members.departmentViews(tenantId,
                result.getRecords().stream().map(IamTenantMemberEntity::getId).toList());
        List<ResourceDetail<MemberRecord>> items = result.getRecords().stream()
                .map(row -> detail(snapshot, caps, viewerId,
                        tenantMember(row, departments.getOrDefault(row.getId(), List.of())),
                        version(row.getVersion())))
                .toList();
        return IamPages.details(items, result.getTotal(), page, pageSize);
    }

    /**
     * 按导出快照 ID 重验范围与字段策略，返回当前仍可见的完整投影。
     *
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
            } catch (BizException ignored) {
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
        Map<BigInteger, List<MemberDepartmentView>> departments = members.departmentViews(tenantId,
                rows.stream().map(IamTenantMemberEntity::getId).toList());
        FieldPolicySnapshot snapshot = fields.snapshot(tenantId, PolicyScenario.MANAGEMENT);
        ObjectCapabilities.Snapshot caps = capabilities.snapshot(actor.context());
        List<ResourceDetail<MemberRecord>> items = new ArrayList<>();
        for (String memberId : order) {
            IamTenantMemberEntity row = byId.get(memberId);
            if (row == null) {
                continue;
            }
            items.add(detail(snapshot, caps, viewerId,
                    tenantMember(row, departments.getOrDefault(row.getId(), List.of())),
                    version(row.getVersion())));
        }
        return IamPages.complete(items);
    }

    /**
     * 读取当前域成员详情。
     *
     * @param domain 接口管理域
     * @param memberId 成员 ID
     * @return 安全投影
     */
    public ResourceDetail<MemberRecord> get(AuthorizationDomain domain, String memberId) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ);
        long id = IamIds.require(memberId);
        scopes.requireVisibleMember(actor.context(),
                domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ,
                id);
        return load(domain, actor, id);
    }

    /**
     * 把已有账号关联为当前域成员。
     *
     * @param domain 接口管理域
     * @param input 账号与任职
     * @return 新成员 ID
     */
    public CreatedResource create(AuthorizationDomain domain, MemberCreateInput input) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_CREATE : IamAction.TENANT_MEMBER_CREATE);
        if (domain == AuthorizationDomain.PLATFORM && !input.departments().isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
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
            scopes.requireCreate(actor.context(),
                    domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_CREATE
                            : IamAction.TENANT_MEMBER_CREATE, departmentIds);
            if (!members.activeAccount(accountId)) {
                throw new BizException(IamReasonCode.IDENTITY_INVALID);
            }
            long id = access.nextId();
            String displayName = input.displayName() == null || input.displayName().isBlank()
                    ? "成员" : input.displayName().trim();
            try {
                if (domain == AuthorizationDomain.PLATFORM) {
                    members.insertPlatform(id, accountId, displayName);
                } else {
                    long tenantId = IamIds.require(actor.context().tenantId());
                    members.insertTenant(id, tenantId, accountId, displayName);
                    replaceDepartments(tenantId, id, departmentBindings);
                }
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "成员资格已存在");
            }
            audits.write(actor.context(), access.nextId(), "member", IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, displayName), Map.of("member", "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新当前域显示资料，不修改凭证或状态；不可编辑字段拒绝写入。
     *
     * @param domain 接口管理域
     * @param memberId 成员 ID
     * @param input 资料
     * @return 更新后投影
     */
    public ResourceDetail<MemberRecord> patch(AuthorizationDomain domain, String memberId, MemberProfileInput input) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_UPDATE : IamAction.TENANT_MEMBER_UPDATE);
        long id = IamIds.require(memberId);
        if (domain == AuthorizationDomain.PLATFORM && (input.phone() != null || input.email() != null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return transaction.execute(status -> {
            BigInteger version = lock(domain, actor, id);
            scopes.requireVisibleMember(actor.context(),
                    domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_UPDATE
                            : IamAction.TENANT_MEMBER_UPDATE, id);
            ResourceDetail<MemberRecord> current = load(domain, actor, id);
            IamIds.requireVersion(input.expectedVersion(), version.toString());
            if (domain == AuthorizationDomain.PLATFORM) {
                requireApplied(members.updatePlatform(id, input.displayName(), input.avatar(), version));
            } else {
                long tenantId = IamIds.require(actor.context().tenantId());
                long viewerId = IamIds.require(actor.context().memberId());
                fields.requireWritable(tenantId, viewerId, id, MemberFieldKey.VALUE_DISPLAY_NAME, input.displayName());
                fields.requireWritable(tenantId, viewerId, id, MemberFieldKey.VALUE_AVATAR, input.avatar());
                fields.requireWritable(tenantId, viewerId, id, MemberFieldKey.VALUE_PHONE, input.phone());
                fields.requireWritable(tenantId, viewerId, id, MemberFieldKey.VALUE_EMAIL, input.email());
                requireApplied(members.updateTenant(tenantId, id, input.displayName(), input.avatar(), input.phone(),
                        input.email(), version));
            }
            ResourceDetail<MemberRecord> next = load(domain, actor, id);
            audits.write(actor.context(), access.nextId(), "member", memberId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, String.valueOf(current.record().displayName())),
                    Map.of(AuditField.NAME, String.valueOf(next.record().displayName())),
                    Map.of("member", next.version()));
            return next;
        });
    }

    /**
     * 在事务内锁定成员行，后续可见性、字段策略与条件更新都基于这一份状态。
     */
    private BigInteger lock(AuthorizationDomain domain, ActiveIdentity actor, long memberId) {
        BigInteger version = domain == AuthorizationDomain.PLATFORM
                ? version(members.lockPlatform(memberId))
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

    private ResourceDetail<MemberRecord> load(AuthorizationDomain domain, ActiveIdentity actor, long memberId) {
        if (domain == AuthorizationDomain.PLATFORM) {
            IamPlatformMemberEntity row = members.findPlatform(memberId);
            if (row == null) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return IamDetails.of(platformMember(row),
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
        return detail(fields.snapshot(tenantId, PolicyScenario.MANAGEMENT), capabilities.snapshot(actor.context()),
                viewerId, tenantMember(row, departments), version(row.getVersion()));
    }

    private ResourceDetail<MemberRecord> detail(FieldPolicySnapshot snapshot, ObjectCapabilities.Snapshot caps,
                                                long viewerId, MemberRecord raw, String version) {
        Map<String, FieldAccess> access = fields.memberAccess(snapshot, viewerId, IamIds.require(raw.id()));
        return IamDetails.of(fields.project(raw, access), access, capabilities.tenantMember(caps, raw), version);
    }

    private static MemberRecord platformMember(IamPlatformMemberEntity row) {
        return new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(), null, null,
                row.getStatus(), List.of());
    }

    private static MemberRecord tenantMember(IamTenantMemberEntity row, List<MemberDepartmentView> departments) {
        return new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(), row.getPhone(),
                row.getEmail(), row.getStatus(), departments);
    }

    private void replaceDepartments(long tenantId, long memberId, Map<String, Boolean> departments) {
        for (String departmentId : departments.keySet()) {
            if (!members.lockDepartment(tenantId, IamIds.require(departmentId))) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
        members.replaceDepartments(tenantId, memberId, departments);
    }

    private static String version(BigInteger version) {
        return version == null ? "0" : version.toString();
    }
}
