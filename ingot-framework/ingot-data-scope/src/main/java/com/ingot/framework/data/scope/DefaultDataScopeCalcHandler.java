package com.ingot.framework.data.scope;

import cn.hutool.core.collection.CollUtil;
import com.ingot.cloud.pms.api.rpc.PmsDataScopeService;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultDataScopeCalcHandler.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/14.</p>
 * <p>Time         : 15:49.</p>
 */
@RequiredArgsConstructor
public class DefaultDataScopeCalcHandler implements DataScopeCalcHandler {
    private final PmsDataScopeService pmsDataScopeService;

    @Override
    public boolean process(DataScope scope) {

        // 自定义数据权限，直接返回
        if (scope.getUserScope() != null || CollUtil.isNotEmpty(scope.getScopes())) {
            return true;
        }



        return false;
    }
}
