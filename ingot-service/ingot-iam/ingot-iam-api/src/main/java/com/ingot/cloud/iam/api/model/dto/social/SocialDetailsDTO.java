package com.ingot.cloud.iam.api.model.dto.social;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.Data;

/**
 * <p>承载仅供内部服务读取的社交渠道配置，不向浏览器复用其密钥字段。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class SocialDetailsDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * App ID
     */
    private String appId;

    /**
     * App Secret
     */
    private String appSecret;

    /**
     * 重定向地址
     */
    private String redirectUrl;

    /**
     * 社交名称
     */
    private String name;

    /**
     * 类型
     */
    private SocialTypeEnum type;

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
