package com.ingot.framework.commons.model.iam.extension;

import java.util.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.ingot.framework.commons.model.iam.*;

/**
 * <p>后端注册的精确操作及读写类别。</p>
 *
 * @param code 精确操作编码
 * @param mode 服务器声明的执行模式
 * @author jy
 * @since 1.0.0
 */
public record ActionDescriptor(@NotBlank String code, @NotNull ExecutionMode mode) {

}
