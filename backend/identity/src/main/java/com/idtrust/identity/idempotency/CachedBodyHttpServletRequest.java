package com.idtrust.identity.idempotency;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reads the request body once, up front, and caches it - so {@link IdempotencyFilter} can hash
 * the body for replay-detection while every downstream reader (Spring MVC's {@code @RequestBody}
 * deserialization) still sees a full, independently-readable stream. A plain {@code
 * ContentCachingRequestWrapper} does not support this: it only caches bytes as something else
 * reads them, so once this filter itself consumed the stream, a later reader would see EOF.
 */
class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
