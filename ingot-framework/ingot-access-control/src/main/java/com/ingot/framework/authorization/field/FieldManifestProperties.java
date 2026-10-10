package com.ingot.framework.authorization.field;

import java.util.Set;
import com.ingot.framework.commons.model.iam.extension.FieldManifestInvocation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>服务元数据验签配置，与使用成员身份的对象查询密钥独立。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter @Setter
@ConfigurationProperties(prefix = "ingot.iam.field-manifest")
public class FieldManifestProperties {
    /** 本服务清单专用签名密钥，至少 32 字节，缺省不发布端点。 */
    private String secret;
    /** 服务器允许的调用服务白名单。 */
    private Set<String> callers = Set.of(FieldManifestInvocation.IAM_CALLER);
}
