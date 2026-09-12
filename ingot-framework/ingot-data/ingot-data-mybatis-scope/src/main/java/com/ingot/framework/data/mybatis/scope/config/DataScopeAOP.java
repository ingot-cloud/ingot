package com.ingot.framework.data.mybatis.scope.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationResourceRuleDTO;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotHolder;
import com.ingot.framework.data.mybatis.scope.context.DataScopeContextHolder;
import com.ingot.framework.data.mybatis.scope.error.DataScopeErrorCode;
import com.ingot.framework.data.mybatis.scope.error.DataScopeException;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * <p>按注解上的资源与操作从授权快照压入数据范围上下文，方法结束时弹出以支持嵌套。</p>
 *
 * <p>不判定功能准入；无匹配规则时压入空帧，由 SQL 谓词写成 {@code 1=2}。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see DataScope
 */
@Slf4j
@Aspect
public class DataScopeAOP {

    /**
     * 匹配 {@link DataScope} 方法。
     */
    @Pointcut("@annotation(com.ingot.framework.data.mybatis.scope.config.DataScope)")
    public void dataScope() {
    }

    /**
     * 解析快照规则并建立当前资源的行过滤上下文。
     *
     * @param point 切点
     * @return 原方法结果
     * @throws Throwable 原方法或授权失败
     */
    @Around("dataScope()")
    public Object dataScopeMethod(ProceedingJoinPoint point) throws Throwable {
        DataScope annotation = resolveAnnotation(point);
        InUser user = SecurityAuthContext.getUser();
        if (user == null || user.getId() == null) {
            throw new DataScopeException(DataScopeErrorCode.DS_401);
        }
        AuthorizationSnapshotDTO snapshot = AuthorizationSnapshotHolder.get();
        if (snapshot == null) {
            throw new DataScopeException(DataScopeErrorCode.DS_503);
        }
        DataScopeContextHolder.Frame frame = buildFrame(
                annotation.resource(), annotation.permission(), snapshot, user.getId());
        DataScopeContextHolder.push(frame);
        try {
            return point.proceed();
        } finally {
            DataScopeContextHolder.pop();
        }
    }

    private static DataScope resolveAnnotation(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataScope annotation = signature.getMethod().getAnnotation(DataScope.class);
        if (annotation == null || StrUtil.isBlank(annotation.resource()) || StrUtil.isBlank(annotation.permission())) {
            throw new DataScopeException(DataScopeErrorCode.DS_403);
        }
        return annotation;
    }

    /**
     * 按 {@code (resource, permission)} 合并快照规则；无匹配则空范围，不抛功能 403。
     *
     * @param resource   资源编码
     * @param permission 规则键
     * @param snapshot   当前请求快照
     * @param userId     当前用户，供 SELF
     * @return 范围帧
     */
    static DataScopeContextHolder.Frame buildFrame(String resource,
                                                   String permission,
                                                   AuthorizationSnapshotDTO snapshot,
                                                   long userId) {
        List<AuthorizationResourceRuleDTO> matched = new ArrayList<>();
        for (AuthorizationResourceRuleDTO rule : CollUtil.emptyIfNull(snapshot.getResourceRules())) {
            if (resource.equals(rule.getResourceCode())
                    && permission.equals(rule.getPermissionCode())) {
                matched.add(rule);
            }
        }
        if (matched.isEmpty()) {
            return DataScopeContextHolder.Frame.of(resource, permission, false, List.of(), null);
        }
        boolean all = matched.stream().anyMatch(rule -> rule.getScopeType() == DataScopeTypeEnum.ALL);
        if (all) {
            return DataScopeContextHolder.Frame.of(resource, permission, true, List.of(), null);
        }
        boolean self = matched.stream().anyMatch(rule -> BooleanUtil.isTrue(rule.getSelf()));
        Set<Long> deptIds = new LinkedHashSet<>();
        for (AuthorizationResourceRuleDTO rule : matched) {
            deptIds.addAll(CollUtil.emptyIfNull(rule.getDeptIds()));
        }
        return DataScopeContextHolder.Frame.of(
                resource,
                permission,
                false,
                new ArrayList<>(deptIds),
                self ? userId : null);
    }
}
