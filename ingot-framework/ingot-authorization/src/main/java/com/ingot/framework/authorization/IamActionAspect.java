package com.ingot.framework.authorization;

import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * <p>方法准入统一委托SDK，不匹配通配权限或管理员角色名。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Aspect
@RequiredArgsConstructor
public class IamActionAspect {

    private final AuthorizationAccess access;

    /**
     * 执行方法精确准入。
     * @param point 调用
     * @param action 注解
     * @return 业务结果
     * @throws Throwable 业务失败
     */
    @Around("@annotation(action)")
    public Object authorize(ProceedingJoinPoint point, RequireIamAction action) throws Throwable {
        access.require(new ResourceKey(action.domain(), action.application(), action.resource()), action.value());
        return point.proceed();
    }

}
