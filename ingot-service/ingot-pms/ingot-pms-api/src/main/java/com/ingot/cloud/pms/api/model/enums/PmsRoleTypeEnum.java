package com.ingot.cloud.pms.api.model.enums;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : UcRoleTypeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/1/14.</p>
 * <p>Time         : 4:40 PM.</p>
 */
public enum PmsRoleTypeEnum {
    FRAMEWORK("Framework", "框架角色"),
    API("Api", "Api角色"),
    SERVICE("Service", "服务角色"),
    APPLICATION("Application", "应用角色"),
    STANDARD("Standard", "标准角色"),
    BUSINESS("Business", "业务角色");

    String type;
    String desc;

    PmsRoleTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String type() {
        return type;
    }

    public String desc() {
        return desc;
    }

    public static String getDesc(String type){
        PmsRoleTypeEnum en = getEnum(type);
        return en != null ? en.desc : null;
    }

    public static PmsRoleTypeEnum getEnum(String type){
        PmsRoleTypeEnum[] arr = PmsRoleTypeEnum.values();
        for (PmsRoleTypeEnum item: arr){
            if (StrUtil.equals(item.type, type)){
                return item;
            }
        }

        return null;
    }
}
