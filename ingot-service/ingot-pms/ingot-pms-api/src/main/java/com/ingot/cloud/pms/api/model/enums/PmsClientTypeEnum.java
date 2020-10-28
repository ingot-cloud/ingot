package com.ingot.cloud.pms.api.model.enums;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : AcClientType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/19.</p>
 * <p>Time         : 5:44 PM.</p>
 */
public enum PmsClientTypeEnum {

    SERVICE("Service", "服务"),
    SYSTEM("System", "后管系统"),
    MOBILE_APP("MobileApp", "移动应用"),
    WEB_APP("WebApp", "Web应用"),
    THIRD_SERVICE("ThirdService", "第三方服务");

    String type;
    String desc;

    PmsClientTypeEnum(String type, String desc){
        this.type = type;
        this.desc = desc;
    }

    public String type(){
        return type;
    }

    public String desc(){
        return desc;
    }

    public static String getDesc(String status){
        PmsClientTypeEnum en = getEnum(status);
        return en != null ? en.desc : null;
    }

    public static PmsClientTypeEnum getEnum(String type){
        PmsClientTypeEnum[] arr = PmsClientTypeEnum.values();
        for (PmsClientTypeEnum item: arr){
            if (StrUtil.equals(item.type, type)){
                return item;
            }
        }

        return null;
    }
}
