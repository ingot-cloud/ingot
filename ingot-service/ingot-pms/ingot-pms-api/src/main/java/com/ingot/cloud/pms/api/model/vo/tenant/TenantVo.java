package com.ingot.cloud.pms.api.model.vo.tenant;

import com.ingot.framework.base.model.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : TenantVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 3:29 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TenantVo extends BaseVo {
    /**
     * 租户名称
     */
    private String name;

    /**
     * 租户编号
     */
    private String code;

    /**
     * 状态
     */
    private String status;

    /**
     * 是否已删除
     */
    private Boolean is_deleted;
}
