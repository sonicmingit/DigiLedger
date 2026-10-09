package com.digiledger.backend.openapi;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.model.dto.asset.*;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.service.AssetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenAssetServiceTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final AssetService assets = mock(AssetService.class);
    private final AssetMapper mapper = mock(AssetMapper.class);

    private ObjectNode patch(String value) throws Exception { return (ObjectNode) json.readTree(value); }
    private OpenAssetService service() {
        return new OpenAssetService(assets, mapper, json, Validation.buildDefaultValidatorFactory().getValidator());
    }
    private void existing() throws Exception {
        DeviceAsset entity = new DeviceAsset();
        entity.setId(42L); entity.setName("相机"); entity.setCategoryId(3L); entity.setStatus("使用中");
        entity.setBrandId(2L); entity.setBrand("用户自定义品牌名"); entity.setSerialNo("ABC123");
        entity.setSpecifications("银色"); entity.setCoverImageUrl("uploads/camera.png"); entity.setNotes("旧备注");
        when(mapper.findByIdForUpdate(42L)).thenReturn(entity);
        when(assets.getAssetDetail(42L)).thenReturn(json.readValue("""
                {"id":42,"tags":[{"id":5,"name":"常用"}],
                 "relatedLinks":[{"url":"https://example.com/item","description":"详情"}]}
                """, AssetDetailDTO.class));
    }

    @Test void editingNotesPreservesOtherFieldsLinksTagsAndPurchases() throws Exception {
        existing(); service().patch(42L, patch("{\"notes\":\"放在书房\"}"));
        ArgumentCaptor<AssetCreateRequest> request = ArgumentCaptor.forClass(AssetCreateRequest.class);
        verify(assets).updateAsset(eq(42L), request.capture());
        var value = request.getValue();
        assertEquals("放在书房", value.getNotes()); assertEquals("相机", value.getName());
        assertEquals(3L, value.getCategoryId()); assertEquals("ABC123", value.getSerialNo());
        assertEquals("银色", value.getSpecifications()); assertEquals("用户自定义品牌名", value.getBrand());
        assertEquals("uploads/camera.png", value.getCoverImageUrl());
        assertEquals(java.util.List.of(5L), value.getTagIds()); assertEquals(1, value.getRelatedLinks().size());
        assertNull(value.getPurchases(), "null preserves all purchase records in AssetService");
    }
    @Test void explicitNullAndEmptyCollectionsClearOnlyRequestedFields() throws Exception {
        existing(); service().patch(42L, patch("{\"notes\":null,\"tagIds\":[],\"relatedLinks\":[]}"));
        ArgumentCaptor<AssetCreateRequest> request = ArgumentCaptor.forClass(AssetCreateRequest.class);
        verify(assets).updateAsset(eq(42L), request.capture());
        assertNull(request.getValue().getNotes()); assertTrue(request.getValue().getTagIds().isEmpty());
        assertTrue(request.getValue().getRelatedLinks().isEmpty()); assertEquals("相机", request.getValue().getName());
    }
    @Test void rejectsFinancialFieldsAndUnknownFieldsBeforeReading() throws Exception {
        assertThrows(BizException.class, () -> service().patch(42L, patch("{\"purchases\":[]}")));
        assertThrows(BizException.class, () -> service().patch(42L, patch("{\"totalInvest\":0}")));
        assertThrows(BizException.class, () -> service().patch(42L, patch("{}")));
        verifyNoInteractions(assets, mapper);
    }
    @Test void cannotClearRequiredName() throws Exception {
        existing(); assertThrows(ConstraintViolationException.class, () -> service().patch(42L, patch("{\"name\":null}")));
        verify(assets, never()).updateAsset(anyLong(), any());
    }
}
