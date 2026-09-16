package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存平台组成员关系。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_platform_group_member", autoResultMap = true)
public class IamPlatformGroupMemberEntity {
    /** 平台组 ID。 */
    @TableField("group_id")
    private BigInteger groupId;

    /** 平台成员 ID。 */
    @TableField("member_id")
    private BigInteger memberId;

}
