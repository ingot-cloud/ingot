package com.ingot.cloud.iam.organization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.evaluation.ResourceScopeFilter;
import com.ingot.cloud.iam.identity.ActiveIdentity;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
    private final IamAuditWriter audits;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计与部门表。
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param audits 同事务审计
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public DepartmentService(IamAccess access, ResourceAccess scopes, IamAuditWriter audits, DataSource dataSource,
                             PlatformTransactionManager transactionManager) {
        this.access = access;
        this.scopes = scopes;
        this.audits = audits;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
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
        ResourceScopeFilter.Predicate scope = scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ,
                "id");
        Map<String, Object> countParameters = new HashMap<>(scope.parameters());
        countParameters.put("tenantId", tenantId);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_department WHERE tenant_id=:tenantId AND " + scope.sql(),
                countParameters, Long.class);
        Map<String, Object> parameters = new HashMap<>(countParameters);
        parameters.put("limit", pageSize);
        parameters.put("offset", IamPages.offset(page, pageSize));
        List<ResourceDetail<DepartmentRecord>> items = jdbc.query("""
                SELECT id,parent_id,name,sort_order,version FROM iam_department
                 WHERE tenant_id=:tenantId AND %s ORDER BY sort_order,id LIMIT :limit OFFSET :offset
                """.formatted(scope.sql()), parameters,
                (row, index) -> IamDetails.of(department(row), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
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
        return load(IamIds.require(actor.context().tenantId()), departmentId);
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
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", id);
            parameters.put("tenantId", tenantId);
            parameters.put("parentId", parentId);
            parameters.put("name", input.name());
            parameters.put("sortOrder", input.sortOrder());
            jdbc.update("""
                    INSERT INTO iam_department(id,tenant_id,parent_id,name,sort_order)
                    VALUES (:id,:tenantId,:parentId,:name,:sortOrder)
                    """, parameters);
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
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", departmentId);
            parameters.put("tenantId", tenantId);
            parameters.put("parentId", parentId);
            parameters.put("name", input.department().name());
            parameters.put("sortOrder", input.department().sortOrder());
            jdbc.update("""
                    UPDATE iam_department SET parent_id=:parentId,name=:name,sort_order=:sortOrder,version=version+1
                     WHERE tenant_id=:tenantId AND id=:id
                    """, parameters);
            audits.write(actor.context(), access.nextId(), "department", id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.record().name()),
                    Map.of(AuditField.NAME, input.department().name()),
                    Map.of("department", Long.toString(Long.parseLong(current.version()) + 1)));
            return load(tenantId, departmentId);
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
            Long children = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_department WHERE tenant_id=:tenantId AND parent_id=:id",
                    Map.of("tenantId", tenantId, "id", departmentId), Long.class);
            Long members = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_member_department WHERE tenant_id=:tenantId AND department_id=:id",
                    Map.of("tenantId", tenantId, "id", departmentId), Long.class);
            if ((children != null && children > 0) || (members != null && members > 0)) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            jdbc.update("DELETE FROM iam_department WHERE tenant_id=:tenantId AND id=:id",
                    Map.of("tenantId", tenantId, "id", departmentId));
            audits.write(actor.context(), access.nextId(), "department", id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.record().name()), Map.of(),
                    Map.of("department", current.version()));
            return new CreatedResource(id, current.version());
        });
    }

    private ResourceDetail<DepartmentRecord> load(long tenantId, long id) {
        List<ResourceDetail<DepartmentRecord>> rows = jdbc.query("""
                SELECT id,parent_id,name,sort_order,version FROM iam_department
                 WHERE tenant_id=:tenantId AND id=:id
                """, Map.of("tenantId", tenantId, "id", id),
                (row, index) -> IamDetails.of(department(row), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceDetail<DepartmentRecord> lock(long tenantId, long id) {
        List<ResourceDetail<DepartmentRecord>> rows = jdbc.query("""
                SELECT id,parent_id,name,sort_order,version FROM iam_department
                 WHERE tenant_id=:tenantId AND id=:id FOR UPDATE
                """, Map.of("tenantId", tenantId, "id", id),
                (row, index) -> IamDetails.of(department(row), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private Long requireParent(long tenantId, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        long id = IamIds.require(parentId);
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_department WHERE tenant_id=:tenantId AND id=:id",
                Map.of("tenantId", tenantId, "id", id), Long.class);
        if (count == null || count != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return id;
    }

    private boolean isAncestor(long tenantId, long candidate, long nodeId) {
        Long current = candidate;
        while (current != null) {
            if (current == nodeId) {
                return true;
            }
            List<Long> parents = jdbc.query(
                    "SELECT parent_id FROM iam_department WHERE tenant_id=:tenantId AND id=:id",
                    Map.of("tenantId", tenantId, "id", current), (row, index) -> {
                        long parent = row.getLong("parent_id");
                        return row.wasNull() ? null : parent;
                    });
            current = parents.isEmpty() ? null : parents.getFirst();
        }
        return false;
    }

    private static DepartmentRecord department(java.sql.ResultSet row) throws java.sql.SQLException {
        String parent = row.getString("parent_id");
        return new DepartmentRecord(row.getString("id"), parent == null || parent.isBlank() ? null : parent,
                row.getString("name"), row.getInt("sort_order"), false);
    }
}
