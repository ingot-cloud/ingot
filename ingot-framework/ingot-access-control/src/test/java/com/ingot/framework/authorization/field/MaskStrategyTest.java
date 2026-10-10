package com.ingot.framework.authorization.field;

import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证参数化掩码、Unicode 边界以及短值与异常格式的关闭行为。</p>
 * @author jy
 * @since 1.0.0
 */
class MaskStrategyTest {
    private final MaskStrategy strategy = new DefaultMaskStrategy();

    @Test
    void presetsNeverRevealShortOrMalformedValues() {
        assertEquals("138****5678", strategy.mask("13812345678", MaskSpec.PHONE));
        assertEquals("***", strategy.mask("123", MaskSpec.PHONE));
        assertEquals("***", strategy.mask("abc12345678", MaskSpec.PHONE));
        assertEquals("a***@example.com", strategy.mask("alice@example.com", MaskSpec.EMAIL));
        assertEquals("***", strategy.mask("alice@@example.com", MaskSpec.EMAIL));
        assertEquals("***", strategy.mask("", MaskSpec.ALL));
        assertNull(strategy.mask(null, MaskSpec.ALL));
    }

    @Test
    void customRulesUseCodePointsAndFailClosedWhenNoCharactersWouldBeMasked() {
        var edges = new MaskSpec(MaskKind.KEEP_EDGES, 1, 1, null, null);
        assertEquals("😀**🚀", strategy.mask("😀中国🚀", edges));
        assertEquals("***", strategy.mask("😀🚀", edges));
        assertEquals("😀**🚀", strategy.mask("😀中国🚀", new MaskSpec(MaskKind.RANGE, null, null, 1, 3)));
        assertEquals("***", strategy.mask("abc", new MaskSpec(MaskKind.RANGE, null, null, 3, 4)));
        assertThrows(IllegalArgumentException.class, () -> new MaskSpec(MaskKind.RANGE, null, null, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> new MaskSpec(MaskKind.KEEP_EDGES, -1, 1, null, null));
        assertThrows(IllegalArgumentException.class, () -> new MaskSpec(MaskKind.PHONE, 1, null, null, null));
    }
}
