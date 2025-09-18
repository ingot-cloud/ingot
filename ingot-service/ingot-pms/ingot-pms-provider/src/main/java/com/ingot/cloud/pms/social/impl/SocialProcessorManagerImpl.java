package com.ingot.cloud.pms.social.impl;

import java.util.List;

import com.ingot.cloud.pms.common.PmsMessageSource;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.commons.error.IllegalOperationException;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : SocialProcessorManagerImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 4:12 PM.</p>
 */
@Service
@RequiredArgsConstructor
public class SocialProcessorManagerImpl implements SocialProcessorManager {
    private final List<SocialProcessor<?>> socialProcessorList;

    @Override
    @SuppressWarnings({"rawtypes"})
    public String getUniqueID(SocialTypeEnum socialType, String code) {
        for (SocialProcessor processor : socialProcessorList) {
            if (processor.support(socialType)) {
                return processor.getUniqueID(code);
            }
        }
        throw new IllegalOperationException(
                PmsMessageSource.getAccessor().getMessage("SocialProcessorManagerImpl.SocialTypeIllegal"));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T getUserInfo(SocialTypeEnum socialType, String uniqueID) {
        for (SocialProcessor processor : socialProcessorList) {
            if (processor.support(socialType)) {
                return (T) processor.getUserInfo(uniqueID);
            }
        }
        throw new IllegalOperationException(
                PmsMessageSource.getAccessor().getMessage("SocialProcessorManagerImpl.SocialTypeIllegal"));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void bind(SocialTypeEnum socialType, T user, String uniqueID) {
        for (SocialProcessor processor : socialProcessorList) {
            if (processor.support(socialType)) {
                processor.bind(user, uniqueID);
                return;
            }
        }
        throw new IllegalOperationException(
                PmsMessageSource.getAccessor().getMessage("SocialProcessorManagerImpl.SocialTypeIllegal"));
    }
}
