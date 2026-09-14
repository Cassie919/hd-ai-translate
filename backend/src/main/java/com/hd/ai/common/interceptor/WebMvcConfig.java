package com.hd.ai.common.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。
 * <p>
 * 负责注册拦截器链、CORS 全局配置等 Web 层基础设施。
 * 当前注册 {@link UserInterceptor} 实现无登录用户识别。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserInterceptor userInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/static/**",
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                );
    }
}
