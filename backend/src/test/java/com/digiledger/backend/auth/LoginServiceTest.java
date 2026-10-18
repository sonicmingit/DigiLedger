package com.digiledger.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class LoginServiceTest {
    private LoginProperties properties(String password) {
        var properties = new LoginProperties(); properties.setPassword(password); return properties;
    }
    @Test void missingPasswordFailsClosed() {
        var service = new LoginService(properties(""));
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(LoginService.AUTHENTICATED, true);
        assertFalse(service.configured());
        assertFalse(service.authenticated(request));
        assertEquals(503, service.login("", request));
    }
    @Test void passwordsBelowAndAbovePreviousLengthLimitsCanAuthenticate() {
        for (String password : new String[]{"1", "123456", "x".repeat(257), "x".repeat(1024)}) {
            var service = new LoginService(properties(password));
            var request = new MockHttpServletRequest();
            assertTrue(service.configured());
            assertEquals(401, service.login(password + "wrong", request));
            assertFalse(service.authenticated(request));
            assertEquals(200, service.login(password, request));
            assertTrue(service.authenticated(request));
        }
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
