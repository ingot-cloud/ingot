package com.ingot.framework.crypto.model;

/**
 * <p>Description  : CryptoInfoRecord.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 11:14 AM.</p>
 */
public record CryptoInfoRecord(CryptoType type, String secretKey) {
}
