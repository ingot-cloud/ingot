package com.ingot.frameword.id.leaf;


import com.ingot.frameword.id.leaf.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
