package com.ingot.framework.sentinel.webmvc.callback;

import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ingot.framework.core.model.support.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;

/**
 * <p>Description  : 自定义{@link BlockException}异常处理.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 10:40 上午.</p>
 */
@Slf4j
public class InBlockExceptionHandler implements BlockExceptionHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       String resourceName,
                       BlockException e) throws Exception {
        log.error("sentinel 限流资源名称：{}", resourceName, e);

        response.setContentType(ContentType.JSON.toString());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        PrintWriter out = response.getWriter();
        out.print(JSONUtil.toJsonStr(R.error500(e.getMessage())));
        out.flush();
        out.close();
    }
}
