package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : HandleAdminLoginUserInfoDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/24.</p>
 * <p>Time         : 3:48 PM.</p>
 */
@Data
public class HandleLoginUserInfoDto implements Serializable {
    /**
     * 当前登录的系统id
     */
    private String loginClientId;
    private String userId;
    private String username;
    private String password;
    private String status;
    private String group_id;
    private String group_name;
    private List<String> authorityList;
}
