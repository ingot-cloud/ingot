package com.ingot.cloud.pms.social.impl;

import com.ingot.cloud.pms.common.PmsMessageSource;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.core.error.exception.IllegalOperationException;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public String getUniqueID(SocialTypeEnums socialType, String code) {
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
    public <T> T getUserInfo(SocialTypeEnums socialType, String uniqueID) {
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
    public <T> void bind(SocialTypeEnums socialType, T user, String uniqueID) {
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
