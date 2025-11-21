package com.ingot.cloud.pms.api.model.bo.common;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>Description  : BizSetBO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/20.</p>
 * <p>Time         : 16:03.</p>
 */
@Data
public class BizSetBO<ID, BID> implements Serializable {
    private ID id;
    private List<BID> assignIds;
    private List<BID> unassignIds;
}
