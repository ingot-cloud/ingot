package com.ingot.cloud.pms.api.model.enums;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : UcMenuStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/6.</p>
 * <p>Time         : 10:42 AM.</p>
 */
public enum PmsMenuStatusEnum {

    ENABLE("enable", "可用"),
    DISABLE("disable", "禁用");

    String status;
    String desc;

    PmsMenuStatusEnum(String status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public String status() {
        return status;
    }

    public String desc() {
        return desc;
    }

    public static String getDesc(String status){
        PmsMenuStatusEnum en = getEnum(status);
        return en != null ? en.desc : null;
    }

    public static PmsMenuStatusEnum getEnum(String status){
        PmsMenuStatusEnum[] arr = PmsMenuStatusEnum.values();
        for (PmsMenuStatusEnum item: arr){
            if (StrUtil.equals(item.status(), status)){
                return item;
            }
        }

        return null;
    }
}
