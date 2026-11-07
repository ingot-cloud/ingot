package com.ingot.cloud.iam.application.id.model;

import java.io.Serial;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;

/**
 * <p>承载IdAllocation维护响应，不提供持久化操作。
 * </p>
 * @author jy
 * @since 1.0.0
 */
@Data
public class IdAllocationRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 业务序列标识。 */
    private String bizTag;

    /** 已分配号段的高水位。 */
    private Long maxId;

    /** 每次分配的号段长度。 */
    private Integer step;
    /** 业务说明。 */
    private String description;

    /** 最近维护时间，按既有 UTC 时间契约收发。 */
    private LocalDateTime updateTime;

}
