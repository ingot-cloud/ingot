package com.ingot.cloud.iam.identity;

import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Pattern;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.cloud.iam.persistence.mapper.IamIdentityMapper;
import com.ingot.cloud.iam.persistence.projection.IdentityRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>按账号、当前域及成员归属查询有效身份，显式限制租户并同时检查全局账号状态。</p>
 *
 * <p>只访问 IAM 新模型，不回退旧用户表或默认租户；查询不加载凭证，不合并多个身份。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class IdentityRepository {
    private static final Pattern DECIMAL_ID = Pattern.compile("[1-9][0-9]{0,19}");
    private static final BigInteger MAX_DATABASE_ID = new BigInteger("18446744073709551615");
    private final IamIdentityMapper mapper;

    /**
     * 同次查询确认账号与成员有效性，租户域同时验证租户有效性。
     *
     * @param context 从已认证会话恢复的候选身份，不能由业务请求自由构造
     * @return 有效身份；不存在、归属不符或停用均返回空，不暴露具体存在性
     * @throws BizException 标识不符合数据库 ID 格式时使用 IdentityInvalid
     * @throws org.springframework.dao.DataAccessException 数据源不可用或目标结构不存在时抛出
     */
    public Optional<ActiveIdentity> findActive(AuthorizationContext context) {
        if (context == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        BigInteger accountId = databaseId(context.accountId());
        BigInteger memberId = databaseId(context.memberId());
        IdentityRow row = context.domain() == AuthorizationDomain.TENANT
                ? mapper.tenant(accountId, databaseId(context.tenantId()), memberId, MemberStatus.ACTIVE)
                : mapper.platform(accountId, memberId, MemberStatus.ACTIVE);
        return Optional.ofNullable(row).map(value -> new ActiveIdentity(context, value.getAccountVersion(),
                value.getMemberVersion(), value.getTenantVersion()));
    }

    /**
     * 在凭证认证成功后按账号和显式目标域选择有效成员，不使用角色或默认租户推断身份。
     *
     * @param accountId 已由 Auth 验证凭证的账号 ID，不能取自业务请求中的任意账号字段
     * @param domain 明确选择的平台或租户域
     * @param tenantId 租户域必填，平台域必须为空
     * @return 当前域有效成员；无成员或任一状态无效返回空
     * @throws BizException 域、租户组合或 ID 不合法时使用 IdentityInvalid
     */
    public Optional<ActiveIdentity> selectActive(String accountId, AuthorizationDomain domain, String tenantId) {
        if (domain == null || (domain == AuthorizationDomain.PLATFORM && tenantId != null)
                || (domain == AuthorizationDomain.TENANT && tenantId == null)) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        BigInteger account = databaseId(accountId);
        IdentityRow row = domain == AuthorizationDomain.TENANT
                ? mapper.tenant(account, databaseId(tenantId), null, MemberStatus.ACTIVE)
                : mapper.platform(account, null, MemberStatus.ACTIVE);
        return Optional.ofNullable(row).map(value -> new ActiveIdentity(
                new AuthorizationContext(domain, tenantId, accountId, value.getMemberId()),
                value.getAccountVersion(), value.getMemberVersion(), value.getTenantVersion()));
    }

    private BigInteger databaseId(String value) {
        if (value == null || !DECIMAL_ID.matcher(value).matches()) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        BigInteger id = new BigInteger(value);
        if (id.compareTo(MAX_DATABASE_ID) > 0) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return id;
    }
}
