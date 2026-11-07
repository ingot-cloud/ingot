package com.ingot.cloud.iam.domain.catalog;

import com.ingot.framework.commons.model.iam.IamReasonCode;

import java.util.regex.Pattern;

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

    /** 目录编码段须采用小写 kebab-case。 */
    public static final String SEGMENT_PATTERN = "[a-z0-9]+(?:-[a-z0-9]+)*";
    private static final Pattern SEGMENT = Pattern.compile(SEGMENT_PATTERN);

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
        if (!validSegment(applicationCode) || !validSegment(resourceCode) || localCode == null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        String prefix = applicationCode + SEGMENT_SEPARATOR + resourceCode + SEGMENT_SEPARATOR;
        String local = localCode.trim();
        if (local.startsWith(prefix)) {
            local = local.substring(prefix.length()).trim();
        }
        if (!validSegment(local)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return prefix + local;
    }

    private static boolean validSegment(String value) {
        return value != null && SEGMENT.matcher(value).matches();
    }
}
