package com.ingot.framework.commons.model.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>Description  : SetDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/21.</p>
 * <p>Time         : 08:29.</p>
 */
@Data
public class SetDTO<ID, BID> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "{Common.IDNonNull}")
    private ID id;
    private List<BID> setIds;
}
