package com.digiledger.backend.openapi;

import com.digiledger.backend.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Set;

@Configuration
public class OpenApiWebConfiguration implements WebMvcConfigurer {
    private final OpenApiAccessService access;
    private final ObjectMapper json;
    public OpenApiWebConfiguration(OpenApiAccessService access, ObjectMapper json) {
        this.access = access; this.json = json;
    }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                if ("OPTIONS".equals(request.getMethod())) return true;
                boolean write = !Set.of("GET", "HEAD").contains(request.getMethod());
                int status = access.authorize(request.getHeader("Authorization"), write);
                if (status == 0) return true;
                response.setStatus(status);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Cache-Control", "no-store");
                if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
                json.writeValue(response.getWriter(), ApiResponse.failure(status,
                        status == 401 ? "访问 Token 无效或缺失" : "开放 API 未启用或 Token 仅有只读权限"));
                return false;
            }
        }).addPathPatterns("/api/open/**");
    }
}
