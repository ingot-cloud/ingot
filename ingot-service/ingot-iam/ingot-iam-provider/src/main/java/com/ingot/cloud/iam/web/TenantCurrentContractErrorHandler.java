package com.ingot.cloud.iam.web;
import com.ingot.cloud.iam.web.v1.platform.PlatformSharedRoleCommandAPI;
import com.ingot.cloud.iam.web.v1.tenant.TenantRoleCommandAPI;
import com.ingot.cloud.iam.web.v1.tenant.TenantAssignmentAPI;
import com.ingot.cloud.iam.web.v1.tenant.TenantDelegationAPI;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.support.R;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
/**
 * <p>只为当前角色与租户授权的新契约映射格式错误，不改变平台固定版本错误协议。</p>
 * @author jy
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {TenantRoleCommandAPI.class, PlatformSharedRoleCommandAPI.class,
        TenantAssignmentAPI.class, TenantDelegationAPI.class})
public class TenantCurrentContractErrorHandler {
    /** 对非法结构、旧输入字段和参数类型返回稳定400，不返回请求原值。 */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<R<?>> invalidArgument() {
        var reason = IamReasonCode.INVALID_ARGUMENT;
        return ResponseEntity.status(reason.getHttpStatus()).body(R.error(reason.getCode(), reason.getText()));
    }

    /** 当前授权依赖失败时返回503；事务并发冲突返回409，不使用旧允许结果。 */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<R<?>> unavailable(org.springframework.dao.DataAccessException error) {
        var reason = error instanceof org.springframework.dao.ConcurrencyFailureException
                ? IamReasonCode.REVISION_CONFLICT : IamReasonCode.AUTHORIZATION_UNAVAILABLE;
        return ResponseEntity.status(reason.getHttpStatus()).body(R.error(reason.getCode(), reason.getText()));
    }
}
