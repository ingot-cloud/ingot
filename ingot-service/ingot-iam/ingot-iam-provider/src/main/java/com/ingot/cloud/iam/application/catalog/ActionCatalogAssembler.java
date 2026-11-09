package com.ingot.cloud.iam.application.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.application.catalog.model.ActionCatalogItem;
import com.ingot.cloud.iam.application.catalog.model.ActionCatalogResource;
import com.ingot.cloud.iam.application.catalog.model.ActionCatalogView;
import com.ingot.cloud.iam.infrastructure.persistence.projection.ActionCatalogRow;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>将单条目录查询结果组装为平台或租户的操作目录视图。</p>
 * @author jy
 * @since 1.0.0
 */
final class ActionCatalogAssembler {
    private ActionCatalogAssembler() {
    }

    static ActionCatalogView assemble(List<ActionCatalogRow> rows, boolean tenantView) {
        if (rows.isEmpty()) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }

        Map<String, Resource> resources = new LinkedHashMap<>();
        for (ActionCatalogRow row : rows) {
            if (row.resourceId() == null || (tenantView && row.actionId() == null)) {
                continue;
            }
            String resourceId = row.resourceId().toString();
            Resource resource = resources.computeIfAbsent(resourceId, id ->
                    new Resource(id, row.resourceCode(), label(row.resourceName(), row.resourceCode(), tenantView),
                            new ArrayList<>()));
            if (row.actionId() != null) {
                resource.actions().add(new ActionCatalogItem(row.actionId().toString(), resourceId, row.actionCode(),
                        label(row.actionName(), row.actionCode(), tenantView), CatalogOperations.statusOf(row.actionEnabled())));
            }
        }
        ActionCatalogRow application = rows.getFirst();
        return new ActionCatalogView(application.applicationId().toString(), application.applicationCode(),
                label(application.applicationName(), application.applicationCode(), tenantView),
                resources.values().stream()
                        .map(row -> new ActionCatalogResource(row.id(), row.code(), row.name(), row.actions()))
                        .toList());
    }

    private static String label(String name, String code, boolean tenantView) {
        return tenantView && (name == null || name.isBlank()) ? code : name;
    }

    /**
     * 按查询顺序积累资源下的操作。
     *
     * @author jy
     * @since 1.0.0
     */
    private record Resource(String id, String code, String name, List<ActionCatalogItem> actions) {
    }
}
