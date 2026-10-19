package com.ingot.framework.commons.model.iam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ingot.framework.commons.annotation.field.FieldBinding;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.annotation.field.PublicField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * <p>
 * 按手机号把账号加入当前租户；没有账号时先创建全局登录凭证。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 * @param displayName 当前租户显示名
 * @param phone 11 位大陆手机号，作为账号标识
 * @param email 组织通讯录邮箱，可空
 * @param departments 任职部门，可空且最多一个主部门
 * @param suppliedFields HTTP 实际提交字段，仅用于写入校验，不传输
 */
@JsonDeserialize(using = TenantMemberCreateInput.Deserializer.class)
@Schema(description = "按手机号加入当前组织；没有账号时先创建全局账号")
public record TenantMemberCreateInput(
        @FieldBinding(key = MemberFieldKey.VALUE_DISPLAY_NAME, uses = FieldUse.WRITE)
        @NotBlank(message = DISPLAY_NAME_REQUIRED)
        @Schema(description = "当前租户显示名", requiredMode = Schema.RequiredMode.REQUIRED) String displayName,
        @PublicField @NotBlank(message = PHONE_REQUIRED)
        @Pattern(regexp = PHONE_PATTERN, message = PHONE_INVALID)
        @Schema(description = "11 位大陆手机号，作为账号标识", requiredMode = Schema.RequiredMode.REQUIRED) String phone,
        @FieldBinding(key = MemberFieldKey.VALUE_EMAIL, uses = FieldUse.WRITE)
        @Schema(description = "组织通讯录邮箱，可空") String email,
        @PublicField @NotNull @Schema(description = "任职部门，可空且最多一个主部门",
                requiredMode = Schema.RequiredMode.REQUIRED) List<@NotNull @Valid MemberDepartmentBinding> departments,
        @JsonIgnore @Schema(hidden = true) Set<String> suppliedFields) {

    /**
     * 大陆手机号形态，不含国家码。
     */
    public static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
    /** 姓名未填时返回给调用方的说明。 */
    public static final String DISPLAY_NAME_REQUIRED = "请输入姓名";
    /** 手机号未填时返回给调用方的说明。 */
    public static final String PHONE_REQUIRED = "请输入手机号";
    /** 手机号不满足形态时返回给调用方的说明，不暴露正则。 */
    public static final String PHONE_INVALID = "手机号格式不正确";

    private static final Set<String> KNOWN_PROPERTIES = Set.of(MemberFieldKey.VALUE_DISPLAY_NAME,
            MemberFieldKey.VALUE_PHONE, MemberFieldKey.VALUE_EMAIL, "departments");

    /**
     * 保留 Java 创建契约，空邮箱仍表示未提交。
     *
     * @param displayName 显示名
     * @param phone 手机号
     * @param email 邮箱
     * @param departments 部门
     */
    public TenantMemberCreateInput(String displayName, String phone, String email,
            List<MemberDepartmentBinding> departments) {
        this(displayName, phone, email, departments, supplied(email));
    }

    private static Set<String> supplied(String email) {
        Set<String> fields = new HashSet<>();
        fields.add(MemberFieldKey.VALUE_DISPLAY_NAME);
        fields.add(MemberFieldKey.VALUE_PHONE);
        if (email != null) {
            fields.add(MemberFieldKey.VALUE_EMAIL);
        }
        return fields;
    }

    /**
     * 查询实际提交中是否包含未声明字段。
     *
     * @return 有未知键时为 true
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasUnknownFields() {
        return !KNOWN_PROPERTIES.containsAll(suppliedFields);
    }

    /**
     * <p>
     * 按本次请求的 Jackson 配置转换嵌套结构，并保留未知键。
     * </p>
     *
     * @author jy
     * @since 1.0.0
     */
    public static final class Deserializer extends JsonDeserializer<TenantMemberCreateInput> {

        /**
         * 转换创建草稿，不在绑定阶段忽略实际提交的字段键。
         *
         * @param parser 请求 JSON
         * @param context 反序列化上下文
         * @return 类型化草稿
         * @throws IOException JSON 结构不合法
         */
        @Override
        public TenantMemberCreateInput deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            if (!node.isObject()) {
                throw JsonMappingException.from(parser, "成员创建请求必须为对象");
            }
            var input = parser.getCodec().treeToValue(node, JacksonInput.class);
            Set<String> keys = new HashSet<>();
            node.fieldNames().forEachRemaining(keys::add);
            return new TenantMemberCreateInput(input.displayName(), input.phone(), input.email(), input.departments(),
                    keys);
        }
    }

    /**
     * <p>
     * 仅供请求内部绑定；未知键由外层草稿保留。
     * </p>
     *
     * @author jy
     * @since 1.0.0
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(hidden = true)
    public record JacksonInput(String displayName, String phone, String email,
            List<MemberDepartmentBinding> departments) {
    }

    /**
     * 规范化任职与提交字段。
     */
    public TenantMemberCreateInput {
        suppliedFields = Set.copyOf(suppliedFields);
        departments = departments == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(departments));
    }

    /**
     * 部门不能重复，且最多一个主部门。
     *
     * @return 是否满足结构约束
     */
    @JsonIgnore
    @AssertTrue(message = "部门不能重复且最多一个主部门")
    @Schema(hidden = true)
    public boolean isDepartmentShapeValid() {
        return MemberCreateInput.uniqueDepartments(departments);
    }
}
