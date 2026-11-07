package com.ingot.cloud.iam.interfaces.http.tenant;

import java.util.List;

import com.ingot.cloud.iam.application.policy.PolicyService;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyDraft;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyInput;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyResource;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>维护已接入资源的租户字段策略。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户策略")
@RequestMapping("/v1/tenant/policies")
@RequiredArgsConstructor
public class TenantPolicyAPI implements RShortcuts {
    private final PolicyService policies;

    /**
     * 列出当前租户已接入字段策略的资源。
     *
     * @return 资源、字段能力与可选动作
     */
    @Operation(summary = "列出已接入字段策略的资源")
    @GetMapping("/field-resources")
    public R<List<FieldPolicyResource>> listFieldPolicyResources() {
        return ok(policies.listFieldPolicyResources());
    }

    /**
     * 读取一份资源的字段策略。
     *
     * @param resourceId 资源 ID
     * @return 策略详情
     */
    @Operation(summary = "读取字段策略")
    @GetMapping("/fields/{resourceId}")
    public R<ResourceDetail<FieldPolicyDraft>> getFieldPolicy(@PathVariable String resourceId) {
        return ok(policies.getFieldPolicy(resourceId));
    }

    /**
     * 整体替换一份资源的字段策略。
     *
     * @param resourceId 资源 ID
     * @param input 完整配置
     * @return 提交后详情
     */
    @Operation(summary = "整体替换字段策略")
    @PutMapping("/fields/{resourceId}")
    public R<ResourceDetail<FieldPolicyDraft>> putFieldPolicy(@PathVariable String resourceId,
            @Valid @RequestBody FieldPolicyInput input) {
        return ok(policies.putFieldPolicy(resourceId, input));
    }
}
