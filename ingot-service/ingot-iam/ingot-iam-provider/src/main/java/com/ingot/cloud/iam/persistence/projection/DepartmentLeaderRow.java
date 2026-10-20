package com.ingot.cloud.iam.persistence.projection;

import java.math.BigInteger;
import lombok.Getter;
import lombok.Setter;
import com.ingot.framework.commons.model.iam.MemberStatus;

/**
 * <p>负责人关联查询内部投影；显示名必须经字段策略处理后输出。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
public class DepartmentLeaderRow {
    /** 负责人所在部门，汇总查询使用。 */
    private BigInteger departmentId;
    /** 当前可见关系数，不是未过滤总数。 */
    private long visibleCount;
    /** 组织内成员 ID。 */
    private BigInteger memberId;
    /** 原始显示名，仅供服务端字段投影。 */
    private String displayName;
    /** 库存头像路径，仅供字段策略处理后签发，不向客户端直接披露。 */
    private String avatar;
    /** 原始成员状态。 */
    private MemberStatus status;
    /** 关系是否指定主负责人。 */
    private boolean primaryFlag;
    /** 成员有效且账号启用、未删除。 */
    private boolean available;
}
