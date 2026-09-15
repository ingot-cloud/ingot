package com.ingot.cloud.iam.authorization.migration;

import com.ingot.cloud.iam.api.model.enums.MigrationBlockReasonEnum;

/**
 * <p>迁移阻断项，apply 前必须清零。</p>
 *
 * @param reason  阻断类别
 * @param subject 对象标识，如菜单 ID 或绑定键
 * @param detail  人类可读说明
 * @author jy
 * @since 1.0.0
 */
public record MigrationBlockingIssue(
        MigrationBlockReasonEnum reason,
        String subject,
        String detail
) {
}
