package com.ingot.cloud.pms.api.model.dto.common;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>Description  : IdsDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/26.</p>
 * <p>Time         : 08:54.</p>
 */
@Data
public class IdsDTO implements Serializable {
    private List<Long> ids;
}
