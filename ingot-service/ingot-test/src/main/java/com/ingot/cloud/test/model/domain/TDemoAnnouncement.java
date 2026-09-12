package com.ingot.cloud.test.model.domain;

import java.io.Serial;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.test.authorization.DemoDataScopeConstants;
import com.ingot.framework.data.mybatis.common.annotation.DataScopeTable;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>示例公告，数据范围与订单相互隔离。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@TenantTable
@DataScopeTable(resource = DemoDataScopeConstants.RESOURCE_ANNOUNCEMENT, scopeColumn = "dept_id", userColumn = "owner_user_id")
@TableName("t_demo_announcement")
public class TDemoAnnouncement extends BaseModel<TDemoAnnouncement> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 标题。
     */
    @TableField("`title`")
    private String title;

    /**
     * 所属部门。
     */
    private Long deptId;

    /**
     * 归属用户。
     */
    private Long ownerUserId;
}
