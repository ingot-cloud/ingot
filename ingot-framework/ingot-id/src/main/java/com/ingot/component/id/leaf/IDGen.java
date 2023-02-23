package com.ingot.component.id.leaf;


import com.ingot.component.id.leaf.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
