package com.ingot.framework.core.oss;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : OSSResult.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/6.</p>
 * <p>Time         : 1:43 PM.</p>
 */
@Data
public class OSSResult implements Serializable {
    /**
     * Bucket name
     */
    private String bucketName;
    /**
     * File name
     */
    private String fileName;
    /**
     * Access url
     */
    private String url;
}
