package com.ingot.framework.security.recording.segment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>解析 {@code 1GB}/{@code 64MB} 等字节大小配置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ByteSizeParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*([KMGTP]?B?)?$", Pattern.CASE_INSENSITIVE);

    private ByteSizeParser() {
    }

    public static long parse(String raw, long defaultBytes) {
        if (raw == null || raw.isBlank()) {
            return defaultBytes;
        }
        Matcher matcher = PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid byte size: " + raw);
        }
        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2) == null ? "B" : matcher.group(2).toUpperCase(Locale.ROOT);
        long multiplier = switch (unit) {
            case "B", "" -> 1L;
            case "KB", "K" -> 1024L;
            case "MB", "M" -> 1024L * 1024L;
            case "GB", "G" -> 1024L * 1024L * 1024L;
            case "TB", "T" -> 1024L * 1024L * 1024L * 1024L;
            default -> throw new IllegalArgumentException("Unsupported byte unit: " + unit);
        };
        return (long) (value * multiplier);
    }
}
