package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.util.List;

/**
 * <p>将持久任务的发现和单批执行与本地调度策略分离。</p>
 * @author jy
 * @since 1.0.0
 */
interface RoleApplicationWork {
    /** 按任务ID游标发现提示，结果不携带授权或身份。 */
    List<BigInteger> discover(BigInteger after, int limit);

    /** 执行最多一个事务批次，返回是否继续以及领取竞争结果。 */
    BatchResult executeTask(BigInteger taskId);

    /**
     * <p>一个批次的内部调度结果，不改变持久业务状态或公开接口。</p>
     * @param more 是否仍有本节点应继续处理的明细
     * @param leaseConflict 是否在事务内领取竞争失败
     * @author jy
     * @since 1.0.0
     */
    record BatchResult(boolean more, boolean leaseConflict) {
        static final BatchResult STOP = new BatchResult(false, false);
        static final BatchResult MORE = new BatchResult(true, false);
        static final BatchResult CONFLICT = new BatchResult(false, true);
    }
}
