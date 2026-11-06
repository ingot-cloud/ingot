package com.ingot.cloud.member.asset;

import com.ingot.cloud.member.authorization.MemberGlobalAccess;
import com.ingot.cloud.member.service.domain.MemberUserService;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImageUploadPurpose;
import com.ingot.framework.commons.model.iam.MemberAdminResources;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.oss.upload.StagedImageUploads;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>区分平台代管 App 用户头像与 App 用户本人头像的授权边界。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MemberImageAccess {
    private final MemberGlobalAccess access;
    private final MemberUserService users;
    private final StagedImageUploads uploads;

    /** 恢复可信的 App 用户身份。 */
    public long requireAppUser() {
        var user = SecurityAuthContext.getUser();
        if (user == null || user.getId() == null || !UserTypeEnum.APP.getValue().equals(user.getUserType())) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        var member = users.getById(user.getId());
        if (member == null || !Boolean.TRUE.equals(member.getEnabled()) || Boolean.TRUE.equals(member.getLocked())) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        return user.getId();
    }

    /** 校验图片用途、可信身份及已有目标，返回暂存凭证归属键。 */
    public String require(ImageUploadPurpose purpose, Long target) {
        return switch (purpose) {
            case APP_USER_CREATE -> {
                if (target != null) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                yield StagedImageUploads.iamOwner(access.requireContext(MemberAdminResources.USER,
                        MemberAdminResources.USER_CREATE));
            }
            case APP_USER_UPDATE -> {
                var actor = access.requireContext(MemberAdminResources.USER, MemberAdminResources.USER_UPDATE);
                if (target == null || users.getById(target) == null) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                yield StagedImageUploads.iamOwner(actor);
            }
            case APP_SELF_UPDATE -> {
                if (target != null) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                yield UserTypeEnum.APP.name() + "/" + requireAppUser();
            }
            default -> throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        };
    }

    /** 同事务绑定 App 用户图片，按当前身份重新检查用途。 */
    public String bind(ImageUploadPurpose purpose, Long target, String submitted, String current) {
        if (submitted == null) return null;
        return uploads.bind(require(purpose, purpose == ImageUploadPurpose.APP_SELF_UPDATE ? null : target),
                purpose.getValue(), target == null ? null : target.toString(), submitted, current, false);
    }
}
