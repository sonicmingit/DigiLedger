package com.digiledger.backend.service;

import com.digiledger.backend.model.dto.dict.BrandDTO;
import com.digiledger.backend.model.dto.dict.BrandRequest;
import com.digiledger.backend.model.dto.dict.CategoryRequest;
import com.digiledger.backend.model.dto.dict.CategoryTreeNodeDTO;
import com.digiledger.backend.model.dto.dict.PlatformDTO;
import com.digiledger.backend.model.dto.dict.PlatformRequest;
import com.digiledger.backend.model.dto.dict.TagRequest;
import com.digiledger.backend.model.dto.dict.TagTreeNodeDTO;

import java.util.List;

public interface DictService {

    List<CategoryTreeNodeDTO> getCategoryTree();

    Long createCategory(CategoryRequest request);

    void updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    List<PlatformDTO> listPlatforms();

    Long createPlatform(PlatformRequest request);

    void updatePlatform(Long id, PlatformRequest request);

    void deletePlatform(Long id);

    List<TagTreeNodeDTO> getTagTree();

    Long createTag(TagRequest request);

    void updateTag(Long id, TagRequest request);

    void deleteTag(Long id);

    List<BrandDTO> listBrands();

    Long createBrand(BrandRequest request);

    void updateBrand(Long id, BrandRequest request);

    void deleteBrand(Long id);
}
