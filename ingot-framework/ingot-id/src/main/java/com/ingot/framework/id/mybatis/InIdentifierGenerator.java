package com.ingot.framework.id.mybatis;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.ingot.framework.id.IdGenerator;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : 自定义{@link IdentifierGenerator}, 通过IdGenerator代理实现.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/29.</p>
 * <p>Time         : 8:14 PM.</p>
 */
@RequiredArgsConstructor
public class InIdentifierGenerator implements IdentifierGenerator {
    private final IdGenerator idGenerator;

    @Override
    public Long nextId(Object entity) {
        return idGenerator.nextId();
    }
}
