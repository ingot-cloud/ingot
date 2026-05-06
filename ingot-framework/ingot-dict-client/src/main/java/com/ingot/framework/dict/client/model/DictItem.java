package com.ingot.framework.dict.client.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * 字典项稳定模型，业务代码与 {@code com.ingot.framework.dict.client.DictService} 的输出契约。
 * 该模型与 {@code PlatformDict} 解耦，避免业务直接依赖 PMS 实体或 VO。
 *
 * @author jy
 * @since 2026/4/25
 */
@Data
@Builder
public class DictItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典节点 ID
     */
    private Long id;

    /**
     * 父字典 ID
     */
    private Long pid;

    /**
     * 字典编码（同一作用域、父节点下唯一）
     */
    private String code;

    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典项值（仅字典项有效，作为业务存储值）
     */
    private String value;

    /**
     * 字典项展示文本（前端选择器主显字段）
     */
    private String label;

    /**
     * 节点类型（true 表示字典项；false 表示字典类型分组）
     */
    private boolean item;

    /**
     * 作用域
     */
    private DictScope scope;

    /**
     * 排序权重，越小越靠前
     */
    private Integer sort;

    /**
     * 是否可用
     */
    private boolean enabled;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展属性
     */
    private Map<String, Object> extra;
}
