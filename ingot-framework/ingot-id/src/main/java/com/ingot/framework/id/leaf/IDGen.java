package com.ingot.framework.id.leaf;


import com.ingot.framework.id.leaf.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
