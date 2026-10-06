package com.ingot.framework.commons.model.iam;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.core.JsonParser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>平台成员资料及角色差量的原子编辑请求，租户资料契约保持独立。</p>
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 成员读取版本
 * @param displayName 显示名
 * @param avatar 头像路径或时效链接，复用成员资料的对象绑定
 * @param phone 平台联系手机号
 * @param email 平台联系邮箱
 * @param roleChanges 可选角色差量
 * @param suppliedFields 实际资料键，保留显式 null 和未知键以严格鉴权
 */
@JsonDeserialize(using = PlatformMemberEditInput.Deserializer.class)
public record PlatformMemberEditInput(@NotBlank String expectedVersion, String displayName, String avatar,
        String phone, String email, @Valid MemberRoleChanges roleChanges,
        @JsonIgnore @Schema(hidden = true) Set<String> suppliedFields) {
    private static final String ROLE_CHANGES = "roleChanges";
    /** 保留资料权限的实际提交键。 */
    public PlatformMemberEditInput { suppliedFields = Set.copyOf(suppliedFields); }
    /**
     * 提取与租户无关的资料部分，未知键不得在转换时丢弃。
     * @return 资料校验输入
     */
    @JsonIgnore @Schema(hidden = true)
    public MemberProfileInput profile() {
        return new MemberProfileInput(expectedVersion, displayName, avatar, phone, email, suppliedFields);
    }
    /**
     * <p>复用资料 Jackson 绑定，并单独绑定角色差量。</p>
     * @author jy
     * @since 1.0.0
     */
    public static final class Deserializer extends JsonDeserializer<PlatformMemberEditInput> {
        /**
         * 绑定原子成员草稿。
         * @param parser 请求 JSON
         * @param context Jackson 上下文
         * @return 保留实际提交键的草稿
         * @throws IOException 非法 JSON
         */
        @Override
        public PlatformMemberEditInput deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            if (!node.isObject()) throw JsonMappingException.from(parser, "成员编辑请求必须为对象");
            var profile = parser.getCodec().treeToValue(node, MemberProfileInput.class);
            Set<String> keys = new HashSet<>(profile.suppliedFields());
            keys.remove(ROLE_CHANGES);
            var roles = node.has(ROLE_CHANGES) ? parser.getCodec().treeToValue(node.get(ROLE_CHANGES), MemberRoleChanges.class) : null;
            return new PlatformMemberEditInput(profile.expectedVersion(), profile.displayName(), profile.avatar(),
                    profile.phone(), profile.email(), roles, keys);
        }
    }
}
