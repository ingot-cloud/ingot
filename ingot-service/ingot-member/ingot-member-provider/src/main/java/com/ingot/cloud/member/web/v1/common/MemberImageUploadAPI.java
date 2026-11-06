package com.ingot.cloud.member.web.v1.common;

import java.io.IOException;

import com.ingot.cloud.member.asset.MemberImageAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImageUploadPurpose;
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
 * <p>为已授权的后台操作和 App 本人资料暂存图片。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping(UploadForm.PATH)
@RequiredArgsConstructor
public class MemberImageUploadAPI implements RShortcuts {
    private final MemberImageAccess access;
    private final StagedImageUploads uploads;

    /** 根据已登记用途和目标检查权限后上传图片。 */
    @PostMapping
    public R<AssetUpload> upload(MultipartHttpServletRequest request) throws IOException {
        var form = UploadForm.read(request);
        var purpose = form.purpose();
        Long target;
        try {
            target = form.targetId() == null ? null : Long.valueOf(form.targetId());
            if (target != null && target <= 0) throw new NumberFormatException();
        } catch (NumberFormatException failure) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var owner = access.require(purpose, target);
        if (purpose == ImageUploadPurpose.APP_SELF_UPDATE) {
            target = access.requireAppUser();
        }
        try (var stream = form.file().getInputStream()) {
            return ok(uploads.stage(owner, purpose.getValue(), target == null ? null : target.toString(), stream));
        }
    }
}
