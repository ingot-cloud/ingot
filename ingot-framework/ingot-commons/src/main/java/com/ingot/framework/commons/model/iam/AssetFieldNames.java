package com.ingot.framework.commons.model.iam;

/**
 * <p>上传凭证在业务写入请求中对应的图片字段。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AssetFieldNames {
    /** 提交头像或组织 Logo 的暂存凭证。 */
    public static final String AVATAR_UPLOAD_ID = "avatarUploadId";
    /** 提交应用图标的暂存凭证。 */
    public static final String ICON_UPLOAD_ID = "iconUploadId";

    private AssetFieldNames() {
    }
}
