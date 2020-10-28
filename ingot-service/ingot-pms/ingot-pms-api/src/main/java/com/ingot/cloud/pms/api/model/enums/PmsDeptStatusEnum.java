package com.ingot.cloud.pms.api.model.enums;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : PmsDeptStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/27.</p>
 * <p>Time         : 上午10:59.</p>
 */
public enum PmsDeptStatusEnum {

    ENABLE("enable", "可用"),
    DISABLE("disable", "禁用");

    String status;
    String desc;

    PmsDeptStatusEnum(String status, String desc) {
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
        PmsDeptStatusEnum en = getEnum(status);
        return en != null ? en.desc : null;
    }

    public static PmsDeptStatusEnum getEnum(String status){
        PmsDeptStatusEnum[] arr = PmsDeptStatusEnum.values();
        for (PmsDeptStatusEnum item: arr){
            if (StrUtil.equals(item.status(), status)){
                return item;
            }
        }

        return null;
    }
}
