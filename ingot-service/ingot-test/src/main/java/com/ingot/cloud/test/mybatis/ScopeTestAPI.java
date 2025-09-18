package com.ingot.cloud.test.mybatis;

import java.util.List;

import com.ingot.cloud.test.model.domain.TStudent;
import com.ingot.cloud.test.service.domain.TStudentService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.data.mybatis.scope.config.DataScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : ScopeTestAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/2.</p>
 * <p>Time         : 09:59.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/mybatis/scope")
@RequiredArgsConstructor
public class ScopeTestAPI implements RShortcuts {
    private final TStudentService studentService;

    @GetMapping
    @DataScope
    public R<?> testScope() {
        return ok(students());
    }

    public List<TStudent> students() {
        return studentService.list();
    }

}
