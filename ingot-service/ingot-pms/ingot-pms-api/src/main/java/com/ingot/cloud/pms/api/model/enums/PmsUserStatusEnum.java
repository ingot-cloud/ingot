package com.ingot.cloud.pms.api.model.enums;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : UcUserStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/26.</p>
 * <p>Time         : 下午2:17.</p>
 */
public enum PmsUserStatusEnum {

    ENABLE("enable", "可用"),
    DISABLE("disable", "禁用"),
    DELETED("deleted", "已删除"); // deprecate

    String status;
    String desc;

    PmsUserStatusEnum(String status, String desc) {
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
        PmsUserStatusEnum en = getEnum(status);
        return en != null ? en.desc : null;
    }

    public static PmsUserStatusEnum getEnum(String status){
        if (StrUtil.isEmpty(status)){
            return null;
        }
        PmsUserStatusEnum[] arr = PmsUserStatusEnum.values();
        for (PmsUserStatusEnum item: arr){
            if (StrUtil.equals(item.status, status)){
                return item;
            }
        }

        return null;
    }
}
