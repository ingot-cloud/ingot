package com.ingot.cloud.iam.support;

import java.util.Map;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer.Admission;
import com.ingot.framework.commons.model.iam.IamAction;

/**
 * <p>保存一次只读查询内复用的操作展示资格，实际写入仍须独立执行实时准入。</p>
 *
 * @param actions 有权操作及其非委派治理资格，缺省操作表示拒绝
 * @author jy
 * @since 1.0.0
 */
public record IamCapabilities(Map<IamAction, Admission> actions) {
    /**
     * 固定本次查询的结果，不允许列表组装过程修改。
     */
    public IamCapabilities {
        actions = Map.copyOf(actions);
    }

    /**
     * 查询操作展示资格，无权操作返回 false。
     *
     * @param action 精确操作
     * @param direct 是否要求非委派来源的治理资格
     * @return 本次求值是否满足资格
     */
    public boolean allows(IamAction action, boolean direct) {
        Admission admission = actions.get(action);
        return admission != null && (!direct || admission.governed());
    }
}
