package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserBaseInfoDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/25.</p>
 * <p>Time         : 8:58 上午.</p>
 */
@Data
public class UserBaseInfoDto implements Serializable {
    private String phone;
    private String email;
    private String realName;
}
