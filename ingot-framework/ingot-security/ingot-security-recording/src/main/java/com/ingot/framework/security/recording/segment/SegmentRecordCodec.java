package com.ingot.framework.security.recording.segment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

/**
 * <p>append-only segment 记录编解码：{@code length + crc32 + payload}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SegmentRecordCodec {

    public static final int HEADER_BYTES = 8;

    private SegmentRecordCodec() {
    }

    public static void writeRecord(OutputStream out, byte[] payload) throws IOException {
        CRC32 crc32 = new CRC32();
        crc32.update(payload);
        ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.BIG_ENDIAN);
        header.putInt(payload.length);
        header.putInt((int) crc32.getValue());
        out.write(header.array());
        out.write(payload);
    }

    public static byte[] readRecord(InputStream in) throws IOException {
        byte[] headerBytes = in.readNBytes(HEADER_BYTES);
        if (headerBytes.length == 0) {
            return null;
        }
        if (headerBytes.length < HEADER_BYTES) {
            throw new CorruptSegmentException("incomplete record header");
        }
        ByteBuffer header = ByteBuffer.wrap(headerBytes).order(ByteOrder.BIG_ENDIAN);
        int length = header.getInt();
        int expectedCrc = header.getInt();
        if (length < 0 || length > 16 * 1024 * 1024) {
            throw new CorruptSegmentException("invalid record length: " + length);
        }
        byte[] payload = in.readNBytes(length);
        if (payload.length < length) {
            throw new CorruptSegmentException("incomplete record payload");
        }
        CRC32 crc32 = new CRC32();
        crc32.update(payload);
        if ((int) crc32.getValue() != expectedCrc) {
            throw new CorruptSegmentException("checksum mismatch");
        }
        return payload;
    }

    /**
     * <p>segment 损坏异常，触发 quarantine。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    public static final class CorruptSegmentException extends IOException {
        public CorruptSegmentException(String message) {
            super(message);
        }
    }
}
