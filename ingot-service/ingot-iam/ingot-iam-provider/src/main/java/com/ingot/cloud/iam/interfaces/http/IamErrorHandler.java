package com.ingot.cloud.iam.interfaces.http;

import java.util.Map;

import com.ingot.framework.commons.error.BizException;
import jakarta.servlet.http.HttpServletRequest;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;

/**
 * <p>把 IAM 对外管理面的稳定业务错误码映射为契约约定的 HTTP 状态，不改变其他服务的错误协议。</p>
 *
 * <p>作用于 {@code /v1} 管理面及 v2 资源求值控制器：命中 {@link IamReasonCode} 时按其声明的状态返回，
 * 其余业务错误码沿用框架全局处理器的既有 500 行为，避免为 IAM 改写全仓库 {@link BizException} 语义。
 * 其他内部 RPC 不在范围内，仍以 R 信封回传错误码供调用方解析。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see com.ingot.framework.core.error.GlobalExceptionHandlerResolver
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ingot.cloud.iam.interfaces.http",
        assignableTypes = com.ingot.cloud.iam.interfaces.inner.InnerResourceAuthorizationAPI.class)
public class IamErrorHandler {
    private static final Map<String, String> DETAIL_OBJECTS = Map.ofEntries(
            Map.entry("/v1/platform/applications/{id}", "应用"),
            Map.entry("/v1/platform/applications/{id}/action-catalog", "应用"),
            Map.entry("/v1/platform/accounts/{id}", "账号"),
            Map.entry("/v1/platform/tenants/{id}", "组织"),
            Map.entry("/v1/platform/members/{id}", "成员"),
            Map.entry("/v1/tenant/members/{id}", "成员"),
            Map.entry("/v1/tenant/departments/{id}", "部门"),
            Map.entry("/v1/platform/groups/{id}", "用户组"),
            Map.entry("/v1/tenant/groups/{id}", "用户组"),
            Map.entry("/v1/platform/roles/{id}", "角色"),
            Map.entry("/v1/platform/roles/{id}/revisions/{revisionId}", "角色版本"),
            Map.entry("/v1/platform/roles/{id}/revisions/{revisionId}/grants", "角色版本"),
            Map.entry("/v1/platform/roles/{roleId}/version-applications", "可应用版本的平台角色"),
            Map.entry("/v1/platform/roles/{roleId}/version-applications/distribution", "可应用版本的平台角色"),
            Map.entry("/v1/platform/shared-roles/{id}", "角色"),
            Map.entry("/v1/tenant/roles/{id}", "角色"),
            Map.entry("/v1/tenant/delegations/{id}", "委派记录"));

    private static final String ASSIGNMENT = "角色分配";
    private static final Map<String, String> WRITE_OBJECTS = Map.ofEntries(
            Map.entry("/v1/platform/applications", "应用"),
            Map.entry("/v1/platform/applications/bundles", "应用"),
            Map.entry("/v1/platform/applications/{id}", "应用"),
            Map.entry("/v1/platform/applications/{id}/menus", "菜单"),
            Map.entry("/v1/platform/applications/{id}/menus/{menuId}", "菜单"),
            Map.entry("/v1/platform/applications/{id}/resources", "应用资源"),
            Map.entry("/v1/platform/applications/{id}/resources/{resourceId}", "应用资源"),
            Map.entry("/v1/platform/applications/{id}/actions", "权限操作"),
            Map.entry("/v1/platform/applications/{id}/actions/{actionId}", "权限操作"),
            Map.entry("/v1/tenant/applications/{id}/audience", "应用使用范围"),
            Map.entry("/v1/platform/roles", "角色"),
            Map.entry("/v1/platform/roles/{id}", "角色"),
            Map.entry("/v1/platform/roles/{id}/revisions", "角色版本"),
            Map.entry("/v1/platform/roles/{id}/revisions/{revisionId}/metadata", "角色版本"),
            Map.entry("/v1/platform/shared-roles", "共享角色"),
            Map.entry("/v1/platform/shared-roles/{id}", "共享角色"),
            Map.entry("/v1/platform/shared-roles/{id}/grants", "共享角色权限"),
            Map.entry("/v1/tenant/roles", "组织角色"),
            Map.entry("/v1/tenant/roles/{id}", "组织角色"),
            Map.entry("/v1/tenant/roles/{id}/grants", "组织角色权限"),
            Map.entry("/v1/platform/assignments", ASSIGNMENT),
            Map.entry("/v1/platform/assignments/{id}", ASSIGNMENT),
            Map.entry("/v1/tenant/assignments", ASSIGNMENT),
            Map.entry("/v1/tenant/assignments/{id}", ASSIGNMENT));

    /**
     * 按稳定错误码映射 IAM 业务错误的 HTTP 状态，详情不可见时返回与对象不存在相同的文案。
     *
     * @param exception 业务异常
     * @param request 当前 HTTP 请求
     * @return 保持 R 信封的错误响应；非 IAM 错误码沿用 500
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<?>> bizException(BizException exception, HttpServletRequest request) {
        IamReasonCode reason = IamReasonCode.find(exception.getCode());
        if (reason == null) {
            log.error("[IamErrorHandler] 非 IAM 业务错误码 code={}", exception.getCode(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(R.error(exception.getCode(), BaseErrorCode.INTERNAL_SERVER_ERROR.getText()));
        }
        log.warn("[IamErrorHandler] code={} status={} message={}",
                reason.getCode(), reason.getHttpStatus(), exception.getLocalizedMessage());
        String message = exception.getLocalizedMessage();
        if (message == null || message.isBlank()) {
            message = reason.getText();
        }
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null && message.equals(reason.getText())) {
            message = withContext(reason, pattern.toString(), request.getMethod());
        }
        return ResponseEntity.status(reason.getHttpStatus()).body(R.error(reason.getCode(), message));
    }

    /**
     * 缺少必填查询参数时返回 IAM 的稳定参数错误。
     *
     * @return HTTP 400 及参数错误信封
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<?>> missingRequestParameter() {
        var reason = IamReasonCode.INVALID_ARGUMENT;
        return ResponseEntity.status(reason.getHttpStatus()).body(R.error(reason.getCode(), reason.getText()));
    }

    private static String withContext(IamReasonCode reason, String pattern, String method) {
        if (HttpMethod.GET.matches(method)) {
            String object = DETAIL_OBJECTS.get(pattern);
            return object != null && reason == IamReasonCode.OBJECT_NOT_FOUND
                    ? object + "不存在或不可访问" : reason.getText();
        }
        String object = WRITE_OBJECTS.get(pattern);
        if (object == null) return reason.getText();
        String operation = switch (method) {
            case "POST" -> "新增";
            case "PUT", "PATCH" -> "更新";
            case "DELETE" -> ASSIGNMENT.equals(object) ? "撤销" : "删除";
            default -> null;
        };
        if (operation == null) return reason.getText();
        String explanation = switch (reason) {
            case OBJECT_NOT_FOUND -> "目标或关联对象不存在或不可访问，请刷新后重试";
            case INVALID_ARGUMENT -> "提交的内容不符合要求，请检查表单或重新选择关联对象";
            case REVISION_CONFLICT -> "记录已变化，请重新打开最新记录后再提交";
            default -> reason.getText();
        };
        return "无法" + operation + object + "，" + explanation;
    }
}
