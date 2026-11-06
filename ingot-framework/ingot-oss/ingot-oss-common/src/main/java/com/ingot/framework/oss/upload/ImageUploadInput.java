package com.ingot.framework.oss.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>对暂存图片按实际解码格式、尺寸及静态帧约束验证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
record ImageUploadInput(byte[] bytes, String extension) {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DIMENSION = 2048;
    private static final Set<String> FORMATS = Set.of("jpeg", "png", "webp");
    private static final int PNG_HEADER_BYTES = 8;
    private static final int WEBP_HEADER_BYTES = 12;
    private static final int CHUNK_HEADER_BYTES = 8;

    static ImageUploadInput read(InputStream input) {
        try {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                throw invalid();
            }
            try (var images = new MemoryCacheImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(images);
                if (!readers.hasNext()) {
                    throw invalid();
                }
                var reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!FORMATS.contains(format) || animated(bytes, format)) {
                        throw invalid();
                    }
                    reader.setInput(images, false, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                            || reader.getNumImages(true) != 1 || reader.read(0) == null) {
                        throw invalid();
                    }
                    return new ImageUploadInput(bytes, format);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw invalid();
        }
    }

    private static boolean animated(byte[] bytes, String format) {
        if ("png".equals(format)) {
            int offset = PNG_HEADER_BYTES;
            while (offset + CHUNK_HEADER_BYTES <= bytes.length) {
                int length = java.nio.ByteBuffer.wrap(bytes, offset, Integer.BYTES).getInt();
                if (chunk(bytes, offset + Integer.BYTES, "acTL")) return true;
                if (length < 0 || length > bytes.length - offset - CHUNK_HEADER_BYTES - Integer.BYTES) {
                    throw invalid();
                }
                offset += CHUNK_HEADER_BYTES + Integer.BYTES + length;
            }
        } else if ("webp".equals(format)) {
            int offset = WEBP_HEADER_BYTES;
            while (offset + CHUNK_HEADER_BYTES <= bytes.length) {
                if (chunk(bytes, offset, "ANIM") || chunk(bytes, offset, "ANMF")) return true;
                int length = java.nio.ByteBuffer.wrap(bytes, offset + Integer.BYTES, Integer.BYTES)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
                if (length < 0 || length > bytes.length - offset - CHUNK_HEADER_BYTES) throw invalid();
                offset += CHUNK_HEADER_BYTES + length + (length & 1);
            }
        }
        return false;
    }

    private static boolean chunk(byte[] bytes, int offset, String name) {
        for (int i = 0; i < name.length(); i++) {
            if (bytes[offset + i] != name.charAt(i)) return false;
        }
        return true;
    }

    private static BizException invalid() {
        return new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "图片格式、尺寸或文件大小不符合要求");
    }
}
