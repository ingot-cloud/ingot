package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;

/**
 * <p>安全事件优先级分类器。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface PriorityClassifier {

    RecordPriority classify(SecurityEventRecord record);
}
