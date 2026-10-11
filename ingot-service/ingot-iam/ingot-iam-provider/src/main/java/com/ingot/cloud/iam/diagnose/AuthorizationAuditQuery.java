package com.ingot.cloud.iam.diagnose;

import java.time.Instant;
import java.util.regex.Pattern;

import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>限定授权审计的域内精确筛选和时间半开区间，不接受替代管理域或租户。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param actorMemberId 操作者成员 ID，可空
 * @param targetType 目标资源编码，可空
 * @param targetId 目标 ID，可空
 * @param changeType 变更类型，可空
 * @param from 时间起点，包含，可空
 * @param until 时间终点，不包含，可空
 */
public record AuthorizationAuditQuery(String actorMemberId, String targetType, String targetId,
                                      AuditChangeType changeType, Instant from, Instant until) {
    private static final Pattern MEMBER_ID_PATTERN = Pattern.compile("[1-9][0-9]*");
    private static final int RESOURCE_CODE_LIMIT = 64;
    private static final int TARGET_ID_LIMIT = 128;

    /** 校验精确条件和时间区间；空白条件视为未筛选。 */
    public AuthorizationAuditQuery {
        actorMemberId = text(actorMemberId, TARGET_ID_LIMIT);
        if (actorMemberId != null && !MEMBER_ID_PATTERN.matcher(actorMemberId).matches()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        IamIds.optional(actorMemberId);
        targetType = text(targetType, RESOURCE_CODE_LIMIT);
        targetId = text(targetId, TARGET_ID_LIMIT);
        if (from != null && until != null && !until.isAfter(from)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    /**
     * 从 HTTP 参数恢复类型化筛选，未知变更类型返回 InvalidArgument。
     * @param actorMemberId 操作者成员 ID，可空
     * @param targetType 目标资源编码，可空
     * @param targetId 目标 ID，可空
     * @param changeType 变更类型编码，可空
     * @param from 包含的时间起点，可空
     * @param until 不包含的时间终点，可空
     * @return 已验证的筛选条件
     */
    public static AuthorizationAuditQuery of(String actorMemberId, String targetType, String targetId,
                                             String changeType, Instant from, Instant until) {
        AuditChangeType type = null;
        if (changeType != null && !changeType.isBlank()) {
            try {
                type = AuditChangeType.getEnum(changeType.trim());
            } catch (IllegalArgumentException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
        return new AuthorizationAuditQuery(actorMemberId, targetType, targetId, type, from, until);
    }

    private static String text(String value, int limit) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String result = value.trim();
        if (result.length() > limit) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return result;
    }
}
