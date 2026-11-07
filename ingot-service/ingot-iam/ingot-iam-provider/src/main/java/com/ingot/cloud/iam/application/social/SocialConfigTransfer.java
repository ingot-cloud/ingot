package com.ingot.cloud.iam.application.social;

import org.springframework.beans.BeanUtils;
import com.ingot.cloud.iam.infrastructure.persistence.entity.SocialConfigEntity;
import com.ingot.cloud.iam.application.social.model.SocialConfigInput;
import com.ingot.cloud.iam.application.social.model.SocialConfigRecord;

/**
 * <p>在维护用例内转换SocialConfig传输字段，持久化对象不进入HTTP契约。</p>
 * @author jy
 * @since 1.0.0
 */
public final class SocialConfigTransfer {
    private SocialConfigTransfer() { }

    /** 复制维护输入，具体写入规则仍由原业务入口执行。 */
    public static SocialConfigEntity toEntity(SocialConfigInput input) {
        SocialConfigEntity target = new SocialConfigEntity();
        BeanUtils.copyProperties(input, target);
        return target;
    }

    /** 复制查询结果，保留原响应字段与空值。 */
    public static SocialConfigRecord toRecord(SocialConfigEntity entity) {
        SocialConfigRecord target = new SocialConfigRecord();
        BeanUtils.copyProperties(entity, target);
        return target;
    }
}
