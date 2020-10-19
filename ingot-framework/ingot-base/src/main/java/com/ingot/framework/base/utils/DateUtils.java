package com.ingot.framework.base.utils;

import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * <p>Description  : DateUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/14.</p>
 * <p>Time         : 2:15 PM.</p>
 */
@UtilityClass
public class DateUtils {

    /**
     * 当前UTC时间
     */
    public Date utc(){
        return now(DateTimeZone.UTC);
    }

    /**
     * 当前默认时区时间
     */
    public Date now(){
        return now(DateTimeZone.getDefault());
    }

    /**
     * 获取当前指定时区时间
     */
    public Date now(DateTimeZone zone){
        return DateTime.now(zone).toLocalDateTime().toDate();
    }

    /**
     * UTC时区转本地
     */
    public String utcToLocal(String utc) throws ParseException {
        return utcToLocal(utc, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * UTC时区转本地
     */
    public String utcToLocal(String utc, String pattern) throws ParseException {
        return timeToLocal(utc, "UTC", pattern);
    }

    /**
     * 时区转本地
     */
    public String timeToLocal(String time, String timeZone, String pattern) throws ParseException{
        if (StrUtil.isEmpty(time)){
            return time;
        }
        SimpleDateFormat timeFormat = new SimpleDateFormat(pattern);
        timeFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        Date timeDate = timeFormat.parse(time);

        SimpleDateFormat localFormat = new SimpleDateFormat(pattern);
        localFormat.setTimeZone(TimeZone.getDefault());
        return localFormat.format(timeDate);
    }

    /**
     * 时区转本地
     */
    public Date timeToLocal(Date time) throws ParseException {
        return timeToLocal(time, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 时区转本地
     */
    public static Date timeToLocal(Date time, String pattern) throws ParseException {
        if (time == null){
            return null;
        }
        SimpleDateFormat localFormat = new SimpleDateFormat(pattern);
        localFormat.setTimeZone(TimeZone.getDefault());
        return localFormat.parse(localFormat.format(time));
    }
}
