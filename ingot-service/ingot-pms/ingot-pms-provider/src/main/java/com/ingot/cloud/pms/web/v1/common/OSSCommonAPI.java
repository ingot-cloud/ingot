package com.ingot.cloud.pms.web.v1.common;

import java.io.IOException;

import com.ingot.framework.commons.error.IllegalOperationException;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.oss.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>Description  : OSSCommonAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/7.</p>
 * <p>Time         : 10:13 AM.</p>
 */
@RestController
@Tag(description = "oss", name = "OSS模块")
@RequestMapping(value = "/v1/oss")
@RequiredArgsConstructor
public class OSSCommonAPI implements RShortcuts {
    private final OssService ossService;

    @Operation(summary = "上传文件", description = "上传文件")
    @PostMapping("/upload")
    public R<?> upload(@RequestParam("file") MultipartFile file,
                       @RequestParam(value = "bucketName") String bucketName,
                       @RequestParam(value = "fileName") String fileName) {
        try {
            return ok(ossService.uploadFile(bucketName, fileName, file.getInputStream()));
        } catch (IOException e) {
            throw new IllegalOperationException("上传失败");
        }
    }
}
