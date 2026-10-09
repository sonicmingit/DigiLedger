package com.digiledger.backend.openapi;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.mapper.OpenApiAccessMapper;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OpenApiAccessServiceTest {
    @Test void disabledAccessAndReadOnlyPermissionsAreEnforced() {
        OpenApiAccessMapper mapper = mock(OpenApiAccessMapper.class);
        OpenApiAccess config = new OpenApiAccess();
        config.setTokenHash(OpenApiAccessService.hash("test-token"));
        when(mapper.get()).thenReturn(config);
        OpenApiAccessService service = new OpenApiAccessService(mapper);
        assertEquals(403, service.authorize("Bearer test-token", false));
        config.setEnabled(true);
        assertEquals(401, service.authorize(null, false));
        assertEquals(401, service.authorize("Basic test-token", false));
        assertEquals(401, service.authorize("Bearer incorrect", false));
        assertEquals(0, service.authorize("Bearer test-token", false));
        assertEquals(403, service.authorize("Bearer test-token", true));
        config.setAllowWrite(true);
        assertEquals(0, service.authorize("Bearer test-token", true));
    }

    @Test void rotatingTokenStoresOnlyHashAndImmediatelyRevokesOldToken() {
        OpenApiAccessMapper mapper = mock(OpenApiAccessMapper.class);
        OpenApiAccess config = new OpenApiAccess(); config.setEnabled(true);
        when(mapper.get()).thenReturn(config);
        AtomicReference<String> stored = new AtomicReference<>();
        doAnswer(call -> {
            stored.set(call.getArgument(0)); config.setTokenHash(call.getArgument(0));
            config.setTokenPrefix(call.getArgument(1)); return null;
        }).when(mapper).rotate(anyString(), anyString());
        OpenApiAccessService service = new OpenApiAccessService(mapper);
        String first = service.rotate();
        assertTrue(first.startsWith("dl_")); assertEquals(46, first.length());
        assertNotEquals(first, stored.get()); assertEquals(64, stored.get().length());
        assertEquals(0, service.authorize("Bearer " + first, false));
        String second = service.rotate();
        assertNotEquals(first, second);
        assertEquals(401, service.authorize("Bearer " + first, false));
        assertEquals(0, service.authorize("Bearer " + second, false));
    }

    @Test void cannotEnableUntilTokenIsGenerated() {
        OpenApiAccessMapper mapper = mock(OpenApiAccessMapper.class);
        when(mapper.get()).thenReturn(new OpenApiAccess());
        assertThrows(BizException.class, () -> new OpenApiAccessService(mapper).update(true, true));
        verify(mapper, never()).update(anyBoolean(), anyBoolean());
    }
}
