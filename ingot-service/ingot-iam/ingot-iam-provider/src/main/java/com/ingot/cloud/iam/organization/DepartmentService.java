package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
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
import com.ingot.framework.commons.model.iam.DepartmentDraft;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.DepartmentUpdateInput;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护当前租户部门树，拒绝跨租户引用、循环移动和非空删除。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class DepartmentService {
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final ObjectCapabilities capabilities;
    private final IamAuditWriter audits;
    private final DepartmentQueryRepository departments;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计与部门表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param capabilities 对象展示能力
     * @param audits 同事务审计
     * @param departments 部门持久化
     * @param transactionManager 同一数据源事务
     */
    public DepartmentService(IamAccess access, ResourceAccess scopes, ObjectCapabilities capabilities,
                             IamAuditWriter audits, DepartmentQueryRepository departments,
                             PlatformTransactionManager transactionManager) {
        this.access = access;
        this.scopes = scopes;
        this.capabilities = capabilities;
        this.audits = audits;
        this.departments = departments;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 列出当前租户部门。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 部门页
     */
    public PageResponse<ResourceDetail<DepartmentRecord>> list(int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ);
        long tenantId = IamIds.require(actor.context().tenantId());
        IamPages.require(page, pageSize);
        ObjectScope scope = scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ);
        ObjectCapabilities.Snapshot caps = capabilities.snapshot(actor.context());
        Page<IamDepartmentEntity> result = departments.page(tenantId, scope, page, pageSize);
        List<ResourceDetail<DepartmentRecord>> items = result.getRecords().stream()
                .map(row -> IamDetails.of(department(row), capabilities.department(caps, row.getId().toString()),
                        version(row.getVersion())))
                .toList();
        return IamPages.details(items, result.getTotal(), page, pageSize);
    }

    /**
     * 读取部门详情，不携带成员计数。
     *
     * @param id 部门 ID
     * @return 部门详情
     */
    public ResourceDetail<DepartmentRecord> get(String id) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ);
        long departmentId = IamIds.require(id);
        scopes.requireVisibleDepartment(actor.context(), IamAction.TENANT_DEPARTMENT_READ, departmentId);
        return load(actor, IamIds.require(actor.context().tenantId()), departmentId);
    }

    /**
     * 创建部门并校验父节点归属。
     *
     * @param input 部门草稿
     * @return 新部门 ID
     */
    public CreatedResource create(DepartmentDraft input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_CREATE);
        long tenantId = IamIds.require(actor.context().tenantId());
        return transaction.execute(status -> {
            Long parentId = requireParent(tenantId, input.parentId());
            if (parentId == null) {
                scopes.requireCreate(actor.context(), IamAction.TENANT_DEPARTMENT_CREATE, Set.of());
            } else {
                scopes.requireDepartmentWrite(actor.context(), IamAction.TENANT_DEPARTMENT_CREATE, parentId);
            }
            long id = access.nextId();
            IamDepartmentEntity row = new IamDepartmentEntity();
            row.setId(BigInteger.valueOf(id));
            row.setTenantId(BigInteger.valueOf(tenantId));
            row.setParentId(parentId == null ? null : BigInteger.valueOf(parentId));
            row.setName(input.name());
            row.setSortOrder(input.sortOrder());
            departments.insert(row);
            audits.write(actor.context(), access.nextId(), "department", IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of("department", "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新部门并拒绝把节点移动到自己的子树。
     *
     * @param id 部门 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<DepartmentRecord> update(String id, DepartmentUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_UPDATE);
        long tenantId = IamIds.require(actor.context().tenantId());
        long departmentId = IamIds.require(id);
        scopes.requireDepartmentWrite(actor.context(), IamAction.TENANT_DEPARTMENT_UPDATE, departmentId);
        return transaction.execute(status -> {
            ResourceDetail<DepartmentRecord> current = lock(tenantId, departmentId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            Long parentId = requireParent(tenantId, input.department().parentId());
            if (parentId != null) {
                scopes.requireDepartmentWrite(actor.context(), IamAction.TENANT_DEPARTMENT_UPDATE, parentId);
            }
            if (parentId != null && (parentId == departmentId || isAncestor(tenantId, parentId, departmentId))) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            departments.update(tenantId, departmentId, parentId, input.department().name(),
                    input.department().sortOrder(), new BigInteger(current.version()));
            audits.write(actor.context(), access.nextId(), "department", id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.record().name()),
                    Map.of(AuditField.NAME, input.department().name()),
                    Map.of("department", Long.toString(Long.parseLong(current.version()) + 1)));
            return load(actor, tenantId, departmentId);
        });
    }

    /**
     * 删除没有子部门且没有任职的部门。
     *
     * @param id 部门 ID
     * @return 删除前版本
     */
    public CreatedResource delete(String id) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_DELETE);
        long tenantId = IamIds.require(actor.context().tenantId());
        long departmentId = IamIds.require(id);
        scopes.requireDepartmentWrite(actor.context(), IamAction.TENANT_DEPARTMENT_DELETE, departmentId);
        return transaction.execute(status -> {
            ResourceDetail<DepartmentRecord> current = lock(tenantId, departmentId);
            if (departments.childCount(tenantId, departmentId) > 0
                    || departments.memberCount(tenantId, departmentId) > 0) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            departments.delete(tenantId, departmentId);
            audits.write(actor.context(), access.nextId(), "department", id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.record().name()), Map.of(),
                    Map.of("department", current.version()));
            return new CreatedResource(id, current.version());
        });
    }

    private ResourceDetail<DepartmentRecord> load(ActiveIdentity actor, long tenantId, long id) {
        IamDepartmentEntity row = departments.find(tenantId, id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(department(row),
                capabilities.department(capabilities.snapshot(actor.context()), row.getId().toString()),
                version(row.getVersion()));
    }

    private ResourceDetail<DepartmentRecord> lock(long tenantId, long id) {
        IamDepartmentEntity row = departments.lock(tenantId, id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(department(row), version(row.getVersion()));
    }

    private Long requireParent(long tenantId, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        long id = IamIds.require(parentId);
        if (departments.find(tenantId, id) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return id;
    }

    private boolean isAncestor(long tenantId, long candidate, long nodeId) {
        BigInteger current = BigInteger.valueOf(candidate);
        BigInteger target = BigInteger.valueOf(nodeId);
        while (current != null) {
            if (current.equals(target)) {
                return true;
            }
            current = departments.parentId(tenantId, current.longValueExact());
        }
        return false;
    }

    private static DepartmentRecord department(IamDepartmentEntity row) {
        String parent = row.getParentId() == null ? null : row.getParentId().toString();
        return new DepartmentRecord(row.getId().toString(), parent, row.getName(),
                row.getSortOrder() == null ? 0 : row.getSortOrder(), false);
    }

    private static String version(BigInteger version) {
        return version == null ? "0" : version.toString();
    }
}
