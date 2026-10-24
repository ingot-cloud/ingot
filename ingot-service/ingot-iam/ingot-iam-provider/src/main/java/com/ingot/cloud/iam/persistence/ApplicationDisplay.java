package com.ingot.cloud.iam.persistence;

/**
 * <p>保存开通列表需要的应用目录名称与图标，名称或图标任一有值即可。</p>
 *
 * @param name 目录名称，空白时为空
 * @param icon 目录图标，Iconify 名或对象路径，空白时为空
 * @author jy
 * @since 1.0.0
 */
public record ApplicationDisplay(String name, String icon) {
}
