package com.ingot.cloud.iam.web.v1.tenant;

import java.time.Instant;
import java.util.List;

import com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest;
import com.ingot.cloud.iam.diagnose.DiagnoseAuditService;
import com.ingot.cloud.iam.diagnose.AuthorizationAuditQuery;
import com.ingot.cloud.iam.diagnose.TenantDiagnoseCandidates;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuditEntry;
import com.ingot.framework.commons.model.iam.AuthorizationCandidateKind;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Decision;
import com.ingot.framework.commons.model.iam.DiagnoseInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供租户受限授权诊断与脱敏审计。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户授权诊断")
@RequestMapping("/v1/tenant/authorization")
@RequiredArgsConstructor
public class TenantAuthorizationAPI implements RShortcuts {
    private final DiagnoseAuditService diagnoses;
    private final TenantDiagnoseCandidates candidates;

    /**
     * 对租户成员做只读诊断。
     *
     * @param input 诊断目标
     * @return 受限解释
     */
    @Operation(summary = "受限授权诊断")
    @PostMapping("/diagnose")
    public R<Decision> diagnose(@Valid @RequestBody DiagnoseInput input) {
        return ok(diagnoses.diagnose(AuthorizationDomain.TENANT, input));
    }

    /**
     * 查询诊断候选，口径与诊断校验一致。
     *
     * @param kind 候选类型：成员、应用、操作、目标对象
     * @param applicationId 操作所属应用，查询操作时必填
     * @param actionId 目标对象所属操作，查询对象时必填
     * @param keyword 名称包含匹配
     * @param ids 已选回显
     * @param page 页码
     * @param pageSize 页大小
     * @return 候选页
     */
    @IamReadOnlyRequest
    @Operation(summary = "诊断候选")
    @GetMapping("/diagnose/candidates")
    public R<AuthorizationCandidatePage> diagnoseCandidates(@RequestParam AuthorizationCandidateKind kind,
            @RequestParam(required = false) String applicationId, @RequestParam(required = false) String actionId,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) List<String> ids,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(candidates.list(kind, applicationId, actionId, keyword, ids, page, pageSize));
    }

    /**
     * 分页列出租户脱敏审计。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param actorMemberId 操作者成员 ID，精确匹配
     * @param targetType 目标资源编码，精确匹配
     * @param targetId 目标 ID，精确匹配
     * @param changeType 变更类型
     * @param from 时间起点，包含，ISO-8601 带时区
     * @param until 时间终点，不包含，ISO-8601 带时区
     * @return 审计页
     */
    @Operation(summary = "租户审计")
    @GetMapping("/audits")
    public R<PageResponse<ResourceDetail<AuditEntry>>> audits(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String actorMemberId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant until) {
        return ok(diagnoses.listAudits(AuthorizationDomain.TENANT, page, pageSize,
                AuthorizationAuditQuery.of(actorMemberId, targetType, targetId, changeType, from, until)));
    }
}
