package com.ingot.framework.commons.model.iam;

import com.ingot.framework.commons.error.BizException;

/**
 * <p>拼装全局唯一操作码：应用编码、资源编码与末段，用冒号分隔。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ActionCodes {
    /**
     * 操作码段分隔符。
     */
    public static final String SEGMENT_SEPARATOR = ":";

    private ActionCodes() {
    }

    /**
     * 规范为 {@code applicationCode:resourceCode:local}；已带此前缀则先去掉再拼。
     *
     * @param applicationCode 应用编码
     * @param resourceCode 资源编码
     * @param localCode 末段或完整操作码
     * @return 三段式操作码
     * @throws BizException 编码为空、含通配符或末段仍含分隔符
     */
    public static String compose(String applicationCode, String resourceCode, String localCode) {
        if (blank(applicationCode) || blank(resourceCode) || blank(localCode)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        String prefix = applicationCode + SEGMENT_SEPARATOR + resourceCode + SEGMENT_SEPARATOR;
        String local = localCode.trim();
        if (local.startsWith(prefix)) {
            local = local.substring(prefix.length()).trim();
        }
        if (local.isEmpty() || local.indexOf('*') >= 0 || local.contains(SEGMENT_SEPARATOR)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return prefix + local;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
