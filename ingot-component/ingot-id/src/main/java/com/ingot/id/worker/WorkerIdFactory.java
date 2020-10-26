package com.ingot.id.worker;

/**
 * <p>Description  : WorkerIdFactory.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/24.</p>
 * <p>Time         : 8:47 下午.</p>
 */
public interface WorkerIdFactory {

    /**
     * 初始化
     * @return 初始化是否成功
     */
    boolean init();

    /**
     * 获取 worker id
     * @return Worker Id
     */
    int getWorkerId();
}
