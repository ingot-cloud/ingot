package com.ingot.cloud.iam.api.model.dto.tenant;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.ingot.cloud.iam.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.oss.common.OssUrl;
import lombok.Data;

/**
 * <p>承载内部租户查询响应，字段兼容既有调用方，不提供持久化行为。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class TenantDetailsDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 租户名称
     */
    private String name;

    /**
     * 租户编号
     */
    private String code;

    /**
     * 组织类型
     */
    private OrgTypeEnum orgType;

    /**
     * 头像
     */
    @OssUrl
    private String avatar;

    /**
     * 计划ID
     */
    private Long planId;

    /**
     * 结束日期
     */
    private LocalDate endAt;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    private LocalDateTime deletedAt;
}
