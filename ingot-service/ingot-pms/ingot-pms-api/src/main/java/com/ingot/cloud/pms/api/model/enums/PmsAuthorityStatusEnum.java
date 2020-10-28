package com.ingot.cloud.pms.api.model.enums;


import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : UcAuthorityStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/6.</p>
 * <p>Time         : 9:06 AM.</p>
 */
public enum PmsAuthorityStatusEnum {

    ENABLE("enable", "可用"),
    DISABLE("disable", "禁用");

    String status;
    String desc;

    PmsAuthorityStatusEnum(String status, String desc) {
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
        PmsAuthorityStatusEnum en = getEnum(status);
        return en != null ? en.desc : null;
    }

    public static PmsAuthorityStatusEnum getEnum(String status){
        PmsAuthorityStatusEnum[] arr = PmsAuthorityStatusEnum.values();
        for (PmsAuthorityStatusEnum item: arr){
            if (StrUtil.equals(item.status(), status)){
                return item;
            }
        }

        return null;
    }
}
