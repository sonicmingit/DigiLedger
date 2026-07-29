package com.digiledger.backend.integration.cover;

import com.digiledger.backend.integration.mtphotos.MtPhotosService;
import com.digiledger.backend.integration.mtphotos.MtPhotosThumbnail;
import com.digiledger.backend.service.impl.AssetCoverService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExternalImageImportServiceTest {

    private final MtPhotosService mtPhotosService = mock(MtPhotosService.class);
    private final AssetCoverService assetCoverService = mock(AssetCoverService.class);
    private final ExternalImageImportService service = new ExternalImageImportService(mtPhotosService, assetCoverService);

    @Test
    void importsMtPhotosProxyPathWithoutHttpLoopback() throws Exception {
        byte[] content = {1, 2, 3};
        when(mtPhotosService.getThumbnail(233912L)).thenReturn(new MtPhotosThumbnail(content, "image/jpeg"));

        MultipartFile file = service.download("/api/external-api-configs/MT_PHOTOS/thumbnail/233912");

        assertEquals("mt-photos-233912.jpg", file.getOriginalFilename());
        assertEquals("image/jpeg", file.getContentType());
        assertArrayEquals(content, file.getBytes());
        verify(mtPhotosService).getThumbnail(233912L);
        verifyNoInteractions(assetCoverService);
    }

    @Test
    void keepsUsingRemoteDownloaderForNormalImageUrls() {
        MultipartFile expected = mock(MultipartFile.class);
        String url = "https://images.example.com/cover.jpg";
        when(assetCoverService.downloadRemoteImage(url)).thenReturn(expected);

        assertEquals(expected, service.download(url));
        verify(assetCoverService).downloadRemoteImage(url);
        verifyNoInteractions(mtPhotosService);
    }
}
