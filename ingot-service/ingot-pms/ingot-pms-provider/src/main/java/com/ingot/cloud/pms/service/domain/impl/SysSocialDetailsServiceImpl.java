package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.Collection;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.mapper.SysSocialDetailsMapper;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.social.common.publisher.SocialConfigMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类 - 支持配置变更通知
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysSocialDetailsServiceImpl extends BaseServiceImpl<SysSocialDetailsMapper, SysSocialDetails> implements SysSocialDetailsService {
    private final SocialConfigMessagePublisher configMessagePublisher;

    @Override
    public boolean save(SysSocialDetails entity) {
        boolean result = super.save(entity);
        if (result && shouldNotify(entity)) {
            log.info("SysSocialDetailsServiceImpl - 社交配置新增成功，通知配置变更: type={}, appId={}", 
                    entity.getType(), entity.getAppId());
            publishAdd(entity);
        }
        return result;
    }

    @Override
    public boolean updateById(SysSocialDetails entity) {
        boolean result = super.updateById(entity);
        if (result && shouldNotify(entity)) {
            log.info("SysSocialDetailsServiceImpl - 社交配置更新成功，通知配置变更: type={}, appId={}", 
                    entity.getType(), entity.getAppId());
            publishUpdate(entity);
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        // 先获取entity以便获取appId和type
        SysSocialDetails entity = getById(id);
        boolean result = super.removeById(id);
        if (result && shouldNotify(entity)) {
            log.info("SysSocialDetailsServiceImpl - 社交配置删除成功，通知配置变更: type={}, appId={}", 
                    entity.getType(), entity.getAppId());
            publishDelete(entity);
        }
        return result;
    }

    @Override
    public boolean saveBatch(Collection<SysSocialDetails> entityList, int batchSize) {
        boolean result = super.saveBatch(entityList, batchSize);
        if (result) {
            // 按社交类型分组通知
            notifyBatchChange(entityList, "批量新增");
        }
        return result;
    }

    @Override
    public boolean updateBatchById(Collection<SysSocialDetails> entityList, int batchSize) {
        boolean result = super.updateBatchById(entityList, batchSize);
        if (result) {
            // 按社交类型分组通知
            notifyBatchChange(entityList, "批量更新");
        }
        return result;
    }

    @Override
    public boolean removeByIds(Collection<?> list) {
        boolean result = super.removeByIds(list);
        if (result) {
            log.info("SysSocialDetailsServiceImpl - 批量删除社交配置成功，通知刷新所有配置");
            // 删除时不知道具体类型，通知所有类型刷新
            for (SocialTypeEnum type : SocialTypeEnum.values()) {
                publishRefreshAll(type);
            }
        }
        return result;
    }
    
    /**
     * 批量变更时按社交类型通知
     */
    private void notifyBatchChange(Collection<SysSocialDetails> entityList, String operation) {
        if (entityList == null || entityList.isEmpty()) {
            return;
        }
        
        // 按社交类型分组
        entityList.stream()
                .filter(this::shouldNotify)
                .map(SysSocialDetails::getType)
                .distinct()
                .forEach(type -> {
                    log.info("SysSocialDetailsServiceImpl - {}社交配置成功，通知刷新配置: type={}", operation, type);
                    publishRefreshAll(type);
                });
    }

    /**
     * 发布添加配置的消息
     */
    private void publishAdd(SysSocialDetails entity) {
        if (configMessagePublisher != null) {
            try {
                configMessagePublisher.publishAdd(entity.getType(), entity.getAppId());
            } catch (Exception e) {
                log.warn("SysSocialDetailsServiceImpl - 发布配置变更消息失败", e);
            }
        }
    }

    /**
     * 发布更新配置的消息
     */
    private void publishUpdate(SysSocialDetails entity) {
        if (configMessagePublisher != null) {
            try {
                configMessagePublisher.publishUpdate(entity.getType(), entity.getAppId());
            } catch (Exception e) {
                log.warn("SysSocialDetailsServiceImpl - 发布配置变更消息失败", e);
            }
        }
    }

    /**
     * 发布删除配置的消息
     */
    private void publishDelete(SysSocialDetails entity) {
        if (configMessagePublisher != null) {
            try {
                configMessagePublisher.publishDelete(entity.getType(), entity.getAppId());
            } catch (Exception e) {
                log.warn("SysSocialDetailsServiceImpl - 发布配置变更消息失败", e);
            }
        }
    }

    /**
     * 发布刷新所有配置的消息
     */
    private void publishRefreshAll(SocialTypeEnum socialType) {
        if (configMessagePublisher != null) {
            try {
                configMessagePublisher.publishRefreshAll(socialType);
            } catch (Exception e) {
                log.warn("SysSocialDetailsServiceImpl - 发布配置变更消息失败", e);
            }
        }
    }

    /**
     * 判断是否需要通知
     */
    private boolean shouldNotify(SysSocialDetails entity) {
        return entity != null
                && entity.getType() != null
                && StrUtil.isNotBlank(entity.getAppId());
    }
}
