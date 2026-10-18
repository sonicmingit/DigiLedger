package com.digiledger.backend.auth;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.config.WebCorsConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;
import java.net.*;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real servlet cookies instead of injecting a MockHttpSession. */
class LoginHttpTest {
    @Configuration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class, MybatisAutoConfiguration.class})
    @Import({AuthWebConfiguration.class, LoginController.class, WebCorsConfiguration.class, ProtectedResource.class})
    static class AuthApplication { }

    @RestController
    static class ProtectedResource {
        @GetMapping("/api/test/private") ApiResponse<String> read() { return ApiResponse.success("private"); }
        @PostMapping("/api/test/private") ApiResponse<Void> write() { return ApiResponse.success(); }
    }

    @Test void realCookiesAuthenticateAndAreRevokedAfterLogout() throws Exception {
        try (var context = new SpringApplicationBuilder(AuthApplication.class)
                .run("--server.port=0", "--app.auth.password=test-password-12345", "--server.servlet.session.cookie.secure=false")) {
            int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
            URI root = URI.create("http://127.0.0.1:" + port);
            var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            var json = new ObjectMapper();
            var status = client.send(HttpRequest.newBuilder(root.resolve("/api/auth/status")).GET().build(), HttpResponse.BodyHandlers.ofString());
            String cookie = status.headers().firstValue("set-cookie").orElseThrow();
            assertTrue(cookie.startsWith("DL_SESSION="));
            assertTrue(cookie.contains("HttpOnly"));
            assertTrue(cookie.contains("SameSite=Lax"));
            assertTrue(cookie.contains("Max-Age=604800"));
            String csrf = json.readTree(status.body()).path("data").path("csrfToken").asText();
            var loggedIn = client.send(HttpRequest.newBuilder(root.resolve("/api/auth/login"))
                    .header("Content-Type", "application/json").header("X-CSRF-Token", csrf)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"test-password-12345\"}")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, loggedIn.statusCode());
            String authenticatedCookie = loggedIn.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
            assertNotEquals(cookie.split(";", 2)[0], authenticatedCookie);
            assertEquals(200, client.send(HttpRequest.newBuilder(root.resolve("/api/test/private")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(403, client.send(HttpRequest.newBuilder(root.resolve("/api/test/private")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            csrf = json.readTree(loggedIn.body()).path("data").path("csrfToken").asText();
            assertEquals(200, client.send(HttpRequest.newBuilder(root.resolve("/api/test/private")).header("X-CSRF-Token", csrf)
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200, client.send(HttpRequest.newBuilder(root.resolve("/api/auth/logout")).header("X-CSRF-Token", csrf)
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            var replay = HttpClient.newHttpClient().send(HttpRequest.newBuilder(root.resolve("/api/test/private"))
                    .header("Cookie", authenticatedCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(401, replay.statusCode());
        }
    }
}
