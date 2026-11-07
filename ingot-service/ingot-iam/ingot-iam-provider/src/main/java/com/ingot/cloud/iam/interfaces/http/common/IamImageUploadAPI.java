package com.ingot.cloud.iam.interfaces.http.common;

import java.io.IOException;

import com.ingot.cloud.iam.application.asset.IamImageUploadAccess;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.oss.upload.AssetUpload;
import com.ingot.framework.oss.upload.StagedImageUploads;
import com.ingot.framework.oss.upload.UploadForm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * <p>通过已登记的 IAM 业务操作暂存头像和图标。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping(UploadForm.PATH)
@RequiredArgsConstructor
public class IamImageUploadAPI implements RShortcuts {
    private final IamImageUploadAccess access;
    private final StagedImageUploads uploads;

    /** 校验业务用途与目标后上传图片，返回仅能绑定该用途的限时凭证。 */
    @PostMapping
    public R<AssetUpload> upload(MultipartHttpServletRequest request) throws IOException {
        var form = UploadForm.read(request);
        var purpose = form.purpose();
        String target = form.targetId() == null ? null : IamIds.text(IamIds.require(form.targetId()));
        var actor = access.require(purpose, target);
        try (var stream = form.file().getInputStream()) {
            return ok(uploads.stage(StagedImageUploads.iamOwner(actor.context()), purpose.getValue(), target, stream));
        }
    }
}
