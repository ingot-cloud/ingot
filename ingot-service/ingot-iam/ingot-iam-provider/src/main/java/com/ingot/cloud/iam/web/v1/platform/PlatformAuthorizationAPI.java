package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.commons.model.iam.IamAction;

import java.time.Instant;

import com.ingot.cloud.iam.diagnose.AuthorizationAuditService;
import com.ingot.cloud.iam.diagnose.DiagnoseAuditService;
import com.ingot.cloud.iam.diagnose.AuthorizationAuditQuery;
import com.ingot.framework.commons.model.iam.AuditCursorPage;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Decision;
import com.ingot.framework.commons.model.iam.DiagnoseInput;
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
 * <p>提供平台受限授权诊断与脱敏审计。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台授权诊断")
@RequestMapping("/v1/platform/authorization")
@RequiredArgsConstructor
public class PlatformAuthorizationAPI implements RShortcuts {
    private final DiagnoseAuditService diagnoses;
    private final AuthorizationAuditService audits;

    /**
     * 对平台成员做只读诊断。
     *
     * @param input 诊断目标
     * @return 受限解释
     */
    @Operation(summary = "受限授权诊断")
    @PostMapping("/diagnose")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE)
    public R<Decision> diagnose(@Valid @RequestBody DiagnoseInput input) {
        return ok(diagnoses.diagnose(AuthorizationDomain.PLATFORM, input));
    }

    /**
     * 按游标列出平台脱敏审计，不返回总数。
     *
     * @param limit 每页条数，1–100
     * @param cursor 上一页返回的 nextCursor，空表示第一页
     * @param actorMemberId 操作者成员 ID，精确匹配
     * @param targetType 目标资源编码，精确匹配
     * @param targetId 目标 ID，精确匹配
     * @param changeType 变更类型
     * @param from 时间起点，包含，ISO-8601 带时区
     * @param until 时间终点，不包含，ISO-8601 带时区
     * @return 审计游标页及对象名称
     */
    @Operation(summary = "平台审计")
    @GetMapping("/audits")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_AUDIT_READ)
    public R<AuditCursorPage> audits(
            @RequestParam(defaultValue = "" + AuthorizationAuditService.DEFAULT_LIMIT) int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String actorMemberId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant until) {
        return ok(audits.list(AuthorizationDomain.PLATFORM, limit, cursor,
                AuthorizationAuditQuery.of(actorMemberId, targetType, targetId, changeType, from, until)));
    }
}
