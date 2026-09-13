package dev.storyblock.api.http;

import java.util.Map;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public final class ReadResponseRecorder implements ResponseBodyAdvice<Object> {
    private final ReadBodyRecorder recorder;
    public ReadResponseRecorder(RecentReadLedger ledger) { this.recorder = new ReadBodyRecorder(ledger); }

    public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        Class<?> type = method.getContainingClass();
        if (type == NarrativeReadController.class || type == RenderController.class) return true;
        if (type == MonitorController.class) return method.getMethod().getName().equals("packet");
        return (type == NovelController.class || type == AdminNovelController.class)
                && method.hasMethodAnnotation(GetMapping.class);
    }

    public Object beforeBodyWrite(Object body, MethodParameter method, MediaType media,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof Map<?, ?> map) recorder.record(map);
        return body;
    }

}
