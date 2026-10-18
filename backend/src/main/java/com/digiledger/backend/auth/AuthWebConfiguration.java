package com.digiledger.backend.auth;

import com.digiledger.backend.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(LoginProperties.class)
public class AuthWebConfiguration implements WebMvcConfigurer {
    private final LoginProperties properties;
    private final ObjectMapper json;
    public AuthWebConfiguration(LoginProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
    }
    @Bean public LoginService loginService() { return new LoginService(properties); }

    @Override public void addInterceptors(InterceptorRegistry registry) {
        LoginService login = loginService();
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("X-Content-Type-Options", "nosniff");
                if ("OPTIONS".equals(request.getMethod())) return true;
                if (!login.authenticated(request)) return reject(response, 401, "请先登录");
                if (!Set.of("GET", "HEAD").contains(request.getMethod()) && !login.validCsrf(request)) {
                    return reject(response, 403, "登录校验已过期，请刷新页面后重试");
                }
                return true;
            }
        }).addPathPatterns("/**")
                .excludePathPatterns("/api/auth/status", "/api/auth/login", "/api/open/**", "/api/agent-resources/**");
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getWriter(), ApiResponse.failure(status, message));
        return false;
    }
}
