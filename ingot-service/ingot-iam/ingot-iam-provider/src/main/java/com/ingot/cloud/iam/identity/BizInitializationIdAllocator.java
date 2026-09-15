package com.ingot.cloud.iam.identity;

import lombok.RequiredArgsConstructor;

import com.ingot.framework.id.BizGenerator;
import org.springframework.stereotype.Component;

/**
 * <p>使用业务发号器为组织初始化分配正数 ID，不接受客户端提交的标识。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class BizInitializationIdAllocator implements InitializationIdAllocator {
    /**
     * 发号业务键，供 leaf 分配器区分 IAM 初始化序列。
     */
    public static final String BIZ_TAG = "iam";
    private final BizGenerator generator;



    /**
     * {@inheritDoc}
     */
    @Override
    public long nextId() {
        long id = generator.getId(BIZ_TAG);
        if (id <= 0) {
            throw new IllegalStateException("IAM 发号结果必须为正数");
        }
        return id;
    }
}
