package com.ingot.cloud.iam.support;

import com.ingot.framework.oss.common.OssObjectInfo;
import com.ingot.framework.oss.common.OssPathParser;

/**
 * <p>把头像引用规范成可入库的 OSS 对象路径，去掉带时效的完整链接。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamOssPaths {
    private IamOssPaths() {
    }

    /**
     * 将客户端提交的头像写成 {@code bucket/objectName}。
     *
     * <p>预签名 URL 只取路径段；已经是对象路径时原样规范化；空引用表示不修改，空白表示清空。
     * 无法解析为对象路径的非 URL 字面量保持原值，避免测试桩或历史短值被拒绝。</p>
     *
     * @param avatar 预签名 URL、对象路径、空白或空引用
     * @return 可入库路径；入参为空时返回 {@code null} 或去空白后的空串
     */
    public static String store(String avatar) {
        if (avatar == null) {
            return null;
        }
        String value = avatar.trim();
        if (value.isEmpty()) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            OssObjectInfo object = OssPathParser.parse(value);
            return object.bucket() + "/" + object.objectName();
        }
        try {
            OssObjectInfo object = OssPathParser.parse(value);
            if (object.objectName() == null || object.objectName().isEmpty()) {
                return value;
            }
            return object.bucket() + "/" + object.objectName();
        } catch (IllegalArgumentException exception) {
            return value;
        }
    }
}
