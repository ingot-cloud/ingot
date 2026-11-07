package com.ingot.cloud.iam.application.id;

import org.springframework.beans.BeanUtils;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IdAllocationEntity;
import com.ingot.cloud.iam.application.id.model.IdAllocationInput;
import com.ingot.cloud.iam.application.id.model.IdAllocationRecord;

/**
 * <p>在维护用例内转换IdAllocation传输字段，持久化对象不进入HTTP契约。</p>
 * @author jy
 * @since 1.0.0
 */
public final class IdAllocationTransfer {
    private IdAllocationTransfer() { }

    /** 复制维护输入，具体写入规则仍由原业务入口执行。 */
    public static IdAllocationEntity toEntity(IdAllocationInput input) {
        IdAllocationEntity target = new IdAllocationEntity();
        BeanUtils.copyProperties(input, target);
        return target;
    }

    /** 复制查询结果，保留原响应字段与空值。 */
    public static IdAllocationRecord toRecord(IdAllocationEntity entity) {
        IdAllocationRecord target = new IdAllocationRecord();
        BeanUtils.copyProperties(entity, target);
        return target;
    }
}
