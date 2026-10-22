package com.agritrace.security;

import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;

/** 校验 Authorization: Bearer <token>，并按 @RequireRole 做角色控制 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod()) || !(handler instanceof HandlerMethod hm)) {
            return true;
        }
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return deny(resp, 401, "请先登录");
        }
        LoginUser user;
        try {
            user = jwtUtil.parse(header.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return deny(resp, 401, "登录已过期，请重新登录");
        }
        RequireRole rr = hm.getMethodAnnotation(RequireRole.class);
        if (rr == null) {
            rr = hm.getBeanType().getAnnotation(RequireRole.class);
        }
        if (rr != null && !Roles.ADMIN.equals(user.role()) && Arrays.stream(rr.value()).noneMatch(user.role()::equals)) {
            return deny(resp, 403, "没有权限执行该操作");
        }
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean deny(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, msg)));
        return false;
    }
}
