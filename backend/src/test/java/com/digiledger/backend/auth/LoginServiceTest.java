package com.digiledger.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class LoginServiceTest {
    private LoginProperties properties(String password) {
        var properties = new LoginProperties(); properties.setPassword(password); return properties;
    }
    @Test void missingPasswordFailsClosedAndWeakPasswordIsRejected() {
        var service = new LoginService(properties(""));
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(LoginService.AUTHENTICATED, true);
        assertFalse(service.configured());
        assertFalse(service.authenticated(request));
        assertEquals(503, service.login("", request));
        assertThrows(IllegalArgumentException.class, () -> new LoginService(properties("short")));
    }
    @Test void throttlingCannotBeBypassedWithANewSessionAndExpiresAfterOneMinute() {
        var clock = new MutableClock();
        var service = new LoginService(properties("test-password-12345"), clock);
        for (int i = 0; i < 10; i++) assertEquals(401, service.login("incorrect", new MockHttpServletRequest()));
        assertEquals(429, service.login("test-password-12345", new MockHttpServletRequest()));
        clock.now += 60_001;
        var request = new MockHttpServletRequest();
        assertEquals(200, service.login("test-password-12345", request));
        assertTrue(service.authenticated(request));
        request.getSession().invalidate();
        assertFalse(service.authenticated(request));
    }
    private static class MutableClock extends Clock {
        long now = 1_000_000;
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(now); }
    }
}
