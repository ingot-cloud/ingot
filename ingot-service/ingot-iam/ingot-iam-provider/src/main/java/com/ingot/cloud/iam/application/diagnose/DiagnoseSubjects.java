package com.ingot.cloud.iam.application.diagnose;

import java.util.List;

import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationOption;

/**
 * <p>只有诊断操作时提供本人的最小候选，不查询成员目录。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DiagnoseSubjects {
    private static final String SELF_LABEL = "本人";

    private DiagnoseSubjects() {
    }

    /**
     * 在搜索和回显条件下返回当前成员身份。
     * @param actor 当前可信成员
     * @param keyword 搜索词
     * @param selected 待回显 ID
     * @param page 当前页
     * @param size 页大小
     * @return 本人候选或空页
     */
    public static AuthorizationCandidatePage self(AuthorizationContext actor, String keyword, List<String> selected,
                                                  int page, int size) {
        boolean matches = (selected == null || selected.isEmpty() || selected.contains(actor.memberId()))
                && (keyword == null || keyword.isBlank() || SELF_LABEL.contains(keyword.strip()));
        var rows = matches && page == 1
                ? List.of(new AuthorizationOption(actor.memberId(), SELF_LABEL, null, null, null, null, null, null))
                : List.<AuthorizationOption>of();
        return new AuthorizationCandidatePage(rows, matches ? 1 : 0, page, size, true, null);
    }
}
