package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserBindDeptDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 2:04 PM.</p>
 */
@Data
public class UserBindDeptDto implements Serializable {
    private String user_id;
    private String dept_id;
}
