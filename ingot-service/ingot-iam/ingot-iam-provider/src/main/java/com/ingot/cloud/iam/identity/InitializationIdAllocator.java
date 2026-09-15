package com.ingot.cloud.iam.identity;

/**
 * <p>为组织初始化事务分配新模型标识，不接受客户端提交的治理或开通 ID。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FunctionalInterface
public interface InitializationIdAllocator {
    /**
     * 返回下一个正数标识。
     *
     * @return 未使用的正数 ID
     */
    long nextId();
}
