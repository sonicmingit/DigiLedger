package com.digiledger.backend.integration.externalapi.persistence;

import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExternalApiConfigMapper {
    List<ExternalApiConfig> findAll();
    ExternalApiConfig findByCode(String apiCode);
    void insert(ExternalApiConfig config);
    void update(ExternalApiConfig config);
}
