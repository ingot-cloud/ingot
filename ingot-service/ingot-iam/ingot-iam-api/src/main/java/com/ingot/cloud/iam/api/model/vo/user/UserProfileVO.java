package com.ingot.cloud.iam.api.model.vo.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.ingot.cloud.iam.api.model.types.UserTenantType;
import com.ingot.framework.oss.common.OssUrl;
import lombok.Data;

/**
 * <p>Description  : 用户简介.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/6/24.</p>
 * <p>Time         : 10:59 上午.</p>
 */
@Data
public class UserProfileVO implements Serializable {
    /**
     * 所在组织
     */
    private List<? extends UserTenantType> orgList;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮件地址
     */
    private String email;

    /**
     * 头像
     */
    @OssUrl
    private String avatar;

    /**
     * 是否可用
     */
    private Boolean enabled;

    /**
     * 是否锁定
     */
    private Boolean locked;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;
}
