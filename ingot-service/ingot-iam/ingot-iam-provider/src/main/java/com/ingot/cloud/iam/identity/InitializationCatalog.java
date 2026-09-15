package com.ingot.cloud.iam.identity;

import java.util.List;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ApplicationSummary;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.TenantCreateInput;
import com.ingot.framework.commons.model.iam.TenantPreviewResult;
import com.ingot.cloud.iam.persistence.InitializationCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>从服务器基础目录解析组织初始化引用，不把客户端传入的版本或应用清单当作权威来源。</p>
 *
 * <p>调用方仍须先校验创建组织 ACTION。本服务只读取目录并分配新 ID，不写入组织行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class InitializationCatalog {
    private static final String DEFAULT_OWNER_NAME = "组织所有者";
    private final InitializationCatalogRepository catalog;

    /**
     * 预览将创建的最小实体和开通应用，不分配 ID、不写入。
     *
     * @param input 组织资料与所有者账号
     * @return 服务器解析后的初始化结果
     * @throws BizException 目录不完整、套餐无效或所有者账号不可用
     */
    public TenantPreviewResult preview(TenantCreateInput input) {
        CatalogSelection selection = resolve(input);
        return new TenantPreviewResult(selection.name(), input.ownerAccountId(), selection.ownerDisplayName(),
                selection.rootDepartmentName(), selection.applications(), input.planId());
    }

    /**
     * 生成内部初始化计划；治理版本、默认策略和基础应用均来自目录。
     *
     * @param input 组织资料与所有者账号
     * @param ids 服务器发号器
     * @return 可供 {@link TenantInitializer} 提交的计划
     * @throws BizException 目录不完整、套餐无效或所有者账号不可用
     */
    public TenantInitializationPlan plan(TenantCreateInput input, InitializationIdAllocator ids) {
        CatalogSelection selection = resolve(input);
        List<TenantInitializationPlan.Application> applications = selection.applicationIds().stream()
                .map(applicationId -> new TenantInitializationPlan.Application(applicationId, ids.nextId()))
                .toList();
        return new TenantInitializationPlan(ids.nextId(), selection.name(), selection.ownerAccountId(), ids.nextId(),
                selection.ownerDisplayName(), ids.nextId(), selection.rootDepartmentName(), ids.nextId(),
                selection.governanceRevisionId(), selection.directoryRevisionId(), selection.fieldRevisionId(),
                applications, ids.nextId());
    }

    private CatalogSelection resolve(TenantCreateInput input) {
        long ownerAccountId = parseId(input.ownerAccountId(), IamReasonCode.IDENTITY_INVALID);
        requireAccount(ownerAccountId);
        long governance = uniqueTenantSystemRevision();
        long directory = latestDefault(DefaultPolicyKind.DIRECTORY);
        long field = latestDefault(DefaultPolicyKind.FIELD);
        List<ApplicationSummary> applications = input.planId() == null || input.planId().isBlank()
                ? baselineApplications() : planApplications(input.planId());
        if (applications.isEmpty()) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        String name = input.name().trim();
        String ownerName = blank(input.ownerDisplayName()) ? DEFAULT_OWNER_NAME : input.ownerDisplayName().trim();
        String rootName = blank(input.rootDepartmentName()) ? name : input.rootDepartmentName().trim();
        return new CatalogSelection(name, ownerAccountId, ownerName, rootName, governance,
                directory, field, applications);
    }

    private void requireAccount(long accountId) {
        if (!catalog.activeAccount(accountId)) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
    }

    private long uniqueTenantSystemRevision() {
        List<Long> roles = catalog.tenantSystemRoles();
        if (roles.size() != 1) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        List<Long> revisions = catalog.systemRevisions(roles.getFirst());
        if (revisions.isEmpty()) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return revisions.getFirst();
    }

    private long latestDefault(DefaultPolicyKind kind) {
        List<Long> revisions = catalog.defaultRevisions(kind);
        if (revisions.isEmpty()) {
            throw new BizException(IamReasonCode.POLICY_CONFLICT);
        }
        return revisions.getFirst();
    }

    private List<ApplicationSummary> baselineApplications() {
        return catalog.baselineApplications();
    }

    private List<ApplicationSummary> planApplications(String planIdValue) {
        long planId = parseId(planIdValue, IamReasonCode.APPLICATION_UNAVAILABLE);
        if (!catalog.activePlan(planId)) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        List<ApplicationSummary> applications = catalog.planApplications(planId);
        if (catalog.planApplicationCount(planId) != applications.size()) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        return applications;
    }

    private static long parseId(String value, IamReasonCode failure) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) {
                throw new BizException(failure);
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new BizException(failure);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * <p>保存经服务器验证的组织初始化目录引用。</p>
     * @author jy
     * @since 1.0.0
     */
    private record CatalogSelection(String name, long ownerAccountId, String ownerDisplayName, String rootDepartmentName,
                                   long governanceRevisionId, long directoryRevisionId, long fieldRevisionId,
                                   List<ApplicationSummary> applications) {
        List<Long> applicationIds() {
            return applications.stream().map(application -> Long.parseLong(application.id())).toList();
        }
    }
}
