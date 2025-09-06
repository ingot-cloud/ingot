package com.ingot.framework.commons.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : DateUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/14.</p>
 * <p>Time         : 2:15 PM.</p>
 */
public final class DateUtils {

    /**
     * UTC 时间戳
     */
    public static long utcEpochMilli(){
        return Instant.now().toEpochMilli();
    }

    /**
     * 当前UTC时间
     */
    public static LocalDateTime utc(){
        return now(ZoneId.of(ZoneOffset.UTC.getId()));
    }

    /**
     * 当前默认时区时间
     */
    public static LocalDateTime now(){
        return now(ZoneId.systemDefault());
    }

    /**
     * 获取当前指定时区时间
     */
    public static LocalDateTime now(ZoneId zone){
        return LocalDateTime.now(zone);
    }

    /**
     * UTC时区转本地
     */
    public static String utcToLocal(String utc) throws ParseException {
        return utcToLocal(utc, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * UTC时区转本地
     */
    public static String utcToLocal(String utc, String pattern) throws ParseException {
        return timeToLocal(utc, "UTC", pattern);
    }

    /**
     * 时区转本地
     */
    public static String timeToLocal(String time, String timeZone, String pattern) throws ParseException{
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
    public static Date timeToLocal(Date time) throws ParseException {
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
