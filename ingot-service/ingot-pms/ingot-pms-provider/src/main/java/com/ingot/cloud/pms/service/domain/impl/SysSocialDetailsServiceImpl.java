package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.Collection;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.mapper.SysSocialDetailsMapper;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.social.wechat.publisher.SocialConfigMessagePublisher;
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
            log.info("SysSocialDetailsServiceImpl - 社交配置新增成功，通知配置变更: appId={}", entity.getAppId());
            publishAdd(entity.getAppId());
        }
        return result;
    }

    @Override
    public boolean updateById(SysSocialDetails entity) {
        boolean result = super.updateById(entity);
        if (result && shouldNotify(entity)) {
            log.info("SysSocialDetailsServiceImpl - 社交配置更新成功，通知配置变更: appId={}", entity.getAppId());
            publishUpdate(entity.getAppId());
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        // 先获取entity以便获取appId
        SysSocialDetails entity = getById(id);
        boolean result = super.removeById(id);
        if (result && shouldNotify(entity)) {
            log.info("SysSocialDetailsServiceImpl - 社交配置删除成功，通知配置变更: appId={}", entity.getAppId());
            publishDelete(entity.getAppId());
        }
        return result;
    }

    @Override
    public boolean saveBatch(Collection<SysSocialDetails> entityList, int batchSize) {
        boolean result = super.saveBatch(entityList, batchSize);
        if (result && hasWechatMiniProgram(entityList)) {
            log.info("SysSocialDetailsServiceImpl - 批量新增社交配置成功，通知刷新所有配置");
            publishRefreshAll();
        }
        return result;
    }

    @Override
    public boolean updateBatchById(Collection<SysSocialDetails> entityList, int batchSize) {
        boolean result = super.updateBatchById(entityList, batchSize);
        if (result && hasWechatMiniProgram(entityList)) {
            log.info("SysSocialDetailsServiceImpl - 批量更新社交配置成功，通知刷新所有配置");
            publishRefreshAll();
        }
        return result;
    }

    @Override
    public boolean removeByIds(Collection<?> list) {
        boolean result = super.removeByIds(list);
        if (result) {
            log.info("SysSocialDetailsServiceImpl - 批量删除社交配置成功，通知刷新所有配置");
            publishRefreshAll();
        }
        return result;
    }

    /**
     * 发布添加配置的消息
     */
    private void publishAdd(String appId) {
        if (configMessagePublisher != null) {
            try {
                configMessagePublisher.publishAdd(appId);
            } catch (Exception e) {
                log.warn("SysSocialDetailsServiceImpl - 发布配置变更消息失败", e);
            }
        }
    }

    /**
     * 发布更新配置的消息
     */
    private void publishUpdate(String appId) {
        if (configMessagePublisher != null) {
            try {
                configMessagePublisher.publishUpdate(appId);
            } catch (Exception e) {
                log.warn("SysSocialDetailsServiceImpl - 发布配置变更消息失败", e);
            }
        }
    }

    /**
     * 发布删除配置的消息
     */
    private void publishDelete(String appId) {
        if (configMessagePublisher != null) {
            try {
                configMessagePublisher.publishDelete(appId);
            } catch (Exception e) {
                log.warn("SysSocialDetailsServiceImpl - 发布配置变更消息失败", e);
            }
        }
    }

    /**
     * 发布刷新所有配置的消息
     */
    private void publishRefreshAll() {
        if (configMessagePublisher != null) {
            try {
                configMessagePublisher.publishRefreshAll();
            } catch (Exception e) {
                log.warn("SysSocialDetailsServiceImpl - 发布配置变更消息失败", e);
            }
        }
    }

    /**
     * 判断是否需要通知（仅微信小程序类型需要通知）
     */
    private boolean shouldNotify(SysSocialDetails entity) {
        return entity != null
                && entity.getType() == SocialTypeEnum.WECHAT_MINI_PROGRAM
                && StrUtil.isNotBlank(entity.getAppId());
    }

    /**
     * 判断集合中是否包含微信小程序配置
     */
    private boolean hasWechatMiniProgram(Collection<SysSocialDetails> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        return entityList.stream().anyMatch(this::shouldNotify);
    }
}
