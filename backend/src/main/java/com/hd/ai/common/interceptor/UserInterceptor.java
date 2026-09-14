package com.hd.ai.common.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.UUID;

/**
 * 用户识别拦截器。
 * <p>
 * 在请求到达 Controller 之前从 Cookie 中读取 userId，
 * 若不存在则自动生成并写入 Cookie 返回前端。
 * 解析后的 userId 存入 {@link UserContext}，供业务层统一获取。
 * <h3>设计要点</h3>
 *
 * <ul>
 *   <li>Cookie 名：{@code userId}</li>
 *   <li>Cookie 有效期：365 天</li>
 *   <li>Cookie 路径：{@code /}</li>
 *   <li>userId 格式：32 位无连字符 UUID</li>
 *   <li>SameSite 策略：Lax（允许跨站导航携带，防止 CSRF）</li>
 *   <li>HttpOnly：false（允许前端 JS 读取）</li>
 * </ul>
 */
@Slf4j
@Component
public class UserInterceptor implements HandlerInterceptor {

    private static final String COOKIE_NAME = "userId";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        log.info("用户识别拦截器 - 请求进入: {} {}", request.getMethod(), request.getRequestURI());

        String userId = extractUserIdFromCookie(request);

        if (userId == null || userId.isBlank()) {
            userId = generateUserId();
            writeUserIdCookie(response, userId);
            log.info("用户识别拦截器 - 为新用户生成 userId: {}", userId);
        } else {
            log.info("用户识别拦截器 - 识别到已有用户 userId: {}", userId);
        }

        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        String userId = UserContext.getUserId();
        UserContext.clear();
        log.debug("用户识别拦截器 - 清理用户上下文 userId: {}", userId);
    }

    /**
     * 从请求 Cookie 中提取 userId
     */
    private String extractUserIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 生成 32 位无连字符 UUID
     */
    private String generateUserId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 将 userId 写入 Set-Cookie 响应头返回前端。
     * 使用 Spring {@link ResponseCookie} 确保跨 Servlet 容器兼容。
     */
    private void writeUserIdCookie(HttpServletResponse response, String userId) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, userId)
                .maxAge(COOKIE_MAX_AGE)
                .path("/")
                .httpOnly(false)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
