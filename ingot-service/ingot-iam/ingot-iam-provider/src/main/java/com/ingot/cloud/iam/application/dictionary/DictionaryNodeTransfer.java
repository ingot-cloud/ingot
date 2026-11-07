package com.ingot.cloud.iam.application.dictionary;

import org.springframework.beans.BeanUtils;
import com.ingot.cloud.iam.infrastructure.persistence.entity.DictionaryEntity;
import com.ingot.cloud.iam.application.dictionary.model.DictionaryNodeInput;
import com.ingot.cloud.iam.application.dictionary.model.DictionaryNodeRecord;

/**
 * <p>在维护用例内转换DictionaryNode传输字段，持久化对象不进入HTTP契约。</p>
 * @author jy
 * @since 1.0.0
 */
public final class DictionaryNodeTransfer {
    private DictionaryNodeTransfer() { }

    /** 复制维护输入，具体写入规则仍由原业务入口执行。 */
    public static DictionaryEntity toEntity(DictionaryNodeInput input) {
        DictionaryEntity target = new DictionaryEntity();
        BeanUtils.copyProperties(input, target);
        return target;
    }

    /** 复制查询结果，保留原响应字段与空值。 */
    public static DictionaryNodeRecord toRecord(DictionaryEntity entity) {
        DictionaryNodeRecord target = new DictionaryNodeRecord();
        BeanUtils.copyProperties(entity, target);
        return target;
    }
}
