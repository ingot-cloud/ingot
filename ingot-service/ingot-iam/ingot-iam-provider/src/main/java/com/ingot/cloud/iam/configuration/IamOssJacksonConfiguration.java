package com.ingot.cloud.iam.configuration;

import com.ingot.framework.commons.jackson.InJackson2ObjectMapperBuilderCustomizer;
import com.ingot.cloud.iam.application.catalog.model.ApplicationRecord;
import com.ingot.cloud.iam.application.catalog.model.AudienceMemberView;
import com.ingot.cloud.iam.domain.catalog.ApplicationSummary;
import com.ingot.cloud.iam.domain.account.CurrentProfile;
import com.ingot.cloud.iam.application.catalog.model.EntitlementRecord;
import com.ingot.cloud.iam.domain.organization.MemberCreateInput;
import com.ingot.cloud.iam.application.organization.model.MemberProfileInput;
import com.ingot.cloud.iam.application.organization.model.MemberRecord;
import com.ingot.cloud.iam.application.organization.model.DepartmentLeaderRecord;
import com.ingot.cloud.iam.application.organization.model.OrganizationMemberPickerOption;
import com.ingot.cloud.iam.domain.tenant.TenantCreateInput;
import com.ingot.cloud.iam.application.tenant.model.TenantRecord;
import com.ingot.cloud.iam.application.tenant.model.TenantSettingsInput;
import com.ingot.cloud.iam.application.tenant.model.TenantUpdateInput;
import com.ingot.cloud.iam.application.tenant.model.TenantWorkbenchStatistics;
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
     * 为成员、组织、当前资料与可用人群成员的 {@code avatar}，以及应用和开通记录 Logo 路径的 {@code icon} 注册读写 mixin。
     *
     * @return ObjectMapper 定制器
     */
    @Bean
    public InJackson2ObjectMapperBuilderCustomizer iamOssAvatarMixins() {
        return builder -> {
            builder.mixIn(MemberRecord.class, ReadAvatar.class);
            builder.mixIn(AudienceMemberView.class, ReadAvatar.class);
            builder.mixIn(DepartmentLeaderRecord.class, ReadAvatar.class);
            builder.mixIn(OrganizationMemberPickerOption.class, ReadAvatar.class);
            builder.mixIn(TenantRecord.class, ReadAvatar.class);
            builder.mixIn(TenantWorkbenchStatistics.class, ReadTenantAvatar.class);
            builder.mixIn(CurrentProfile.class, ReadAvatar.class);
            builder.mixIn(ApplicationRecord.class, ReadIcon.class);
            builder.mixIn(ApplicationSummary.class, ReadIcon.class);
            builder.mixIn(EntitlementRecord.class, ReadIcon.class);
            builder.mixIn(MemberCreateInput.class, SaveAvatar.class);
            builder.mixIn(MemberCreateInput.JacksonInput.class, SaveAvatar.class);
            builder.mixIn(MemberProfileInput.class, SaveAvatar.class);
            builder.mixIn(MemberProfileInput.JacksonInput.class, SaveAvatar.class);
            builder.mixIn(TenantCreateInput.class, SaveAvatar.class);
            builder.mixIn(TenantUpdateInput.class, SaveAvatar.class);
            builder.mixIn(TenantSettingsInput.class, SaveAvatar.class);
        };
    }

    /**
     * <p>为工作台当前组织头像签发时效链接。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    private abstract static class ReadTenantAvatar {
        @OssUrl
        abstract String tenantAvatar();
    }

    private abstract static class ReadAvatar {
        @OssUrl
        abstract String avatar();
    }

    private abstract static class ReadIcon {
        @OssUrl
        abstract String icon();
    }

    private abstract static class SaveAvatar {
        @OssSaveUrl
        abstract String avatar();
    }
}
