package com.ingot.cloud.pms.authorization.engine;

import java.util.Collection;

import cn.hutool.core.util.StrUtil;

/**
 * <p>委派上限判定：拟授予功能必须能证明为授予者自身范围的子集。</p>
 *
 * <p>叶子精确码的全集不能推导出未来通配；通配资格必须自身持有覆盖命名空间的通配。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class GrantCeiling {

    private GrantCeiling() {
    }

    /**
     * 判断授予者能否授予指定功能编码。
     *
     * @param grantor 授予者有效授权
     * @param proposedCode 拟授予编码
     * @return 能证明覆盖时返回 {@code true}
     */
    public static boolean canGrantCode(EffectiveAuthorization grantor, String proposedCode) {
        if (grantor == null || StrUtil.isBlank(proposedCode)) {
            return false;
        }
        if (PermissionMatcher.isWildcard(proposedCode)) {
            for (String granted : grantor.safeWildcardCodes()) {
                if (PermissionMatcher.covers(granted, proposedCode)) {
                    return true;
                }
            }
            return false;
        }
        return grantor.hasPermission(proposedCode);
    }

    /**
     * 判断授予者能否授予一组功能编码。
     *
     * @param grantor 授予者有效授权
     * @param proposedCodes 拟授予编码
     * @return 全部能覆盖时返回 {@code true}
     */
    public static boolean canGrantCodes(EffectiveAuthorization grantor, Collection<String> proposedCodes) {
        if (proposedCodes == null || proposedCodes.isEmpty()) {
            return true;
        }
        for (String proposedCode : proposedCodes) {
            if (!canGrantCode(grantor, proposedCode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 拟授予编码是否因缺少通配资格而被拒绝。
     *
     * @param grantor 授予者有效授权
     * @param proposedCode 拟授予编码
     * @return 拟授予通配且授予者仅有叶子时返回 {@code true}
     */
    public static boolean deniedAsFutureWildcard(EffectiveAuthorization grantor, String proposedCode) {
        return PermissionMatcher.isWildcard(proposedCode) && !canGrantCode(grantor, proposedCode);
    }
}
