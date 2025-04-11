package com.ingot.framework.data.mybatis.scope.config;

import cn.hutool.core.collection.CollUtil;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.rpc.PmsDataScopeService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.data.mybatis.scope.context.DataScopeContextHolder;
import com.ingot.framework.data.mybatis.scope.error.DataScopeErrorCode;
import com.ingot.framework.data.mybatis.scope.error.DataScopeException;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : DataScopeAOP.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/1.</p>
 * <p>Time         : 14:44.</p>
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DataScopeAOP {
    private final PmsDataScopeService dataScopeService;

    @Pointcut("@annotation(com.ingot.framework.data.mybatis.scope.config.DataScope)")
    public void dataScope() {
    }

    @Around("dataScope()")
    public Object dataScopeMethod(ProceedingJoinPoint point) throws Throwable {
        try {
            // 如果跳过数据权限，直接返回
            if (DataScopeContextHolder.isSkip()) {
                return point.proceed();
            }

            // 如果当前上下文存在scope配置，那么直接使用当前scope
            if (DataScopeContextHolder.isNotEmpty()) {
                return point.proceed();
            }

            InUser user = SecurityAuthContext.getUser();
            if (user == null) {
                throw new DataScopeException(DataScopeErrorCode.DS_401);
            }

            List<String> roleCodes = SecurityAuthContext.getRoles();
            if (CollUtil.isEmpty(roleCodes)) {
                throw new DataScopeException(DataScopeErrorCode.DS_403);
            }

            R<List<SysRole>> result = dataScopeService.getRoleListByCodes(roleCodes);
            if (!result.isSuccess()) {
                throw new DataScopeException(result.getMessage());
            }

            // 填充scope
            List<SysRole> roles = result.getData();
            setScopes(user, roles);
            return point.proceed();
        } finally {
            DataScopeContextHolder.clear();
        }
    }

    private void setScopes(InUser user, List<SysRole> roles) {
        List<Long> scopes = new ArrayList<>();
        for (SysRole role : roles) {
            switch (role.getScopeType()) {
                case ALL:
                    DataScopeContextHolder.skip();
                    return;
                case CUSTOM:
                    if (CollUtil.isNotEmpty(role.getScopes())) {
                        scopes.addAll(role.getScopes());
                    }
                    break;
                case DEPT_AND_CHILD:
                    dataScopeService.getUserSelfAndDescendantDeptList(user.getId())
                            .ifSuccess(deptList -> {
                                if (CollUtil.isNotEmpty(deptList)) {
                                    scopes.addAll(deptList.stream().map(SysDept::getId).distinct().toList());
                                }
                            });
                    break;
                case DEPT:
                    dataScopeService.getUserDeptIds(user.getId())
                            .ifSuccess(deptIds -> {
                                if (CollUtil.isNotEmpty(deptIds)) {
                                    scopes.addAll(deptIds);
                                }
                            });
                    break;
                case SELF:
                    DataScopeContextHolder.setUserScope(user.getId());
                    break;
            }
        }
        DataScopeContextHolder.setScopes(scopes);
    }
}
