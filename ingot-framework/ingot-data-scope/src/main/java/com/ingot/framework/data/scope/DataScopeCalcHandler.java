package com.ingot.framework.data.scope;

/**
 * <p>Description  : DataScopeCalcHandler.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/12.</p>
 * <p>Time         : 16:29.</p>
 */
public interface DataScopeCalcHandler {

    /**
     * 处理DataScope<br>
     * 1. 如果主动填充scope，那么不进行角色判断 <br>
     * 2. 根据角色中的数据权限，填充scope <br>
     * @param scope 未加工的{@link DataScope}
     * @return true：进行数据权限过滤，false：不进行数据权限过滤
     */
    boolean process(DataScope scope);
}
