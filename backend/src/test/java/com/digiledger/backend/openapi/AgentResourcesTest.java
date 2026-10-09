package com.digiledger.backend.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

class AgentResourcesTest {
    @Test void downloadableBundleContainsUsableSkillClientAndDocumentedContract() throws Exception {
        byte[] body = new AgentResourcesController().skill().getBody();
        assertNotNull(body);
        var files = new HashSet<String>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(body))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                files.add(entry.getName());
                String content = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                if (entry.getName().endsWith("SKILL.md")) assertTrue(content.startsWith("---\nname: digiledger"));
                if (entry.getName().endsWith("openapi.json")) {
                    var spec = new ObjectMapper().readTree(content);
                    assertEquals("bearer", spec.at("/components/securitySchemes/bearerAuth/scheme").asText());
                    assertTrue(spec.at("/components/schemas/AssetCreate/required").toString().contains("categoryId"));
                    var properties = spec.at("/components/schemas/AssetPatch/properties");
                    var fields = new HashSet<String>(); properties.fieldNames().forEachRemaining(fields::add);
                    assertEquals(OpenAssetService.FIELDS, fields);
                    assertFalse(properties.has("purchases"));
                }
            }
        }
        assertEquals(java.util.Set.of("digiledger/SKILL.md", "digiledger/scripts/digiledger.py", "digiledger/references/api.md", "digiledger/references/openapi.json"), files);
    }
}
