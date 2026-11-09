package com.ingot.cloud.iam.infrastructure.persistence.projection;

import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>会话菜单及同一次查询取得的一条可选操作绑定。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
public class SessionMenuRow extends IamMenuEntity {
    /** 已绑定的操作编码；未绑定时为空。 */
    private String requiredActionCode;
}
