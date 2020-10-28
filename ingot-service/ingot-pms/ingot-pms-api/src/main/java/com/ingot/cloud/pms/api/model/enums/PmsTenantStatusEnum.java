package com.ingot.cloud.pms.api.model.enums;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : PmsTenantStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 3:28 PM.</p>
 */
public enum PmsTenantStatusEnum {

    ENABLE("enable", "可用"),
    DISABLE("disable", "禁用");

    String status;
    String desc;

    PmsTenantStatusEnum(String status, String desc) {
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
        PmsTenantStatusEnum en = getEnum(status);
        return en != null ? en.desc : null;
    }

    public static PmsTenantStatusEnum getEnum(String status){
        PmsTenantStatusEnum[] arr = PmsTenantStatusEnum.values();
        for (PmsTenantStatusEnum item: arr){
            if (StrUtil.equals(item.status, status)){
                return item;
            }
        }

        return null;
    }
}
