package com.ingot.cloud.pms.model.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.store.mybatis.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author magician
 * @since 2020-11-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseModel<SysTenant> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 版本号
     */
    private Long version;

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
