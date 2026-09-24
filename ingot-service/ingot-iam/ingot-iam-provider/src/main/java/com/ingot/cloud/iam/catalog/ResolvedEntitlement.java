package com.ingot.cloud.iam.catalog;

import java.time.Instant;

import com.ingot.framework.commons.model.iam.ApplicationSummary;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.EntitlementPreviewItem;
import com.ingot.framework.commons.model.iam.EntitlementSource;

/**
 * <p>保存服务器解析后的单条开通并集，供创建预览与整表替换共用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param application 已确认可开通的租户域应用
 * @param status 显式开通或停用
 * @param source 开通来源
 * @param sourceId 套餐 ID，仅 PLAN 时有值
 * @param validFrom 生效时间
 * @param validUntil 失效时间，可空
 */
public record ResolvedEntitlement(ApplicationSummary application, ConfigurationStatus status, EntitlementSource source,
                                 String sourceId, Instant validFrom, Instant validUntil) {

    /**
     * 转为对外预览项。
     *
     * @return 含名称与来源的开通预览
     */
    public EntitlementPreviewItem toPreview() {
        return new EntitlementPreviewItem(application.id(), application.name(), status, source, sourceId, validFrom,
                validUntil);
    }
}
