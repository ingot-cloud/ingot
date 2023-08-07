package com.ingot.framework.core.model.common;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * <p>Description  : RelationDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/27.</p>
 * <p>Time         : 2:53 下午.</p>
 */
@Data
public class RelationDTO<ID, BID> implements Serializable {
    @NotNull(message = "{Common.IDNonNull}")
    private ID id;
    private List<BID> removeIds;
    private List<BID> bindIds;
}
