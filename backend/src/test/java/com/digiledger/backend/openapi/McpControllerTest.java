package com.digiledger.backend.openapi;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.OpenApiAccessMapper;
import com.digiledger.backend.model.dto.asset.AssetPageDTO;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.DictService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(McpController.class)
@Import({McpToolService.class, OpenApiAccessService.class})
class McpControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OpenApiAccessService access;
    @MockBean OpenApiAccessMapper mapper;
    @MockBean AssetService assets;
    @MockBean DictService dictionaries;
    @MockBean OpenAssetService edits;
    private OpenApiAccess settings;

    @BeforeEach void configure() {
        settings = new OpenApiAccess(); settings.setEnabled(true);
        settings.setTokenHash(OpenApiAccessService.hash("test-token"));
        when(mapper.get()).thenReturn(settings);
    }

    private ResultActions rpc(String body) throws Exception {
        return mvc.perform(post(McpController.PATH).header("Authorization", "Bearer test-token")
                .contentType("application/json").accept("application/json", "text/event-stream").content(body));
    }
    private ResultActions call(String tool, String args) throws Exception {
        return rpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool + "\",\"arguments\":" + args + "}}");
    }

    @Test void initializeNegotiatesVersionsAndDeclaresTools() throws Exception {
        for (String version : List.of("2025-03-26", "2025-06-18", "2025-11-25", "future")) {
            rpc(json.writeValueAsString(Map.of("jsonrpc", "2.0", "id", "init", "method", "initialize", "params",
                            Map.of("protocolVersion", version, "capabilities", Map.of(), "clientInfo", Map.of("name", "test", "version", "1")))))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.id").value("init"))
                    .andExpect(jsonPath("$.result.protocolVersion").value(version.equals("future") ? "2025-11-25" : version))
                    .andExpect(jsonPath("$.result.capabilities.tools").isMap())
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
    }

    @Test void sameTokenAndEnabledFlagProtectAllTransportMethods() throws Exception {
        mvc.perform(post(McpController.PATH).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error.code").value(-32000));
        mvc.perform(get(McpController.PATH).header("Authorization", "Bearer wrong")).andExpect(status().isUnauthorized());
        settings.setEnabled(false);
        rpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}").andExpect(status().isForbidden());
        verifyNoInteractions(assets, dictionaries, edits);
    }

    @Test void readOnlyCanDiscoverAndQueryButCannotExecuteHiddenWrites() throws Exception {
        rpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}")
                .andExpect(jsonPath("$.result.tools.length()").value(6))
                .andExpect(jsonPath("$.result.tools[0].annotations.readOnlyHint").value(true));
        when(assets.pageAssets(null, "camera", 3L, null, null, List.of(2L), 2, 10, "purchaseDate", "desc"))
                .thenReturn(new AssetPageDTO(List.of(), 15, 2, 10));
        String response = call("list_assets", "{\"keyword\":\"camera\",\"category_id\":3,\"tag_ids\":[2],\"page\":2,\"page_size\":10}")
                .andExpect(jsonPath("$.result.isError").value(false)).andReturn().getResponse().getContentAsString();
        JsonNode data = json.readTree(json.readTree(response).at("/result/content/0/text").asText());
        assertEquals(15, data.path("total").asInt());
        for (String name : List.of("create_asset", "update_asset", "delete_asset")) {
            call(name, "{}").andExpect(status().isOk()).andExpect(jsonPath("$.result.isError").value(true));
        }
        verify(assets, never()).createAsset(any()); verify(assets, never()).deleteAsset(any()); verifyNoInteractions(edits);
    }

    @Test void writeToolsReuseServicesAndPatchPreservesSubmittedFields() throws Exception {
        settings.setAllowWrite(true);
        rpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}").andExpect(jsonPath("$.result.tools.length()").value(9));
        when(assets.createAsset(any())).thenReturn(42L);
        call("create_asset", "{\"name\":\"camera\",\"categoryId\":3,\"status\":\"使用中\",\"purchases\":[{\"type\":\"PRIMARY\",\"price\":100,\"purchaseDate\":\"2026-10-09\"}]}")
                .andExpect(jsonPath("$.result.isError").value(false));
        verify(assets).createAsset(argThat(asset -> asset.getPurchases().size() == 1 && asset.getCategoryId() == 3));
        call("update_asset", "{\"id\":42,\"patch\":{\"notes\":null,\"tagIds\":[]}}")
                .andExpect(jsonPath("$.result.isError").value(false));
        verify(edits).patch(eq(42L), argThat(patch -> patch.size() == 2 && patch.get("notes").isNull() && patch.get("tagIds").isEmpty()));
        call("delete_asset", "{\"id\":42}").andExpect(jsonPath("$.result.isError").value(false));
        verify(assets).deleteAsset(42L);
    }

    @Test void validatesArgumentsBeforeAnyServiceCall() throws Exception {
        settings.setAllowWrite(true);
        for (String args : List.of("{\"page_size\":101}", "{\"page\":0}", "{\"page\":\"1\"}", "{\"page\":2147483648}",
                "{\"tag_ids\":[\"1\"]}", "{\"unexpected\":1}")) {
            call("list_assets", args).andExpect(jsonPath("$.result.isError").value(true));
        }
        call("create_asset", "{\"name\":\"camera\"}").andExpect(jsonPath("$.result.isError").value(true));
        call("create_asset", "{\"name\":\" \",\"categoryId\":1,\"status\":\"使用中\"}").andExpect(jsonPath("$.result.isError").value(true));
        call("update_asset", "{\"id\":1,\"patch\":{}}").andExpect(jsonPath("$.result.isError").value(true));
        call("update_asset", "{\"id\":1,\"patch\":{\"purchases\":[]}}").andExpect(jsonPath("$.result.isError").value(true));
        call("delete_asset", "{\"id\":9223372036854775808}").andExpect(jsonPath("$.result.isError").value(true));
        verifyNoInteractions(assets, dictionaries, edits);
    }

    @Test void businessErrorsRemainToolErrorsAndUnexpectedErrorsDoNotLeakSecrets() throws Exception {
        when(assets.getAssetDetail(1L)).thenThrow(new BizException(ErrorCode.ASSET_NOT_FOUND));
        call("get_asset", "{\"id\":1}").andExpect(jsonPath("$.result.isError").value(true));
        when(assets.getAssetDetail(2L)).thenThrow(new RuntimeException("SECRET database password"));
        String output = call("get_asset", "{\"id\":2}").andExpect(jsonPath("$.result.isError").value(true))
                .andReturn().getResponse().getContentAsString();
        assertFalse(output.contains("SECRET"));
    }

    @Test void malformedMessagesNotificationsAndUnsupportedMethodsAreHandled() throws Exception {
        rpc("{").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value(-32700));
        for (String body : List.of("[]", "null", "{}", "{\"jsonrpc\":\"2.0\",\"id\":null,\"method\":\"ping\"}")) {
            rpc(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value(-32600));
        }
        rpc("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}").andExpect(status().isAccepted()).andExpect(content().string(""));
        rpc("{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"delete_asset\",\"arguments\":{\"id\":1}}}")
                .andExpect(status().isBadRequest());
        rpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"unknown\"}").andExpect(jsonPath("$.error.code").value(-32601));
        call("unknown", "{}").andExpect(jsonPath("$.error.code").value(-32602));
        mvc.perform(get(McpController.PATH).header("Authorization", "Bearer test-token")).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(McpController.PATH).header("Authorization", "Bearer test-token")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(McpController.PATH).header("Authorization", "Bearer test-token").header("MCP-Protocol-Version", "invalid")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value(-32600));
        verifyNoInteractions(assets, dictionaries, edits);
    }

    @Test void untrustedOriginsAreRejectedAndConfiguredOriginsCanPreflight() throws Exception {
        mvc.perform(post(McpController.PATH).header("Origin", "https://untrusted.example").header("Authorization", "Bearer test-token")
                .contentType("application/json").content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(options(McpController.PATH).header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "authorization,mcp-protocol-version,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        verifyNoInteractions(assets, dictionaries, edits);
    }

    @Test void rotatingTokenImmediatelyRevokesOldMcpAccess() throws Exception {
        settings.setTokenHash(OpenApiAccessService.hash("new-token"));
        rpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}").andExpect(status().isUnauthorized());
    }
}
