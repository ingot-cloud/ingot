package com.ingot.cloud.pms.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
 * @since 2020-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_authority")
public class SysAuthority extends BaseModel<SysAuthority> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
    private Long id;

    /**
     * 版本号
     */
    @Version
    private Long version;

    /**
     * 租户ID
     */
    private Integer tenantId;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限编码
     */
    private String code;

    /**
     * URL
     */
    private String path;

    /**
     * 状态, 0:正常，9:禁用
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

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
    @TableLogic
    private LocalDateTime deletedAt;


}
