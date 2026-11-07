package com.ingot.cloud.iam.application.dictionary.model;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Map;
import com.ingot.cloud.iam.api.model.enums.DictScopeEnum;
import com.ingot.cloud.iam.api.model.enums.DictTypeEnum;
import com.ingot.cloud.iam.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;

/**
 * <p>承载DictionaryNode维护输入，沿用既有字段及校验语义。
 * </p>
 * @author jy
 * @since 1.0.0
 */
@Data
public class DictionaryNodeInput implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @NotNull(groups = Group.Update.class)
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
    private String code;

    /**
     * 名称
     */
    @NotBlank(groups = Group.Create.class)
    @Size(max = 128, groups = {Group.Create.class, Group.Update.class})
    private String name;

    /**
     * 字典项值（仅 {@code type=ITEM} 时生效）
     */
    @Size(max = 128, groups = {Group.Create.class, Group.Update.class})
    private String value;

    /**
     * 字典项展示文本（仅 {@code type=ITEM} 时生效）
     */
    @Size(max = 128, groups = {Group.Create.class, Group.Update.class})
    private String label;

    /**
     * 字典类型，{@link DictTypeEnum#TYPE} / {@link DictTypeEnum#ITEM}
     */
    @NotNull(groups = Group.Create.class)
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
    private CommonStatusEnum status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展属性，例如 {@code icon}、{@code color}、{@code i18n} 等业务字段
     */
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
    private LocalDateTime deletedAt;

}
