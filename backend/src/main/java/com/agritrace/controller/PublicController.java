package com.agritrace.controller;

import com.agritrace.common.Result;
import com.agritrace.dto.BatchDetail;
import com.agritrace.service.TraceCodeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 不需要登录的接口：消费者扫码查询、健康检查 */
@RestController
@RequiredArgsConstructor
public class PublicController {
    private final TraceCodeService traceCodeService;

    @GetMapping("/api/public/trace/{code}")
    public Result<BatchDetail> trace(@PathVariable String code, HttpServletRequest req) {
        return Result.ok(traceCodeService.scan(code, clientIp(req), req.getHeader("User-Agent")));
    }

    @GetMapping("/api/health")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of("status", "UP"));
    }

    /** 经过 Nginx 转发时取真实 IP */
    private static String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = req.getHeader("X-Real-IP");
        return StringUtils.hasText(ip) ? ip : req.getRemoteAddr();
    }
}
