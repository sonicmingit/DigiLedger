package com.digiledger.backend.controller;

import com.digiledger.backend.model.dto.wishlist.WishlistPriceRequest;
import com.digiledger.backend.service.WishlistService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WishlistControllerTest {
    @Test void patchPriceKeepsUnifiedApiResponse() {
        WishlistService service = mock(WishlistService.class);
        WishlistController controller = new WishlistController(service);
        var request = new WishlistPriceRequest(new BigDecimal("7799"), null);
        var response = controller.updatePrice(7L, request);
        assertEquals(200, response.code());
        verify(service).updatePrice(7L, request);
    }
}
