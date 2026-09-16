package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存角色版本的范围参数定义。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_role_parameter", autoResultMap = true)
public class IamRoleParameterEntity {
    /** 角色版本 ID。 */
    @TableField("revision_id")
    private BigInteger revisionId;

    /** 参数键。 */
    @TableField("parameter_key")
    private String parameterKey;

    /** 绑定种类。 */
    @TableField("binding_kind")
    private ScopeBindingKind bindingKind;

}
