package com.digiledger.backend.auth;

import com.digiledger.backend.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginService login;
    public LoginController(LoginService login) { this.login = login; }
    public record Status(boolean configured, boolean authenticated, String csrfToken) { }
    public record PasswordRequest(String password) { }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Status>> status(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(ApiResponse.success(new Status(login.configured(), login.authenticated(request), login.csrfToken(request))));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody PasswordRequest body, HttpServletRequest request) {
        if (!login.validCsrf(request)) return failure(403, "登录校验已过期，请刷新页面后重试");
        int status = login.login(body.password(), request);
        if (status != 200) return failure(status, switch (status) {
            case 429 -> "尝试次数过多，请在一分钟后重试";
            case 503 -> "请先在服务器配置 DL_AUTH_PASSWORD 后重启服务";
            default -> "密码错误";
        });
        return status(request);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        request.getSession(false).invalidate();
        response.setHeader("Clear-Site-Data", "\"cache\"");
        return ApiResponse.success();
    }

    private ResponseEntity<?> failure(int status, String message) {
        var response = ResponseEntity.status(status).cacheControl(org.springframework.http.CacheControl.noStore());
        if (status == 429) response.header("Retry-After", "60");
        return response.body(ApiResponse.failure(status, message));
    }
}
