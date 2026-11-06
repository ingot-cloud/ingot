package com.ingot.cloud.iam.asset;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImageUploadPurpose;
import com.ingot.framework.oss.upload.StagedImageUploads;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>在业务事务内消费当前 IAM 身份已上传的图片。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class IamImageBinding {
    private final StagedImageUploads uploads;
    private final IamImageUploadAccess access;

    /** 绑定凭证或保留已有引用。调用前须已通过对应业务写入授权。 */
    public String bind(AuthorizationContext actor, ImageUploadPurpose purpose, String target,
                       String submitted, String existing) {
        String owner = StagedImageUploads.iamOwner(actor);
        if (StagedImageUploads.isReference(submitted)
                && !owner.equals(StagedImageUploads.iamOwner(access.require(purpose, target).context()))) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        return uploads.bind(owner, purpose.getValue(), target, submitted, existing,
                purpose == ImageUploadPurpose.APPLICATION_CREATE || purpose == ImageUploadPurpose.APPLICATION_UPDATE);
    }
}
