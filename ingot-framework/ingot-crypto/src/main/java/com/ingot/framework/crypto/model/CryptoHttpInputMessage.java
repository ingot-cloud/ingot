package com.ingot.framework.crypto.model;

import java.io.IOException;
import java.io.InputStream;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.lang.NonNull;

/**
 * <p>Description  : CryptoHttpInputMessage.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 11:38 AM.</p>
 */
@RequiredArgsConstructor
public class CryptoHttpInputMessage implements HttpInputMessage {
    private final InputStream body;
    private final HttpHeaders headers;

    @Override
    @NonNull
    public InputStream getBody() throws IOException {
        return this.body;
    }

    @Override
    @NonNull
    public HttpHeaders getHeaders() {
        return this.headers;
    }
}
