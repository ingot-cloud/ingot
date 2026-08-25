package com.ingot.framework.dict.client.internal;

import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.event.DictInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;

/**
 * <p>字典失效广播的订阅入口：全量失效走 {@link DictService#evictAll()}，
 * 按编码失效走 {@link DictService#evict(String)}。</p>
 *
 * <p>字典的域标识是运行期才知道的 {@code dictCode}，无法预先 {@code register} 每个编码，
 * 因此覆盖 {@link #handle} 直接派发到 {@link DictService}。</p>
 *
 * @author jy
 * @since 2026/4/27
 * @see LayeredCacheCoordinator
 */
public class DictCacheCoordinator extends LayeredCacheCoordinator<DictInvalidationEvent, String> {

    private final DictService dictService;

    public DictCacheCoordinator(InvalidationBus bus, DictService dictService) {
        super(bus, DictInvalidationEvent.class,
                event -> event.isAll() ? null : event.getDictCode(), null);
        this.dictService = dictService;
    }

    @Override
    protected void handle(DictInvalidationEvent event) {
        if (event.isAll() || event.getDictCode() == null || event.getDictCode().isBlank()) {
            dictService.evictAll();
        } else {
            dictService.evict(event.getDictCode());
        }
    }
}
