package com.ingot.id;

/**
 * <p>Description  : IdGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/24.</p>
 * <p>Time         : 7:40 下午.</p>
 */
public interface IdGenerator {

    /**
     * 获取ID
     * @return long ID
     */
    long nextId();

}
