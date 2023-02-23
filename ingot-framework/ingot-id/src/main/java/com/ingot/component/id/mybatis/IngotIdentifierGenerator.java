package com.ingot.component.id.mybatis;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.ingot.component.id.IdGenerator;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : {@link IdentifierGenerator} Ingot-Id实现.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/29.</p>
 * <p>Time         : 8:14 PM.</p>
 */
@RequiredArgsConstructor
public class IngotIdentifierGenerator implements IdentifierGenerator {
    private final IdGenerator idGenerator;

    @Override
    public Long nextId(Object entity) {
        return idGenerator.nextId();
    }
}
