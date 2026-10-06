package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.HashSet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * 更新当前域成员资料，禁止携带凭证、状态或其它组织身份。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 成员读取版本
 * @param displayName 显示名，可空表示不修改
 * @param avatar 头像，可空表示不修改；可提交时效链接或对象路径，入库只保存路径
 * @param phone 组织通讯录手机号，可空表示不修改；不可编辑或脱敏占位由服务拒绝
 * @param email 组织通讯录邮箱，可空表示不修改；不可编辑或脱敏占位由服务拒绝
 * @param suppliedFields 实际提交的键，仅服务端校验使用；平台显式 null 同样校验字段权限
 */
@JsonDeserialize(using = MemberProfileInput.Deserializer.class)
@Schema(description = "更新当前域成员资料，禁止携带凭证、状态或其它组织身份")
public record MemberProfileInput(
        @NotBlank @Schema(description = "成员读取版本", requiredMode = Schema.RequiredMode.REQUIRED) String expectedVersion,
        @Schema(description = "显示名，可空表示不修改") String displayName,
        @Schema(description = "头像，可空表示不修改；可提交时效链接或对象路径，入库只保存路径") String avatar,
        @Schema(description = "组织通讯录手机号，可空表示不修改") String phone, @Schema(description = "组织通讯录邮箱，可空表示不修改") String email,
        @com.fasterxml.jackson.annotation.JsonIgnore @Schema(hidden = true) java.util.Set<String> suppliedFields) {

    /** 保留既有 Java 调用的空值不修改语义。 */
    public MemberProfileInput(String expectedVersion, String displayName, String avatar, String phone, String email) {
        this(expectedVersion, displayName, avatar, phone, email, supplied(displayName, avatar, phone, email));
    }

    /**
     * <p>
     * 保留本次请求 Jackson 的头像绑定规则及实际提交键。
     * </p>
     *
     * @author jy
     * @since 1.0.0
     */
    public static final class Deserializer extends JsonDeserializer<MemberProfileInput> {

        /**
         * 绑定资料而不丢弃显式 null 与未知键。
         * @param parser 请求 JSON
         * @param context 反序列化上下文
         * @return 类型化资料
         * @throws IOException JSON 结构不合法
         */
        @Override
        public MemberProfileInput deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            if (!node.isObject())
                throw JsonMappingException.from(parser, "成员资料请求必须为对象");
            var input = parser.getCodec().treeToValue(node, JacksonInput.class);
            java.util.Set<String> keys = new HashSet<>();
            node.fieldNames().forEachRemaining(keys::add);
            return new MemberProfileInput(input.expectedVersion(), input.displayName(), input.avatar(), input.phone(),
                    input.email(), keys);
        }

    }

    /**
     * <p>
     * 供服务器 Jackson mixin 配置的内部绑定形态，不属于 API 请求或响应。
     * </p>
     *
     * @param expectedVersion 成员版本
     * @param displayName 显示名
     * @param avatar 头像
     * @param phone 手机号
     * @param email 邮箱
     * @author jy
     * @since 1.0.0
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(hidden = true)
    public record JacksonInput(String expectedVersion, String displayName, String avatar, String phone, String email) {
    }

    /** 复制提交字段集合。 */
    public MemberProfileInput {
        suppliedFields = java.util.Set.copyOf(suppliedFields);
    }

    private static java.util.Set<String> supplied(String displayName, String avatar, String phone, String email) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        if (displayName != null)
            keys.add("displayName");
        if (avatar != null)
            keys.add("avatar");
        if (phone != null)
            keys.add("phone");
        if (email != null)
            keys.add("email");
        return keys;
    }
}
