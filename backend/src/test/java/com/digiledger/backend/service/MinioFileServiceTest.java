package com.digiledger.backend.service;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.config.StorageProperties;
import com.digiledger.backend.config.UploadProperties;
import com.digiledger.backend.service.impl.MinioFileService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioFileServiceTest {
    private final MinioClient client = mock(MinioClient.class);
    private final UploadProperties upload = new UploadProperties();
    private final MinioFileService service;

    MinioFileServiceTest() {
        upload.setMaxSizeMb(5);
        upload.setAllowedContentTypes(List.of("application/pdf", "image/png"));
        service = new MinioFileService(client, new StorageProperties(), upload);
    }

    @Test
    void acceptsDocumentLargerThanOneMegabyteWithinFiveMegabytes() throws Exception {
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        String key = service.upload(new MockMultipartFile("file", "invoice.pdf", "application/pdf", new byte[2 * 1024 * 1024]));
        assertTrue(key.endsWith(".pdf"));
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void rejectsDocumentOverFiveMegabytes() {
        BizException error = assertThrows(BizException.class, () ->
                service.upload(new MockMultipartFile("file", "invoice.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1])));
        assertEquals(ErrorCode.FILE_TOO_LARGE, error.getErrorCode());
    }
}
