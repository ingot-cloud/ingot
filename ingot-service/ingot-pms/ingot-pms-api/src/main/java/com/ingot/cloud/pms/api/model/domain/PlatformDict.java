package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ingot.cloud.pms.api.model.enums.DictScopeEnum;
import com.ingot.cloud.pms.api.model.enums.DictTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 字典表。
 * <ul>
 *     <li>{@code type=TYPE} 表示字典类型节点（即字典分组）。</li>
 *     <li>{@code type=ITEM} 表示字典项节点，作为实际可选枚举值。</li>
 *     <li>{@code scopeType} 表示作用域，支持平台级共享、租户隔离与应用隔离。</li>
 * </ul>
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Getter
@Setter
@ToString
@TableName(value = "platform_dict", autoResultMap = true)
public class PlatformDict extends BaseModel<PlatformDict> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @NotNull(groups = Group.Update.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 编码
     */
    @NotBlank(groups = Group.Create.class)
    @Size(max = 64, groups = {Group.Create.class, Group.Update.class})
    @TableField("`code`")
    private String code;

    /**
     * 名称
     */
    @NotBlank(groups = Group.Create.class)
    @Size(max = 128, groups = {Group.Create.class, Group.Update.class})
    @TableField("`name`")
    private String name;

    /**
     * 字典项值（仅 {@code type=ITEM} 时生效）
     */
    @Size(max = 128, groups = {Group.Create.class, Group.Update.class})
    @TableField("`value`")
    private String value;

    /**
     * 字典项展示文本（仅 {@code type=ITEM} 时生效）
     */
    @Size(max = 128, groups = {Group.Create.class, Group.Update.class})
    @TableField("`label`")
    private String label;

    /**
     * 字典类型，{@link DictTypeEnum#TYPE} / {@link DictTypeEnum#ITEM}
     */
    @NotNull(groups = Group.Create.class)
    @TableField("`type`")
    private DictTypeEnum type;

    /**
     * 作用域，{@link DictScopeEnum}
     */
    private DictScopeEnum scopeType;

    /**
     * 租户ID（{@code scopeType=TENANT} 时必填）
     */
    private Long tenantId;

    /**
     * 应用ID（{@code scopeType=APP} 时必填）
     */
    private Long appId;

    /**
     * 组织类型
     */
    private OrgTypeEnum orgType;

    /**
     * 排序权重，越小越靠前
     */
    private Integer sort;

    /**
     * 是否内置字典，内置字典禁止删除/修改 code 与 value
     */
    private Boolean systemFlag;

    /**
     * 状态, 0:正常，9:禁用
     */
    @TableField("`status`")
    private CommonStatusEnum status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展属性，例如 {@code icon}、{@code color}、{@code i18n} 等业务字段
     */
    @TableField(value = "extra", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extra;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 更新人
     */
    private Long updatedBy;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
