package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;

/**
 * <p>批量升级预览及保存结果。</p>
 *
 * @param targetRevisionRef 目标固定版本
 * @param items 逐条结果
 * @author jy
 * @since 1.0.0
 */
public record AssignmentUpgradeResult(RoleRevisionRef targetRevisionRef, List<AssignmentUpgradePreviewItem> items) {

}
