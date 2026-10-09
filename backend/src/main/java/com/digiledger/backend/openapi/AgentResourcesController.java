package com.digiledger.backend.openapi;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Public, credential-free documentation and portable skill distribution. */
@RestController
@RequestMapping("/api/agent-resources")
public class AgentResourcesController {
    private static final String ROOT = "agent-skill/digiledger/";
    private static final String[] FILES = {"SKILL.md", "scripts/digiledger.py", "references/api.md", "references/openapi.json"};

    @GetMapping("/api.md") public ResponseEntity<byte[]> documentation() throws IOException {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(read("references/api.md"));
    }
    @GetMapping("/openapi.json") public ResponseEntity<byte[]> openapi() throws IOException {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(read("references/openapi.json"));
    }
    @GetMapping("/digiledger-skill.zip") public ResponseEntity<byte[]> skill() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String file : FILES) {
                zip.putNextEntry(new ZipEntry("digiledger/" + file));
                zip.write(read(file)); zip.closeEntry();
            }
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=digiledger-skill.zip").body(bytes.toByteArray());
    }
    private byte[] read(String file) throws IOException {
        try (var input = new ClassPathResource(ROOT + file).getInputStream()) { return input.readAllBytes(); }
    }
}
