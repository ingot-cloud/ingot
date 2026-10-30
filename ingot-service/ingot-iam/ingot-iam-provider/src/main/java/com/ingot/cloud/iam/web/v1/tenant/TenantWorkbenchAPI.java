package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest;
import com.ingot.cloud.iam.tenant.TenantWorkbenchService;
import com.ingot.framework.commons.model.iam.TenantWorkbenchStatistics;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供当前租户工作台统计，每项指标独立遵循已有资源读取资格与范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/tenant/workbench")
@Tag(name = "IAM 租户工作台")
@RequiredArgsConstructor
public class TenantWorkbenchAPI implements RShortcuts {
    private final TenantWorkbenchService workbench;

    /**
     * 汇总当前可信租户的可见统计，不接受外部指定组织或替代数据范围。
     *
     * @return 当前组织统计，无读取资格的指标省略
     */
    @GetMapping("/statistics")
    @IamReadOnlyRequest
    @Operation(summary = "租户工作台统计")
    public R<TenantWorkbenchStatistics> statistics() {
        return ok(workbench.statistics());
    }
}
