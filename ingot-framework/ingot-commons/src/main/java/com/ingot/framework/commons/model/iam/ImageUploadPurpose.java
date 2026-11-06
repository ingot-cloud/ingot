package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>已登记的业务图片暂存用途，编辑、创建、本人及重新加入不能相互消费凭证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ImageUploadPurpose {
    PLATFORM_MEMBER_CREATE("platform-member-avatar-create"),
    PLATFORM_MEMBER_UPDATE("platform-member-avatar-update"),
    TENANT_MEMBER_CREATE("tenant-member-avatar-create"),
    TENANT_MEMBER_REJOIN("tenant-member-avatar-rejoin"),
    TENANT_MEMBER_UPDATE("tenant-member-avatar-update"),
    PLATFORM_TENANT_CREATE("platform-tenant-logo-create"),
    PLATFORM_TENANT_UPDATE("platform-tenant-logo-update"),
    TENANT_SETTINGS_UPDATE("tenant-logo-update"),
    APPLICATION_CREATE("application-icon-create"),
    APPLICATION_UPDATE("application-icon-update"),
    APP_USER_CREATE("app-user-avatar-create"),
    APP_USER_UPDATE("app-user-avatar-update"),
    APP_SELF_UPDATE("app-self-avatar-update");

    /** 服务间、客户端和数据库共同使用的用途字面量。 */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, ImageUploadPurpose> BY_VALUE =
            EnumUtils.index(values(), ImageUploadPurpose::getValue);

    /** 按已登记的业务用途解析，拒绝任意目录或未知用途。 */
    @JsonCreator
    public static ImageUploadPurpose getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
