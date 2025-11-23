package com.ingot.cloud.pms.api.model.dto.common;

import com.ingot.framework.commons.model.common.AssignDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : BizBindDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 15:46.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizBindDTO extends AssignDTO<Long, Long> {
    /**
     * 当前绑定ID是否为元数据
     */
    private boolean metaFlag;
}
