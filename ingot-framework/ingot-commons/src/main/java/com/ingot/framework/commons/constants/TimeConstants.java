package com.ingot.framework.commons.constants;

import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * <p>区分时间点使用的 UTC 与业务日历使用的缺省时区。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class TimeConstants {
    /** UTC 时区 ID，用于数据库连接及 API mapper 配置。 */
    public static final String UTC_ZONE_ID = "UTC";
    /** 时间点计算和持久化使用的时区。 */
    public static final ZoneOffset UTC = ZoneOffset.UTC;
    /** 缺省业务日历时区 ID，不用于解释无时区的 API 时间点。 */
    public static final String SHANGHAI_ZONE_ID = "Asia/Shanghai";
    /** 缺省业务日历时区，用于 Cron 调度。 */
    public static final ZoneId DEFAULT_BUSINESS_ZONE = ZoneId.of(SHANGHAI_ZONE_ID);

    private TimeConstants() {
    }
}
