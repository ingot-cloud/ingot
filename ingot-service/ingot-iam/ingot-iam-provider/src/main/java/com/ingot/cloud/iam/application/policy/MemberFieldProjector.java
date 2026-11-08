package com.ingot.cloud.iam.application.policy;


import java.util.Map;

import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.cloud.iam.application.organization.model.MemberRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * <p>将成员字段访问结果交给统一投影引擎处理，不参与授权范围求值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberFieldProjector {
    private final com.ingot.framework.authorization.field.FieldProjectionEngine projection;


    /** 按绑定注解投影全部受控属性，时间、别名及新增 DTO 不需要成员枚举分支。 */
    public MemberRecord project(MemberRecord raw, Map<String, FieldAccess> access, ResourceKey resource,
            Map<String, com.ingot.framework.commons.model.iam.MaskSpec> masks) {
        return projection.projectRecord(raw, resource,
                new com.ingot.framework.authorization.field.FieldReadSnapshot(Map.of(resource, access), Map.of(resource, masks)));
    }
}
