package com.ingot.framework.oss.upload;

/**
 * <p>客户端提交业务表单时使用的一次性图片上传凭证。</p>
 *
 * @param uploadId 不透明凭证，绑定创建者、业务用途及目标
 * @author jy
 * @since 1.0.0
 */
public record AssetUpload(String uploadId) {
}
