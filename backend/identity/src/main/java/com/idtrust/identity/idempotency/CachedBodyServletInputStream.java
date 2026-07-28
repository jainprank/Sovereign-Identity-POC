package com.idtrust.identity.idempotency;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Replays an already-fully-read request body as a fresh {@link ServletInputStream}. See
 * {@link CachedBodyHttpServletRequest}.
 */
class CachedBodyServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream buffer;

    CachedBodyServletInputStream(byte[] cachedBody) {
        this.buffer = new ByteArrayInputStream(cachedBody);
    }

    @Override
    public boolean isFinished() {
        return buffer.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Async I/O is not used by this filter chain");
    }

    @Override
    public int read() throws IOException {
        return buffer.read();
    }
}
