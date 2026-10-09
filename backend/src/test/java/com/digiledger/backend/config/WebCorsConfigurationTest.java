package com.digiledger.backend.config;

import com.digiledger.backend.controller.DashboardController;
import com.digiledger.backend.openapi.OpenApiAccessService;
import com.digiledger.backend.service.DashboardService;
import com.digiledger.backend.service.impl.DashboardSpendingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;

@WebMvcTest(DashboardController.class)
@Import(WebCorsConfiguration.class)
class WebCorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private DashboardSpendingService dashboardSpendingService;

    @MockBean
    private OpenApiAccessService openApiAccessService;

    @Test
    void allowsLocalH5PreflightRequest() throws Exception {
        mockMvc.perform(options("/api/dashboard/summary")
                        .header(ORIGIN, "http://localhost:5173")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS"));
    }

    @Test
    void allowsCapacitorAndroidOrigin() throws Exception {
        mockMvc.perform(options("/api/dashboard/summary")
                        .header(ORIGIN, "https://localhost")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "https://localhost"));
    }

    @Test
    void allowsLanH5PreflightRequest() throws Exception {
        mockMvc.perform(options("/api/dashboard/summary")
                        .header(ORIGIN, "http://192.168.1.20:5173")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://192.168.1.20:5173"));
    }

    @Test
    void rejectsUnlistedPublicOrigin() throws Exception {
        mockMvc.perform(options("/api/dashboard/summary")
                        .header(ORIGIN, "https://untrusted.example")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bindsSpendingQueryWithoutCompilerParameterMetadata() throws Exception {
        mockMvc.perform(get("/api/dashboard/spending")
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-01-31")
                        .param("categoryId", "3")
                        .param("type", "PRIMARY")
                        .param("platformId", "5")
                        .param("q", "手机")
                        .param("page", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
        verify(dashboardSpendingService).getSpending(java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 1, 31), 3L, "PRIMARY", 5L, "手机", 2, 10);
    }
}
