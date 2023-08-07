package com.ingot.cloud.pms.web.v1;

import com.ingot.framework.core.error.exception.IllegalOperationException;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.oss.OSSService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>Description  : OSSCommonAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/7.</p>
 * <p>Time         : 10:13 AM.</p>
 */
@RestController
@RequestMapping(value = "/v1/oss")
@RequiredArgsConstructor
public class OSSCommonAPI implements RShortcuts {
    private final OSSService ossService;

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
