package com.ingot.framework.commons.model.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>Description  : 分配实体.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/27.</p>
 * <p>Time         : 2:53 下午.</p>
 */
@Data
public class AssignDTO<ID, BID> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "{Common.IDNonNull}")
    private ID id;
    private List<BID> unassignIds;
    private List<BID> assignIds;
}
