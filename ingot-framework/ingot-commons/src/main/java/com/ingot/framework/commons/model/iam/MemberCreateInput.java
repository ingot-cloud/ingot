package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import com.ingot.framework.commons.annotation.field.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * 把已有全局账号关联为当前域成员，不创建或改写登录凭证。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 * @param accountId 已存在的全局账号 ID
 * @param displayName 当前域显示名，可空
 * @param avatar 当前域头像，可空；可提交时效链接或对象路径，入库只保存路径
 * @param departments 租户任职；平台必须为空且最多一个主部门
 * @param roleIds 选填的直接角色定义 ID，默认为空；平台创建时可写入最新已发布版本
 * @param groupIds 选填的平台用户组 ID，默认为空；租户路径拒绝非空
 * @param roleAssignments 选填的固定版本直接分配；平台成员、角色与组在同一事务写入
 * @param suppliedFields HTTP 实际提交字段，仅用于平台写入校验，不传输
 */
@JsonDeserialize(using = MemberCreateInput.Deserializer.class)
@Schema(description = "把已有全局账号关联为当前域成员，不创建或改写登录凭证")
public record MemberCreateInput(
        @PublicField @NotBlank @Schema(description = "已存在的全局账号 ID", requiredMode = Schema.RequiredMode.REQUIRED) String accountId,
        @FieldBinding(key = MemberFieldKey.VALUE_DISPLAY_NAME, uses = FieldUse.WRITE) @Schema(description = "当前域显示名，可空") String displayName,
        @FieldBinding(key = MemberFieldKey.VALUE_AVATAR, uses = FieldUse.WRITE) @Schema(description = "当前域头像，可空；可提交时效链接或对象路径，入库只保存路径") String avatar,
        @PublicField @NotNull @Schema(description = "租户任职；平台必须为空且最多一个主部门",
                requiredMode = Schema.RequiredMode.REQUIRED) List<@NotNull @Valid MemberDepartmentBinding> departments,
        @PublicField @Schema(description = "选填的直接角色定义 ID，默认为空") List<String> roleIds,
        @PublicField @Schema(description = "选填的用户组 ID，默认为空") List<String> groupIds,
        @PublicField @Schema(description = "选填的固定版本直接分配") List<@NotNull @Valid MemberRoleAssignmentDraft> roleAssignments,
        @JsonIgnore @Schema(hidden = true) Set<String> suppliedFields) {

    private static final Set<String> KNOWN_PROPERTIES = Set.of("accountId", MemberFieldKey.VALUE_DISPLAY_NAME,
            MemberFieldKey.VALUE_AVATAR, "departments", "roleIds", "groupIds", "roleAssignments");

    /**
     * 保留既有 Java 创建契约，空资料字段仍表示未提交。
     * @param accountId 全局账号
     * @param displayName 显示名
     * @param avatar 头像
     * @param departments 部门
     * @param roleIds 直接角色
     * @param groupIds 用户组
     * @param roleAssignments 固定版本分配
     */
    public MemberCreateInput(String accountId, String displayName, String avatar,
            List<MemberDepartmentBinding> departments, List<String> roleIds, List<String> groupIds,
            List<MemberRoleAssignmentDraft> roleAssignments) {
        this(accountId, displayName, avatar, departments, roleIds, groupIds, roleAssignments,
                supplied(displayName, avatar));
    }

    private static Set<String> supplied(String displayName, String avatar) {
        Set<String> fields = new HashSet<>();
        if (displayName != null)
            fields.add(MemberFieldKey.VALUE_DISPLAY_NAME);
        if (avatar != null)
            fields.add(MemberFieldKey.VALUE_AVATAR);
        return fields;
    }

    /**
     * 查询实际提交中是否包含未声明字段；租户兼容性由服务管理域决定。
     * @return 有未知键时为 true
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasUnknownFields() {
        return !KNOWN_PROPERTIES.containsAll(suppliedFields);
    }

    /**
     * <p>
     * 按本次请求的 Jackson 配置转换嵌套结构，并保留 null 和未知键。
     * </p>
     *
     * @author jy
     * @since 1.0.0
     */
    public static final class Deserializer extends JsonDeserializer<MemberCreateInput> {

        /**
         * 转换创建草稿，不在绑定阶段忽略实际提交的字段键。
         * @param parser 请求 JSON
         * @param context 反序列化上下文
         * @return 类型化草稿
         * @throws IOException JSON 结构不合法
         */
        @Override
        public MemberCreateInput deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            if (!node.isObject())
                throw JsonMappingException.from(parser, "成员创建请求必须为对象");
            var input = parser.getCodec().treeToValue(node, JacksonInput.class);
            Set<String> keys = new HashSet<>();
            node.fieldNames().forEachRemaining(keys::add);
            return new MemberCreateInput(input.accountId(), input.displayName(), input.avatar(), input.departments(),
                    input.roleIds(), input.groupIds(), input.roleAssignments(), keys);
        }

    }

    /**
     * <p>
     * 仅供请求内部绑定；未知键由外层草稿保留并按管理域校验。
     * </p>
     *
     * @author jy
     * @since 1.0.0
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(hidden = true)
    public record JacksonInput(String accountId, String displayName, String avatar,
            List<MemberDepartmentBinding> departments, List<String> roleIds, List<String> groupIds,
            List<MemberRoleAssignmentDraft> roleAssignments) {
    }

    /**
     * 兼容仅提交任职的创建请求。
     * @param accountId 已存在的全局账号 ID
     * @param displayName 当前域显示名，可空
     * @param avatar 当前域头像，可空
     * @param departments 租户任职
     */
    public MemberCreateInput(String accountId, String displayName, String avatar,
            List<MemberDepartmentBinding> departments) {
        this(accountId, displayName, avatar, departments, List.of(), List.of(), List.of());
    }

    /**
     * 保持旧客户端的角色定义 ID 和用户组创建构造契约。
     * @param accountId 全局账号
     * @param displayName 当前域显示名
     * @param avatar 头像
     * @param departments 租户部门
     * @param roleIds 简单角色
     * @param groupIds 平台用户组
     */
    public MemberCreateInput(String accountId, String displayName, String avatar,
            List<MemberDepartmentBinding> departments, List<String> roleIds, List<String> groupIds) {
        this(accountId, displayName, avatar, departments, roleIds, groupIds, List.of());
    }

    /**
     * 规范化任职与选填集合。
     */
    public MemberCreateInput {
        suppliedFields = Set.copyOf(suppliedFields);
        if (departments != null) {
            departments = Collections.unmodifiableList(new ArrayList<>(departments));
        }
        roleIds = copyIds(roleIds);
        groupIds = copyIds(groupIds);
        roleAssignments = roleAssignments == null ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(roleAssignments));
    }

    /**
     * 部门不能重复，且最多一个主部门。
     * @return 是否满足结构约束，跨域合法性由服务按路径校验
     */
    @JsonIgnore
    @AssertTrue(message = "部门不能重复且最多一个主部门")
    @Schema(hidden = true)
    public boolean isDepartmentShapeValid() {
        return uniqueDepartments(departments);
    }

    static boolean uniqueDepartments(List<MemberDepartmentBinding> departments) {
        if (departments == null || departments.contains(null)) {
            return true;
        }
        var ids = new HashSet<String>();
        int primary = 0;
        for (MemberDepartmentBinding department : departments) {
            if (!ids.add(department.id()) || (department.primary() && ++primary > 1)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> copyIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(ids));
    }
}
