package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.policy.PolicyService;
import com.ingot.framework.commons.model.iam.FieldPolicyDraft;
import com.ingot.framework.commons.model.iam.FieldPolicyInput;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>维护租户成员字段策略。</p>
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
     * 读取字段策略。
     *
     * @return 策略详情
     */
    @Operation(summary = "读取字段策略")
    @GetMapping("/fields")
    public R<ResourceDetail<FieldPolicyDraft>> getFields() {
        return ok(policies.getFieldPolicy());
    }

    /**
     * 整体替换字段策略。
     *
     * @param input 完整配置
     * @return 提交后详情
     */
    @Operation(summary = "整体替换字段策略")
    @PutMapping("/fields")
    public R<ResourceDetail<FieldPolicyDraft>> putFields(@Valid @RequestBody FieldPolicyInput input) {
        return ok(policies.putFieldPolicy(input));
    }
}
