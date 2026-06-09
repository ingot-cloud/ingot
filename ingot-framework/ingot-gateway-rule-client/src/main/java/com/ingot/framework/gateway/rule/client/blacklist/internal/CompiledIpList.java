package com.ingot.framework.gateway.rule.client.blacklist.internal;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListItem;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListType;
import lombok.extern.slf4j.Slf4j;

/**
 * 编译后的黑白名单索引：按 listType + keyType 拆分到独立集合 / 正则列表，
 * 实现 O(1) IP / 用户 / 设备命中判断，UA / Referer 走正则。
 *
 * <p>编译期已过滤 {@code enabled=false}、{@code effectiveAt} 未到、{@code expiresAt} 已过的条目；
 * 非法正则跳过并打 warn 日志。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
public class CompiledIpList {

    private static final CompiledIpList EMPTY = new CompiledIpList();

    private final Map<IpKeyType, Set<String>> blackExact = new EnumMap<>(IpKeyType.class);
    private final Map<IpKeyType, Set<String>> whiteExact = new EnumMap<>(IpKeyType.class);
    private final List<Pattern> blackUaPatterns = new ArrayList<>();
    private final List<Pattern> whiteUaPatterns = new ArrayList<>();
    private final List<Pattern> blackRefererPatterns = new ArrayList<>();
    private final List<Pattern> whiteRefererPatterns = new ArrayList<>();
    private final List<String> blackCidrs = new ArrayList<>();
    private final List<String> whiteCidrs = new ArrayList<>();

    /** 空索引单例，无任何命中条目。 */
    public static CompiledIpList empty() {
        return EMPTY;
    }

    /**
     * 将原始名单条目编译为多维索引。
     * <p>编译期过滤 {@code enabled=false}、生效时间未到、已过期条目；非法正则跳过并打 warn。</p>
     *
     * @param items 原始条目列表；null 或空时返回空索引
     */
    public static CompiledIpList compile(List<IpListItem> items) {
        CompiledIpList compiled = new CompiledIpList();
        if (items == null || items.isEmpty()) {
            return compiled;
        }
        LocalDateTime now = LocalDateTime.now();
        for (IpListItem item : items) {
            if (!isActive(item, now)) {
                continue;
            }
            boolean blacklist = item.getListType() == IpListType.BLACK;
            String key = item.getKeyValue();
            if (key == null || key.isBlank()) {
                continue;
            }
            switch (item.getKeyType()) {
                case IP, DEVICE, USER -> compiled.exact(blacklist, item.getKeyType()).add(key);
                case CIDR -> (blacklist ? compiled.blackCidrs : compiled.whiteCidrs).add(key);
                case USER_AGENT -> addPattern(blacklist, key, item, compiled.blackUaPatterns,
                        compiled.whiteUaPatterns);
                case REFERER -> addPattern(blacklist, key, item, compiled.blackRefererPatterns,
                        compiled.whiteRefererPatterns);
                default -> {
                }
            }
        }
        return compiled;
    }

    /**
     * 黑名单匹配：IP / 设备 / 用户 / CIDR / UA / Referer 任一维度命中即 true。
     */
    public boolean isBlocked(String ip, String device, String user, String ua, String referer) {
        return match(true, ip, device, user, ua, referer);
    }

    /**
     * 白名单匹配：逻辑同 {@link #isBlocked}，查 white 侧索引。
     */
    public boolean isWhitelisted(String ip, String device, String user, String ua, String referer) {
        return match(false, ip, device, user, ua, referer);
    }

    /**
     * 精确键查询：判断指定 keyType + keyValue 是否在黑/白名单精确集合中（不含 CIDR / 正则）。
     */
    public boolean contains(IpKeyType keyType, String keyValue, boolean blacklist) {
        Set<String> set = blacklist ? blackExact.get(keyType) : whiteExact.get(keyType);
        return set != null && set.contains(keyValue);
    }

    private boolean match(boolean blacklist, String ip, String device, String user,
                        String ua, String referer) {
        if (notBlank(ip) && exact(blacklist, IpKeyType.IP).contains(ip)) {
            return true;
        }
        if (notBlank(device) && exact(blacklist, IpKeyType.DEVICE).contains(device)) {
            return true;
        }
        if (notBlank(user) && exact(blacklist, IpKeyType.USER).contains(user)) {
            return true;
        }
        if (notBlank(ip) && cidrMatch(blacklist, ip)) {
            return true;
        }
        if (notBlank(ua) && patternMatch(blacklist, ua, true)) {
            return true;
        }
        if (notBlank(referer) && patternMatch(blacklist, referer, false)) {
            return true;
        }
        return false;
    }

    private boolean patternMatch(boolean blacklist, String value, boolean ua) {
        List<Pattern> patterns = blacklist
                ? (ua ? blackUaPatterns : blackRefererPatterns)
                : (ua ? whiteUaPatterns : whiteRefererPatterns);
        for (Pattern p : patterns) {
            if (p.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean cidrMatch(boolean blacklist, String ip) {
        List<String> cidrs = blacklist ? blackCidrs : whiteCidrs;
        for (String cidr : cidrs) {
            if (CidrMatcher.matches(cidr, ip)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> exact(boolean blacklist, IpKeyType type) {
        Map<IpKeyType, Set<String>> map = blacklist ? blackExact : whiteExact;
        return map.computeIfAbsent(type, k -> new HashSet<>());
    }

    private static void addPattern(boolean blacklist, String regex, IpListItem item,
                                   List<Pattern> blackList, List<Pattern> whiteList) {
        try {
            Pattern p = Pattern.compile(regex);
            (blacklist ? blackList : whiteList).add(p);
        } catch (PatternSyntaxException e) {
            log.warn("[Blacklist] skip invalid regex, keyType={} value={} id={}: {}",
                    item.getKeyType(), regex, item.getId(), e.getMessage());
        }
    }

    private static boolean isActive(IpListItem item, LocalDateTime now) {
        if (!item.isEnabled()) {
            return false;
        }
        if (item.getEffectiveAt() != null && item.getEffectiveAt().isAfter(now)) {
            return false;
        }
        return item.getExpiresAt() == null || !item.getExpiresAt().isBefore(now);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** 已编译条目总数（各维度集合 + CIDR + 正则之和）。 */
    public int size() {
        int total = 0;
        for (Set<String> set : blackExact.values()) {
            total += set.size();
        }
        for (Set<String> set : whiteExact.values()) {
            total += set.size();
        }
        total += blackCidrs.size() + whiteCidrs.size();
        total += blackUaPatterns.size() + whiteUaPatterns.size();
        total += blackRefererPatterns.size() + whiteRefererPatterns.size();
        return total;
    }

    /**
     * 当前已加载的条目数量明细，便于启动日志与运维排查。
     */
    public Map<String, Integer> describe() {
        Map<String, Integer> map = new LinkedHashMap<>();
        blackExact.forEach((k, v) -> map.put("black:" + k.name(), v.size()));
        whiteExact.forEach((k, v) -> map.put("white:" + k.name(), v.size()));
        map.put("black:CIDR", blackCidrs.size());
        map.put("white:CIDR", whiteCidrs.size());
        map.put("black:UA", blackUaPatterns.size());
        map.put("white:UA", whiteUaPatterns.size());
        map.put("black:REFERER", blackRefererPatterns.size());
        map.put("white:REFERER", whiteRefererPatterns.size());
        return map;
    }
}
