package com.ingot.cloud.iam.support;

import com.ingot.framework.commons.jackson.InJackson2ObjectMapperBuilderCustomizer;
import com.ingot.framework.commons.model.iam.CurrentProfile;
import com.ingot.framework.commons.model.iam.MemberCreateInput;
import com.ingot.framework.commons.model.iam.MemberProfileInput;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.TenantCreateInput;
import com.ingot.framework.commons.model.iam.TenantRecord;
import com.ingot.framework.commons.model.iam.TenantSettingsInput;
import com.ingot.framework.commons.model.iam.TenantUpdateInput;
import com.ingot.framework.oss.common.OssSaveUrl;
import com.ingot.framework.oss.common.OssUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>给 IAM 公共契约的头像字段挂上框架 OSS 注解，避免 commons 反向依赖 oss-common。</p>
 *
 * <p>读取侧把库存路径签发为时效链接；写入侧把客户端提交的时效链接还原为 {@code bucket/objectName}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class IamOssJacksonConfiguration {

    /**
     * 为成员、组织与当前资料的 {@code avatar} 注册读写 mixin。
     *
     * @return ObjectMapper 定制器
     */
    @Bean
    public InJackson2ObjectMapperBuilderCustomizer iamOssAvatarMixins() {
        return builder -> {
            builder.mixIn(MemberRecord.class, ReadAvatar.class);
            builder.mixIn(TenantRecord.class, ReadAvatar.class);
            builder.mixIn(CurrentProfile.class, ReadAvatar.class);
            builder.mixIn(MemberCreateInput.class, SaveAvatar.class);
            builder.mixIn(MemberProfileInput.class, SaveAvatar.class);
            builder.mixIn(TenantCreateInput.class, SaveAvatar.class);
            builder.mixIn(TenantUpdateInput.class, SaveAvatar.class);
            builder.mixIn(TenantSettingsInput.class, SaveAvatar.class);
        };
    }

    private abstract static class ReadAvatar {
        @OssUrl
        abstract String avatar();
    }

    private abstract static class SaveAvatar {
        @OssSaveUrl
        abstract String avatar();
    }
}
