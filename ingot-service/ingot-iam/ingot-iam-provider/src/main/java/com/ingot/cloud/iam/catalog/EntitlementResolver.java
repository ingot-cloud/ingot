package com.ingot.cloud.iam.catalog;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.persistence.CatalogRepository;
import com.ingot.cloud.iam.persistence.InitializationCatalogRepository;
import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ApplicationSummary;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.EntitlementDraft;
import com.ingot.framework.commons.model.iam.EntitlementSource;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按套餐与自选应用计算租户开通并集，不把客户端清单当作完整目录。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class EntitlementResolver {
    private final InitializationCatalogRepository initialization;
    private final CatalogRepository catalog;

    /**
     * 计算套餐应用、自选覆盖与必要 baseline 的并集。
     *
     * @param planId 可选套餐
     * @param extras 自选应用及期限覆盖，可空
     * @return 解析后的开通行
     * @throws BizException 套餐无效、应用不可用或基础治理入口被关闭
     */
    public List<ResolvedEntitlement> resolve(String planId, List<EntitlementDraft> extras) {
        Instant now = Instant.now();
        List<EntitlementDraft> drafts = extras == null ? List.of() : extras;
        Set<String> extraIds = new LinkedHashSet<>();
        for (EntitlementDraft draft : drafts) {
            if (!extraIds.add(draft.applicationId())) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
        Long parsedPlan = null;
        List<ApplicationSummary> planApps = List.of();
        if (planId != null && !planId.isBlank()) {
            parsedPlan = parseId(planId);
            if (!initialization.activePlan(parsedPlan)) {
                throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
            }
            planApps = initialization.planApplications(parsedPlan);
            if (initialization.planApplicationCount(parsedPlan) != planApps.size()) {
                throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
            }
        }
        Map<String, ApplicationSummary> planById = new LinkedHashMap<>();
        for (ApplicationSummary application : planApps) {
            planById.put(application.id(), application);
        }
        Map<String, ResolvedEntitlement> union = new LinkedHashMap<>();
        for (EntitlementDraft draft : drafts) {
            IamApplicationEntity entity = requireTenantEnabled(draft.applicationId());
            if (Boolean.TRUE.equals(entity.getBaseline()) && draft.status() == ConfigurationStatus.DISABLED) {
                throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
            }
            boolean inPlan = planById.containsKey(text(entity.getId()));
            union.put(text(entity.getId()), new ResolvedEntitlement(summary(entity), draft.status(),
                    inPlan ? EntitlementSource.PLAN : EntitlementSource.MANUAL,
                    inPlan ? Long.toString(parsedPlan) : null,
                    draft.validFrom() == null ? now : draft.validFrom(), draft.validUntil()));
        }
        for (ApplicationSummary application : planApps) {
            if (union.containsKey(application.id())) {
                continue;
            }
            union.put(application.id(), new ResolvedEntitlement(application, ConfigurationStatus.ENABLED,
                    EntitlementSource.PLAN, Long.toString(parsedPlan), now, null));
        }
        boolean selected = !planById.isEmpty() || !drafts.isEmpty();
        List<ApplicationSummary> baseline = initialization.baselineApplications();
        if (!selected) {
            if (baseline.isEmpty()) {
                throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
            }
            for (ApplicationSummary application : baseline) {
                union.put(application.id(), new ResolvedEntitlement(application, ConfigurationStatus.ENABLED,
                        EntitlementSource.INITIALIZATION, null, now, null));
            }
        } else {
            for (ApplicationSummary application : baseline) {
                union.putIfAbsent(application.id(), new ResolvedEntitlement(application, ConfigurationStatus.ENABLED,
                        EntitlementSource.INITIALIZATION, null, now, null));
            }
        }
        if (union.isEmpty()) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        return List.copyOf(union.values());
    }

    private IamApplicationEntity requireTenantEnabled(String applicationId) {
        long id = parseId(applicationId);
        IamApplicationEntity entity = catalog.findApplication(id);
        if (entity == null || entity.getDomain() != AuthorizationDomain.TENANT
                || !Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        return entity;
    }

    private static ApplicationSummary summary(IamApplicationEntity entity) {
        return new ApplicationSummary(text(entity.getId()), entity.getCode(), entity.getName(), entity.getIcon(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder());
    }

    private static long parseId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) {
                throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
    }

    private static String text(java.math.BigInteger id) {
        return id == null ? null : id.toString();
    }
}
