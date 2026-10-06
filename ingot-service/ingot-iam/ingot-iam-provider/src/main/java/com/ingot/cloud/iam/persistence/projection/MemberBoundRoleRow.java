package com.ingot.cloud.iam.persistence.projection;

import java.math.BigInteger;
import com.ingot.framework.commons.model.iam.RoleKind;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>成员有效角色关系的分页聚合投影，不带组名称或标识。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter @Setter
public class MemberBoundRoleRow {
    /** 角色 ID。 */ private BigInteger roleId;
    /** 名称。 */ private String name;
    /** 固定版本 ID。 */ private BigInteger revisionId;
    /** 版本种类。 */ private RoleKind kind;
    /** 版本号。 */ private BigInteger revisionNumber;
    /** 直接分配数量。 */ private long directCount;
    /** 组继承数量。 */ private long groupCount;
}
