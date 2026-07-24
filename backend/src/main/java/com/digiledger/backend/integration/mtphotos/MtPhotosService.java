package com.digiledger.backend.integration.mtphotos;

public interface MtPhotosService {
    MtPhotosSearchResponse testSearch(MtPhotosSearchRequest request);
    MtPhotosThumbnail getThumbnail(Long fileId);
}
