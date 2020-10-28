package com.ingot.cloud.pms.api.model.enums;

/**
 * <p>Description  : UcUserTokenStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/26.</p>
 * <p>Time         : 下午2:21.</p>
 */
public enum PmsUserTokenStatusEnum {

    ON_LINE(0, "在线"),
    ON_REFRESH(10, "已刷新"),
    OFF_LINE(20, "离线");

    int status;
    String desc;

    PmsUserTokenStatusEnum(int status, String desc){
        this.status = status;
        this.desc = desc;
    }

    public int status(){
        return status;
    }

    public String desc(){
        return desc;
    }

    public static PmsUserTokenStatusEnum getEnum(int status){
        PmsUserTokenStatusEnum[] arr = PmsUserTokenStatusEnum.values();
        for (PmsUserTokenStatusEnum item: arr){
            if (item.status == status){
                return item;
            }
        }
        return null;
    }

    public static String getDesc(int status){
        PmsUserTokenStatusEnum en = getEnum(status);
        return en != null ? en.desc : null;
    }
}
