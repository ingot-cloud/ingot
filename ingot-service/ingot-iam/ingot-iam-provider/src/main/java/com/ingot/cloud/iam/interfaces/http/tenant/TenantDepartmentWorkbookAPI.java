package com.ingot.cloud.iam.interfaces.http.tenant;

import java.util.List;

import com.ingot.cloud.iam.application.authorization.read.IamReadOnlyRequest;
import com.ingot.cloud.iam.application.organization.DepartmentWorkbookService;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.application.workbook.model.DepartmentWorkbookExportInput;
import com.ingot.cloud.iam.domain.workbook.MemberWorkbookArtifact;
import com.ingot.cloud.iam.application.workbook.model.MemberWorkbookFile;
import com.ingot.cloud.iam.application.workbook.model.MemberWorkbookHistoryItem;
import com.ingot.cloud.iam.domain.workbook.MemberWorkbookMode;
import com.ingot.cloud.iam.application.workbook.model.MemberWorkbookTask;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>提供当前租户部门表格模板、范围导出与导入任务，不改动部门 JSON CRUD。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户部门表格")
@RequestMapping("/v1/tenant/departments")
@RequiredArgsConstructor
public class TenantDepartmentWorkbookAPI implements RShortcuts {
    private final DepartmentWorkbookService workbooks;

    /**
     * 下载创建部门空白模板。
     *
     * @return 模板文件
     */
    @Operation(summary = "下载部门导入模板")
    @IamReadOnlyRequest
    @GetMapping("/workbook/template")
    public R<MemberWorkbookFile> workbookTemplate() {
        return ok(workbooks.template());
    }

    /**
     * 登记更新导出任务。
     *
     * @param input 组织版本与可选部门范围
     * @return 下载任务 ID
     */
    @Operation(summary = "导出部门表格")
    @PostMapping("/workbook/export")
    public R<CreatedResource> exportWorkbook(@Valid @RequestBody DepartmentWorkbookExportInput input) {
        return ok(workbooks.export(input));
    }

    /**
     * 登记导入任务。
     *
     * @param file xlsx
     * @param mode 创建或更新
     * @return 任务 ID
     */
    @Operation(summary = "导入部门表格")
    @PostMapping("/workbook/import")
    public R<CreatedResource> importWorkbook(@RequestParam("file") MultipartFile file,
            @RequestParam("mode") MemberWorkbookMode mode) {
        return ok(workbooks.importWorkbook(file, mode));
    }

    /**
     * 返回当前操作者最近导入任务。
     *
     * @return 历史摘要
     */
    @Operation(summary = "部门导入历史")
    @IamReadOnlyRequest
    @GetMapping("/workbook/history")
    public R<List<MemberWorkbookHistoryItem>> workbookHistory() {
        return ok(workbooks.history());
    }

    /**
     * 读取表格任务状态。
     *
     * @param id 任务 ID
     * @return 任务状态
     */
    @Operation(summary = "查询部门表格任务")
    @IamReadOnlyRequest
    @GetMapping("/workbook/{id}/status")
    public R<MemberWorkbookTask> workbookStatus(@PathVariable String id) {
        return ok(workbooks.status(id));
    }

    /**
     * 下载表格任务产物。
     *
     * @param id 任务 ID
     * @param artifact 产物类型
     * @return 文件
     */
    @Operation(summary = "下载部门表格产物")
    @IamReadOnlyRequest
    @GetMapping("/workbook/{id}/file")
    public R<MemberWorkbookFile> workbookFile(@PathVariable String id,
            @RequestParam("artifact") MemberWorkbookArtifact artifact) {
        return ok(workbooks.file(id, artifact));
    }
}
