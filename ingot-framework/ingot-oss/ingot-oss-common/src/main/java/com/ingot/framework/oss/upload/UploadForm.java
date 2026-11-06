package com.ingot.framework.oss.upload;

import java.util.Set;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImageUploadPurpose;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * <p>解析固定的图片暂存表单，禁止请求指定存储位置。</p>
 *
 * @param purpose 业务用途
 * @param targetId 可选既有对象
 * @param file 唯一图片文件
 * @author jy
 * @since 1.0.0
 */
public record UploadForm(ImageUploadPurpose purpose, String targetId, MultipartFile file) {
    /** 服务内路由，由各业务服务网关前缀区分。 */
    public static final String PATH = "/v1/assets/uploads";

    private static final String PURPOSE = "purpose";
    private static final String TARGET = "targetId";
    private static final String FILE = "file";
    private static final Set<String> FIELDS = Set.of(PURPOSE, TARGET);

    /** 验证表单只有支持的标量和一个图片。 */
    public static UploadForm read(MultipartHttpServletRequest request) {
        var files = request.getMultiFileMap();
        var params = request.getParameterMap();
        if (files.size() != 1 || !files.containsKey(FILE) || files.get(FILE).size() != 1
                || !FIELDS.containsAll(params.keySet()) || !params.containsKey(PURPOSE)
                || params.values().stream().anyMatch(values -> values.length != 1)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        ImageUploadPurpose purpose;
        try {
            purpose = ImageUploadPurpose.getEnum(request.getParameter(PURPOSE));
        } catch (IllegalArgumentException failure) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (purpose == null) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        return new UploadForm(purpose, request.getParameter(TARGET), files.getFirst(FILE));
    }
}
