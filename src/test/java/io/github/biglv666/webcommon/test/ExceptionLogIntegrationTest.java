package io.github.biglv666.webcommon.test;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.biglv666.webcommon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 异常日志契约测试：0.4.0 在日志模板单点追加 traceId 时，
 * 一度把 varargs 末尾的 Throwable 摘除后未放回，导致系统异常日志静默丢失堆栈。
 * 本用例钉住「堆栈必须存在」，防止该缺陷回归。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExceptionLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 系统异常日志必须携带 Throwable 堆栈，且 traceId 字段已追加。
     */
    @Test
    void systemErrorLogCarriesStackTrace() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            mockMvc.perform(get("/demo/unknown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        } finally {
            logger.detachAppender(appender);
        }
        ILoggingEvent event = appender.list.stream()
                .filter(e -> e.getFormattedMessage().contains("未捕获系统异常"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未捕获系统异常日志未输出"));
        assertNotNull(event.getThrowableProxy(), "系统异常日志必须携带堆栈（Throwable 不得在 traceId 追加时丢失）");
        assertEquals("java.lang.IllegalStateException", event.getThrowableProxy().getClassName());
        assertEquals("-", event.getFormattedMessage().substring(
                event.getFormattedMessage().length() - 1));
    }
}
