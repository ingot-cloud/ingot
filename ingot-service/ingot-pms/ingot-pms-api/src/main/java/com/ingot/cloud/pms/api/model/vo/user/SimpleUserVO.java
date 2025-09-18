package com.ingot.cloud.pms.api.model.vo.user;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : SimpleUserVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/6.</p>
 * <p>Time         : 17:02.</p>
 */
@Data
public class SimpleUserVO implements Serializable {
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 姓名
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
}
