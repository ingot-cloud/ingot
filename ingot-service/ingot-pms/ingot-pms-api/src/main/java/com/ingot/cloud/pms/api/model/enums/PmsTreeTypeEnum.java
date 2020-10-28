package com.ingot.cloud.pms.api.model.enums;

/**
 * <p>Description  : UcTreeTypeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/26.</p>
 * <p>Time         : 1:34 PM.</p>
 */
public enum PmsTreeTypeEnum {

    LEAF(1, "叶子"),
    NODE(0, "节点");

    int type;
    String desc;

    PmsTreeTypeEnum(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public int type() {
        return type;
    }

    public String desc() {
        return desc;
    }

    public static String getDesc(int type){
        PmsTreeTypeEnum en = getEnum(type);
        return en != null ? en.desc : null;
    }

    public static PmsTreeTypeEnum getEnum(int type){
        PmsTreeTypeEnum[] arr = PmsTreeTypeEnum.values();
        for (PmsTreeTypeEnum item: arr){
            if (item.type == type){
                return item;
            }
        }

        return null;
    }
}
