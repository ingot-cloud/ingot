package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.policy.PolicyService;
import com.ingot.framework.commons.model.iam.DirectoryPolicyDraft;
import com.ingot.framework.commons.model.iam.DirectoryPolicyInput;
import com.ingot.framework.commons.model.iam.FieldPolicyDraft;
import com.ingot.framework.commons.model.iam.FieldPolicyInput;
import com.ingot.framework.commons.model.iam.PolicyPreviewInput;
import com.ingot.framework.commons.model.iam.PolicyPreviewResult;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>维护租户通讯录与字段策略，预览无写入。</p>
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
     * 读取通讯录策略。
     *
     * @return 策略详情
     */
    @Operation(summary = "读取通讯录策略")
    @GetMapping("/directory")
    public R<ResourceDetail<DirectoryPolicyDraft>> getDirectory() {
        return ok(policies.getDirectoryPolicy());
    }

    /**
     * 整体替换通讯录策略。
     *
     * @param input 完整配置
     * @return 提交后详情
     */
    @Operation(summary = "整体替换通讯录策略")
    @PutMapping("/directory")
    public R<ResourceDetail<DirectoryPolicyDraft>> putDirectory(@Valid @RequestBody DirectoryPolicyInput input) {
        return ok(policies.putDirectoryPolicy(input));
    }

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

    /**
     * 只读策略预览。
     *
     * @param input 草稿与查看者
     * @return 可见样例
     */
    @Operation(summary = "只读策略预览")
    @PostMapping("/preview")
    public R<Preview<PolicyPreviewResult>> preview(@Valid @RequestBody PolicyPreviewInput input) {
        return ok(policies.preview(input));
    }
}
