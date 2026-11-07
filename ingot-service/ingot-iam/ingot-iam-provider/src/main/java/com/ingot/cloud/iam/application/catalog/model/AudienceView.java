package com.ingot.cloud.iam.application.catalog.model;

import com.ingot.cloud.iam.domain.catalog.AudienceKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回当前应用可用人群的展示快照。写入仍使用 {@link AudienceDraft}，本类型不参与保存。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param kind 人群种类
 * @param members 已绑定成员；ALL 为空列表
 * @param departments 已绑定部门；ALL 为空列表
 * @param groups 已绑定用户组；ALL 为空列表
 */
@Schema(description = "当前应用可用人群的展示快照，不参与保存")
public record AudienceView(
        @NotNull @Schema(description = "人群种类", requiredMode = Schema.RequiredMode.REQUIRED)
        AudienceKind kind,
        @NotNull @Schema(description = "已绑定成员；ALL 为空列表", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid AudienceMemberView> members,
        @NotNull @Schema(description = "已绑定部门；ALL 为空列表", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid AudienceDepartmentView> departments,
        @NotNull @Schema(description = "已绑定用户组；ALL 为空列表", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid AudienceGroupView> groups) {

    /**
     * 绑定目标缺失或名称为空时的展示名，不回填更宽的候选列表。
     */
    public static final String RESTRICTED_NAME = "信息受限";

    /**
     * 复制三类绑定列表，避免调用方改动已返回的快照。
     */
    public AudienceView {
        members = members == null ? null : Collections.unmodifiableList(new ArrayList<>(members));
        departments = departments == null ? null : Collections.unmodifiableList(new ArrayList<>(departments));
        groups = groups == null ? null : Collections.unmodifiableList(new ArrayList<>(groups));
    }
}
