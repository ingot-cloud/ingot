package com.ingot.cloud.pms.api.model.vo.dept;

import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>Description  : DeptPageItemVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/25.</p>
 * <p>Time         : 10:14 下午.</p>
 */
@Data
public class DeptTreeNode implements Serializable {
    /**
     * ID
     */
    private Long id;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 部门角色范围, 0:当前部门，1:当前部门和直接子部门
     */
    private DeptRoleScopeEnum scope;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 删除日期
     */
    private LocalDateTime deletedAt;
}
