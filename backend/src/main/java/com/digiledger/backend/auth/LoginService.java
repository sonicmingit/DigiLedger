package com.digiledger.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;

/** Single-owner authentication. Neither passwords nor session IDs are returned to clients. */
public class LoginService {
    public static final String AUTHENTICATED = LoginService.class.getName() + ".authenticated";
    public static final String CSRF = LoginService.class.getName() + ".csrf";
    private static final int MAX_FAILURES = 10;
    private static final long WINDOW_MILLIS = 60_000;
    private final SecureRandom random = new SecureRandom();
    private final byte[] salt = new byte[16];
    private final byte[] passwordHash;
    private final Clock clock;
    // Global limit intentionally ignores spoofable forwarding headers and bounds memory.
    private final Deque<Long> failures = new ArrayDeque<>();

    public LoginService(LoginProperties properties) { this(properties, Clock.systemUTC()); }

    LoginService(LoginProperties properties, Clock clock) {
        this.clock = clock;
        random.nextBytes(salt);
        String password = properties.getPassword();
        passwordHash = password == null || password.isEmpty() ? null : hash(password);
    }

    public boolean configured() { return passwordHash != null; }

    public boolean authenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return configured() && session != null && Boolean.TRUE.equals(session.getAttribute(AUTHENTICATED));
    }

    public String csrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(CSRF);
        if (token == null) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF, token);
        }
        return token;
    }

    public boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String supplied = request.getHeader("X-CSRF-Token");
        String expected = session == null ? null : (String) session.getAttribute(CSRF);
        return supplied != null && expected != null && MessageDigest.isEqual(
                supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Returns an HTTP status; synchronized so concurrent attempts cannot bypass the limit. */
    public synchronized int login(String password, HttpServletRequest request) {
        if (!configured()) return 503;
        long now = clock.millis();
        while (!failures.isEmpty() && failures.peekFirst() <= now - WINDOW_MILLIS) failures.removeFirst();
        if (failures.size() >= MAX_FAILURES) return 429;
        if (password == null || !MessageDigest.isEqual(passwordHash, hash(password))) {
            failures.addLast(now);
            return 401;
        }
        // Rotate both the session ID and CSRF token after authentication.
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(AUTHENTICATED, true);
        csrfToken(request);
        failures.clear();
        return 200;
    }

    private byte[] hash(String password) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("无法初始化密码校验", ex);
        } finally { spec.clearPassword(); }
    }
}
