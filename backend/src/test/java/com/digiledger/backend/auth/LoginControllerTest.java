package com.digiledger.backend.auth;

import com.digiledger.backend.openapi.OpenApiAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = LoginController.class, properties = "app.auth.password=test-password-12345")
class LoginControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean OpenApiAccessService openAccess;

    private MvcResult initialStatus() throws Exception {
        return mvc.perform(get("/api/auth/status")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.authenticated").value(false)).andReturn();
    }
    private String csrf(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).path("data").path("csrfToken").asText();
    }
    private MvcResult signIn(MvcResult initial) throws Exception {
        return mvc.perform(post("/api/auth/login").session((MockHttpSession) initial.getRequest().getSession())
                .header("X-CSRF-Token", csrf(initial)).contentType("application/json")
                .content("{\"password\":\"test-password-12345\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.authenticated").value(true)).andReturn();
    }

    @Test void privateApisFilesAndSettingsAreBlockedWithoutSession() throws Exception {
        for (String path : new String[]{"/api/assets", "/api/dashboard/summary", "/api/settings/open-api", "/oss/digiledger/private.jpg", "/api/unknown"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        }
        mvc.perform(post("/api/files/upload")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/settings/open-api/token")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/assets;ignored=1")).andExpect(status().isUnauthorized());
    }

    @Test void wrongPasswordAndForgedRequestsCannotLogin() throws Exception {
        var initial = initialStatus();
        var session = (MockHttpSession) initial.getRequest().getSession();
        mvc.perform(post("/api/auth/login").session(session).contentType("application/json")
                .content("{\"password\":\"test-password-12345\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-Token", csrf(initial))
                .contentType("application/json").content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.msg").value("密码错误"));
        mvc.perform(get("/api/assets").session(session)).andExpect(status().isUnauthorized());
    }

    @Test void loginRotatesSessionAndCsrfThenLogoutRevokesAccess() throws Exception {
        var initial = initialStatus();
        var oldSession = (MockHttpSession) initial.getRequest().getSession();
        var signedIn = signIn(initial);
        var session = (MockHttpSession) signedIn.getRequest().getSession();
        assertTrue(oldSession.isInvalid());
        assertNotEquals(oldSession.getId(), session.getId());
        assertNotEquals(csrf(initial), csrf(signedIn));
        mvc.perform(get("/api/auth/status").session(session)).andExpect(jsonPath("$.data.authenticated").value(true));
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-Token", csrf(initial)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-Token", csrf(signedIn)))
                .andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/settings/open-api")).andExpect(status().isUnauthorized());
    }

    @Test void sessionIdsInUrlsDoNotAuthenticate() throws Exception {
        mvc.perform(get("/api/assets;jsessionid=forged")).andExpect(status().isUnauthorized());
    }
}
