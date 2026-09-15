package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射菜单操作关联的持久化字段。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_menu_action", autoResultMap = true)
public class IamMenuActionEntity {
    /** 所属应用 ID。 */
    @TableField("application_id")
    private BigInteger applicationId;

    /** 菜单 ID。 */
    @TableField("menu_id")
    private BigInteger menuId;

    /** 操作 ID。 */
    @TableField("action_id")
    private BigInteger actionId;

}
