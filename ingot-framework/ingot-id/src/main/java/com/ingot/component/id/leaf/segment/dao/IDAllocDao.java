package com.ingot.component.id.leaf.segment.dao;

import java.util.List;

import com.ingot.component.id.leaf.segment.model.LeafAlloc;


public interface IDAllocDao {
     List<LeafAlloc> getAllLeafAllocs();
     LeafAlloc updateMaxIdAndGetLeafAlloc(String tag);
     LeafAlloc updateMaxIdByCustomStepAndGetLeafAlloc(LeafAlloc leafAlloc);
     List<String> getAllTags();
}
