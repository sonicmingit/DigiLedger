package com.digiledger.backend.openapi;

import com.digiledger.backend.model.dto.asset.AssetPageDTO;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.DictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({OpenApiController.class, AgentResourcesController.class, OpenApiSettingsController.class})
class OpenApiControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private AssetService assets;
    @MockBean private DictService dictionaries;
    @MockBean private OpenAssetService edits;
    @MockBean private OpenApiAccessService access;
    @BeforeEach void configured() {
        when(access.authorize(null, false)).thenReturn(401);
        when(access.authorize("Bearer valid", false)).thenReturn(0);
        when(access.authorize("Bearer valid", true)).thenReturn(403);
    }
    @Test void missingTokenIsRejectedBeforeServiceCall() throws Exception {
        mvc.perform(get("/api/open/v1/assets")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        verifyNoInteractions(assets);
    }
    @Test void readOnlyCannotCreatePatchOrDelete() throws Exception {
        mvc.perform(post("/api/open/v1/assets").header("Authorization", "Bearer valid").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/open/v1/assets/42").header("Authorization", "Bearer valid").contentType("application/json").content("{\"notes\":\"new\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/open/v1/assets/42").header("Authorization", "Bearer valid")).andExpect(status().isForbidden());
        verifyNoInteractions(assets, edits);
    }
    @Test void listBindsFiltersAndPaginates() throws Exception {
        when(assets.pageAssets("使用中", "相机", 3L, null, null, List.of(1L, 2L), 2, 10, "purchaseDate", "desc"))
                .thenReturn(new AssetPageDTO(List.of(), 15, 2, 10));
        mvc.perform(get("/api/open/v1/assets").header("Authorization", "Bearer valid").param("keyword", "相机")
                .param("status", "使用中").param("category_id", "3").param("tag_ids", "1,2").param("page", "2").param("page_size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(15)).andExpect(jsonPath("$.data.page").value(2));
    }
    @Test void validatesPageSizeAndCreateBody() throws Exception {
        mvc.perform(get("/api/open/v1/assets").header("Authorization", "Bearer valid").param("page_size", "101"))
                .andExpect(jsonPath("$.code").value(400));
        when(access.authorize("Bearer valid", true)).thenReturn(0);
        mvc.perform(post("/api/open/v1/assets").header("Authorization", "Bearer valid").contentType("application/json").content("{\"name\":\"相机\"}"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(assets);
    }
    @Test void validCreationAndPartialEditReachServices() throws Exception {
        when(access.authorize("Bearer valid", true)).thenReturn(0);
        when(assets.createAsset(any())).thenReturn(42L);
        mvc.perform(post("/api/open/v1/assets").header("Authorization", "Bearer valid").contentType("application/json")
                .content("{\"name\":\"相机\",\"categoryId\":3,\"status\":\"使用中\"}"))
                .andExpect(jsonPath("$.data").value(42));
        mvc.perform(patch("/api/open/v1/assets/42").header("Authorization", "Bearer valid").contentType("application/json").content("{\"notes\":\"放在书房\"}"))
                .andExpect(jsonPath("$.code").value(200));
        verify(edits).patch(eq(42L), argThat(node -> "放在书房".equals(node.path("notes").asText())));
    }
    @Test void invalidJsonAndParameterTypesAreClientErrors() throws Exception {
        when(access.authorize("Bearer valid", true)).thenReturn(0);
        mvc.perform(patch("/api/open/v1/assets/42").header("Authorization", "Bearer valid").contentType("application/json").content("[]"))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/api/open/v1/assets").header("Authorization", "Bearer valid").param("page", "abc"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(assets, edits);
    }
    @Test void tokenIsOnlyReturnedOnRotationAndNotInSettings() throws Exception {
        OpenApiAccess config = new OpenApiAccess(); config.setTokenHash("SECRET_HASH"); config.setTokenPrefix("dl_prefix");
        when(access.get()).thenReturn(config); when(access.rotate()).thenReturn("dl_only-once");
        mvc.perform(get("/api/settings/open-api")).andExpect(jsonPath("$.data.tokenHash").doesNotExist())
                .andExpect(jsonPath("$.data.token").doesNotExist()).andExpect(jsonPath("$.data.tokenConfigured").value(true));
        mvc.perform(post("/api/settings/open-api/token")).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.token").value("dl_only-once"));
    }
    @Test void publicResourcesAreAccessibleWithoutToken() throws Exception {
        mvc.perform(get("/api/agent-resources/api.md")).andExpect(status().isOk()).andExpect(content().contentType("text/plain;charset=UTF-8"));
        mvc.perform(get("/api/agent-resources/openapi.json")).andExpect(status().isOk()).andExpect(jsonPath("$.servers[0].url").value("/api/open/v1"));
        mvc.perform(get("/api/agent-resources/digiledger-skill.zip")).andExpect(status().isOk()).andExpect(content().contentType("application/zip"));
    }
}
