package com.ingot.example.ops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ingot.framework.commons.annotation.field.*;

/**
 * <p>同一资源的多 DTO 与 JSON 别名声明，逻辑字段不依赖实际属性名。</p>
 * @author jy
 * @since 1.0.0
 */
public final class IncidentViews {
    private IncidentViews() { }
    /** 列表 DTO。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Row(@PublicField String id, @FieldBinding(key = IncidentProvider.TITLE) String title,
            @FieldBinding(key = IncidentProvider.CONTACT) String contact) { }
    /** 详情属性 aphone 映射到同一 contact 规则。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(@PublicField String id, @FieldBinding(key = IncidentProvider.TITLE) String title,
            @JsonProperty("contactPhone") @FieldBinding(key = IncidentProvider.CONTACT) String aphone) { }
    /** 更新 DTO，用实际 JSON 键收集，null 表示清空。 */
    public record Patch(@FieldBinding(key = IncidentProvider.TITLE, uses = FieldUse.WRITE) String title,
            @JsonProperty("contactPhone") @FieldBinding(key = IncidentProvider.CONTACT, uses = FieldUse.WRITE) String aphone) { }
    /** 查询 DTO。 */
    public record Filter(@FieldBinding(key = IncidentProvider.TITLE, uses = FieldUse.FILTER) String title,
            @PublicField Integer page, @PublicField Integer pageSize) { }
}
