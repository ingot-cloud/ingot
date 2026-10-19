package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>部门关联分页及版本；total仅统计当前成员读取范围内的记录。</p>
 *
 * @param items 当前页内可见的关联或有效候选
 * @param total 按相同成员读取范围统计的总数
 * @param page 从 1 开始的页号
 * @param pageSize 每页条数
 * @param departmentVersion 当前部门版本，用于差量写入并发校验
 * @param restricted 是否存在不可见的负责人关系
 * @param primaryLeader 当前可见主负责人，不可见或未设置时省略
 * @author jy
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "部门关联分页及版本；total仅统计当前成员读取范围内的记录。")
public record DepartmentLeaderPage(
        @NotNull List<DepartmentLeaderRecord> items,
        long total,
        int page,
        int pageSize,
        @NotBlank String departmentVersion,
        boolean restricted,
        DepartmentLeaderRecord primaryLeader) {
}
