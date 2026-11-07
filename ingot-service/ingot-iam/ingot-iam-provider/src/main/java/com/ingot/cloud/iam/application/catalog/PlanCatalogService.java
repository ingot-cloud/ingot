package com.ingot.cloud.iam.application.catalog;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlanEntity;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.PlanApplication;
import com.ingot.cloud.iam.application.catalog.model.PlanDraft;
import com.ingot.cloud.iam.application.catalog.model.PlanRecord;
import com.ingot.cloud.iam.application.catalog.model.PlanSummary;
import com.ingot.cloud.iam.application.catalog.model.PlanUpdateInput;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>维护对应目录对象并保留原授权、审计与事务顺序。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlanCatalogService {
    private final CatalogOperations operationsContext;

    /**
     * 分页列出套餐。
     * @param page 页码
     * @param pageSize 页大小
     * @param name 套餐名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @return 套餐页
     */
    public PageResponse<ResourceDetail<PlanRecord>> listPlans(int page, int pageSize, String name, String status) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_READ);
        IamPages.require(page, pageSize);
        Page<IamPlanEntity> rows = operationsContext.catalog().pagePlans(page, pageSize, name, IamFilters.enabledOf(status));
        List<ResourceDetail<PlanRecord>> items = new ArrayList<>();
        for (IamPlanEntity row : rows.getRecords()) {
            List<BigInteger> applicationIds = operationsContext.catalog().planApplicationIds(row.getId().longValue());
            items.add(ResourceDetailFactory.of(plan(row, CatalogOperations.texts(applicationIds), planApplications(applicationIds)),
                    CatalogOperations.version(row.getVersion())));
        }
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 分页列出套餐选择器所需的最小标识，不附带应用清单。
     * @param page 页码
     * @param pageSize 页大小
     * @param name 套餐名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @return 套餐摘要页
     */
    public PageResponse<ResourceDetail<PlanSummary>> listPlanSummaries(int page, int pageSize, String name,
            String status) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_READ);
        IamPages.require(page, pageSize);
        Page<IamPlanEntity> rows = operationsContext.catalog().pagePlans(page, pageSize, name, IamFilters.enabledOf(status));
        List<ResourceDetail<PlanSummary>> items = rows.getRecords()
            .stream()
            .map(row -> ResourceDetailFactory.of(new PlanSummary(CatalogOperations.text(row.getId()), row.getName()), CatalogOperations.version(row.getVersion())))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 读取套餐详情。
     * @param id 套餐 ID
     * @return 套餐详情
     */
    public ResourceDetail<PlanRecord> getPlan(String id) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_READ);
        return loadPlan(IamIds.require(id));
    }

    /**
     * 创建套餐。修改套餐不会改变既有租户开通。
     * @param input 套餐草稿
     * @return 新套餐 ID
     */
    public CreatedResource createPlan(PlanDraft input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_CREATE);
        return operationsContext.transaction().execute(status -> {
            long id = operationsContext.access().nextId();
            IamPlanEntity entity = new IamPlanEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setName(input.name());
            entity.setDescription(CatalogOperations.nullable(input.description()));
            entity.setEnabled(input.status() != ConfigurationStatus.DISABLED);
            operationsContext.catalog().insertPlan(entity);
            replacePlanApplications(id, input.applicationIds());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.PLAN, IamIds.text(id), AuditChangeType.CREATE, Map.of(),
                    Map.of(AuditField.NAME, input.name()), Map.of(CatalogOperations.PLAN, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 整体替换套餐应用清单，不影响已开通租户。
     * @param id 套餐 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<PlanRecord> updatePlan(String id, PlanUpdateInput input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_UPDATE);
        long planId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            IamPlanEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockPlan(planId));
            IamIds.requireVersion(input.expectedVersion(), CatalogOperations.version(current.getVersion()));
            ConfigurationStatus nextStatus = input.plan().status() == null ? CatalogOperations.statusOf(current.getEnabled())
                    : input.plan().status();
            operationsContext.catalog().updatePlan(planId, input.plan().name(), CatalogOperations.nullable(input.plan().description()),
                    nextStatus == ConfigurationStatus.ENABLED, current.getVersion());
            replacePlanApplications(planId, input.plan().applicationIds());
            String next = CatalogOperations.nextVersion(current.getVersion());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.PLAN, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName(), AuditField.STATUS,
                            CatalogOperations.statusOf(current.getEnabled()).name()),
                    Map.of(AuditField.NAME, input.plan().name(), AuditField.STATUS, nextStatus.name()),
                    Map.of(CatalogOperations.PLAN, next));
            return loadPlan(planId);
        });
    }

    private ResourceDetail<PlanRecord> loadPlan(long id) {
        IamPlanEntity row = operationsContext.catalog().findPlan(id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        List<BigInteger> applicationIds = operationsContext.catalog().planApplicationIds(id);
        return ResourceDetailFactory.of(plan(row, CatalogOperations.texts(applicationIds), planApplications(applicationIds)),
                CatalogOperations.version(row.getVersion()));
    }

    private void replacePlanApplications(long planId, List<String> applicationIds) {
        List<Long> ids = new ArrayList<>();
        if (applicationIds != null) {
            for (String applicationId : applicationIds) {
                long id = IamIds.require(applicationId);
                operationsContext.requireApplication(id);
                ids.add(id);
            }
        }
        operationsContext.catalog().replacePlanApplications(planId, applicationIds == null ? null : ids);
    }

    private List<PlanApplication> planApplications(List<BigInteger> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return List.of();
        }
        Map<String, IamApplicationEntity> byId = new LinkedHashMap<>();
        for (IamApplicationEntity row : operationsContext.catalog()
            .listApplicationsByIds(applicationIds.stream().map(BigInteger::longValue).toList())) {
            byId.putIfAbsent(CatalogOperations.text(row.getId()), row);
        }
        List<PlanApplication> result = new ArrayList<>();
        for (BigInteger applicationId : applicationIds) {
            IamApplicationEntity row = byId.get(CatalogOperations.text(applicationId));
            if (row != null) {
                result.add(new PlanApplication(CatalogOperations.text(row.getId()), row.getCode(), row.getName(),
                        CatalogOperations.statusOf(row.getEnabled())));
            }
        }
        return result;
    }

    private static PlanRecord plan(IamPlanEntity row, List<String> applicationIds, List<PlanApplication> applications) {
        return new PlanRecord(CatalogOperations.text(row.getId()), row.getName(), row.getDescription(), applicationIds, applications,
                CatalogOperations.statusOf(row.getEnabled()));
    }
}
