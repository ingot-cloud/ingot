package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.download.TenantDownloadService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.download.DownloadLink;
import com.ingot.framework.commons.model.download.DownloadTask;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>租户域「我的下载」：列出当前成员的导出任务并签发下载链接，不需要独立操作权限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 我的下载")
@RequestMapping("/v1/tenant/downloads")
@RequiredArgsConstructor
public class TenantDownloadAPI implements RShortcuts {
    private final TenantDownloadService downloads;

    /**
     * 分页列出当前成员的下载任务，新的在前。
     *
     * @param page 从 1 开始的页码
     * @param pageSize 每页条数
     * @return 当前页
     */
    @GetMapping
    @Operation(summary = "我的下载列表")
    public R<PageResponse<DownloadTask>> page(@RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(downloads.page(page, pageSize));
    }

    /**
     * 读取单个下载任务。
     *
     * @param id 任务 ID
     * @return 任务
     */
    @GetMapping("/{id}")
    @Operation(summary = "下载任务详情")
    public R<DownloadTask> get(@PathVariable String id) {
        return ok(downloads.get(id));
    }

    /**
     * 复核来源权限后签发短时效下载链接。
     *
     * @param id 任务 ID
     * @return 链接
     */
    @PostMapping("/{id}/link")
    @Operation(summary = "签发下载链接")
    public R<DownloadLink> link(@PathVariable String id) {
        return ok(downloads.link(id));
    }
}
