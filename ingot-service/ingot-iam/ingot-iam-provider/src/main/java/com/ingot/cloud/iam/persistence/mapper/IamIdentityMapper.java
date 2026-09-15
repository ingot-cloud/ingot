package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.projection.IdentityRow;
import com.ingot.framework.commons.model.iam.MemberStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>通过固定联表 SQL 原子验证独立身份，所有过滤值均使用绑定参数。</p>
 * @author jy
 * @since 1.0.0
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamIdentityMapper {
    /** 验证平台身份；memberId 为空时只按已认证账号选择平台成员，不接受租户条件。 */
    IdentityRow platform(@Param("accountId") BigInteger accountId, @Param("memberId") BigInteger memberId,
                         @Param("active") MemberStatus active);
    /** 验证当前租户身份；tenantId 必填，memberId 为空用于已认证账号的身份选择。 */
    IdentityRow tenant(@Param("accountId") BigInteger accountId, @Param("tenantId") BigInteger tenantId,
                       @Param("memberId") BigInteger memberId, @Param("active") MemberStatus active);
}
