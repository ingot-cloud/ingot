package com.ingot.framework.core.model.dto.common;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : RelationDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/27.</p>
 * <p>Time         : 2:53 下午.</p>
 */
@Data
public class RelationDto<ID, BID> implements Serializable {
    @NotNull(message = "{Common.IDNonNull}")
    private ID id;
    private List<BID> removeIds;
    private List<BID> bindIds;
}
