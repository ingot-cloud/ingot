package com.ingot.cloud.pms.api.model.dto.token;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : OfflineDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/24.</p>
 * <p>Time         : 下午3:23.</p>
 */
@Data
public class OfflineUserDto implements Serializable {
    // 需要离线的用户
    private String user_id;
    private String username;
}
