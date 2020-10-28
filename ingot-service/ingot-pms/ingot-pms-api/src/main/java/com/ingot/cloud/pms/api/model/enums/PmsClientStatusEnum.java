package com.ingot.cloud.pms.api.model.enums;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : AcClientStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/1/10.</p>
 * <p>Time         : 10:38 AM.</p>
 */
public enum PmsClientStatusEnum {

    ENABLE("enable", "正常"),
    DISABLE("disable", "禁用"),
    DELETED("deleted", "已删除");

    String status;
    String desc;

    PmsClientStatusEnum(String status, String desc){
        this.status = status;
        this.desc = desc;
    }

    public String status(){
        return status;
    }

    public String desc(){
        return desc;
    }

    public static String getDesc(String status){
        PmsClientStatusEnum en = getEnum(status);
        return en != null ? en.desc : null;
    }

    public static PmsClientStatusEnum getEnum(String status){
        PmsClientStatusEnum[] arr = PmsClientStatusEnum.values();
        for (PmsClientStatusEnum item: arr){
            if (StrUtil.equals(item.status, status)){
                return item;
            }
        }

        return null;
    }
}
