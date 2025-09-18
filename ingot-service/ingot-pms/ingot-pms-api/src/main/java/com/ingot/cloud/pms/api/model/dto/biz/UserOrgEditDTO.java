package com.ingot.cloud.pms.api.model.dto.biz;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>Description  : 用户组织相关内容编辑.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/21.</p>
 * <p>Time         : 11:09 AM.</p>
 */
@Data
public class UserOrgEditDTO implements Serializable {
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 组织ID
     */
    private Long orgId;
    /**
     * 部门ID
     */
    private List<Long> deptIds;
    /**
     * 角色ID
     */
    private List<Long> roleIds;
}
