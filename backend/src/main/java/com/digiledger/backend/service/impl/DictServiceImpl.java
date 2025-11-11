package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.AssetTagMapMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.mapper.DictPlatformMapper;
import com.digiledger.backend.mapper.DictTagMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.mapper.SaleMapper;
import com.digiledger.backend.model.dto.dict.CategoryRequest;
import com.digiledger.backend.model.dto.dict.CategoryTreeNodeDTO;
import com.digiledger.backend.model.dto.dict.PlatformDTO;
import com.digiledger.backend.model.dto.dict.PlatformRequest;
import com.digiledger.backend.model.dto.dict.TagRequest;
import com.digiledger.backend.model.dto.dict.TagTreeNodeDTO;
import com.digiledger.backend.model.entity.DictCategory;
import com.digiledger.backend.model.entity.DictPlatform;
import com.digiledger.backend.model.entity.DictTag;
import com.digiledger.backend.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DictServiceImpl implements DictService {

    private final DictCategoryMapper dictCategoryMapper;
    private final DictPlatformMapper dictPlatformMapper;
    private final DictTagMapper dictTagMapper;
    private final AssetMapper assetMapper;
    private final PurchaseMapper purchaseMapper;
    private final SaleMapper saleMapper;
    private final AssetTagMapMapper assetTagMapMapper;

    public DictServiceImpl(DictCategoryMapper dictCategoryMapper,
                           DictPlatformMapper dictPlatformMapper,
                           DictTagMapper dictTagMapper,
                           AssetMapper assetMapper,
                           PurchaseMapper purchaseMapper,
                           SaleMapper saleMapper,
                           AssetTagMapMapper assetTagMapMapper) {
        this.dictCategoryMapper = dictCategoryMapper;
        this.dictPlatformMapper = dictPlatformMapper;
        this.dictTagMapper = dictTagMapper;
        this.assetMapper = assetMapper;
        this.purchaseMapper = purchaseMapper;
        this.saleMapper = saleMapper;
        this.assetTagMapMapper = assetTagMapMapper;
    }

    @Override
    public List<CategoryTreeNodeDTO> getCategoryTree() {
        List<DictCategory> categories = dictCategoryMapper.findAll();
        Map<Long, CategoryTreeNodeDTO> nodeMap = new LinkedHashMap<>();
        List<CategoryTreeNodeDTO> roots = new ArrayList<>();
        for (DictCategory category : categories) {
            CategoryTreeNodeDTO node = new CategoryTreeNodeDTO(
                    category.getId(),
                    category.getName(),
                    category.getParentId(),
                    category.getLevel(),
                    category.getSort()
            );
            nodeMap.put(category.getId(), node);
        }
        for (CategoryTreeNodeDTO node : nodeMap.values()) {
            if (node.getParentId() == null) {
                roots.add(node);
            } else {
                CategoryTreeNodeDTO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        sortCategoryNodes(roots);
        return roots;
    }

    @Override
    @Transactional
    public Long createCategory(CategoryRequest request) {
        DictCategory parent = null;
        if (request.getParentId() != null) {
            parent = Optional.ofNullable(dictCategoryMapper.findById(request.getParentId()))
                    .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "父级类别不存在"));
        }
        if (dictCategoryMapper.countByNameAndParent(request.getParentId(), request.getName(), null) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "同级类别名称已存在");
        }
        DictCategory category = new DictCategory();
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setSort(Optional.ofNullable(request.getSort()).orElse(0));
        category.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        dictCategoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional
    public void updateCategory(Long id, CategoryRequest request) {
        List<DictCategory> allCategories = dictCategoryMapper.findAll();
        Map<Long, DictCategory> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(DictCategory::getId, category -> category));
        DictCategory existing = Optional.ofNullable(categoryMap.get(id))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "类别不存在"));
        Long newParentId = request.getParentId();
        if (newParentId != null) {
            DictCategory parent = Optional.ofNullable(categoryMap.get(newParentId))
                    .orElseGet(() -> Optional.ofNullable(dictCategoryMapper.findById(newParentId))
                            .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "父级类别不存在")));
            categoryMap.put(parent.getId(), parent);
        }
        ensureNoCategoryCycle(categoryMap, id, newParentId);
        if (dictCategoryMapper.countByNameAndParent(newParentId, request.getName(), id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "同级类别名称已存在");
        }
        Long oldParentId = existing.getParentId();
        int oldLevel = existing.getLevel();
        DictCategory parent = newParentId == null ? null : categoryMap.get(newParentId);
        existing.setName(request.getName());
        existing.setParentId(newParentId);
        existing.setSort(Optional.ofNullable(request.getSort()).orElse(0));
        existing.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        dictCategoryMapper.update(existing);
        categoryMap.put(existing.getId(), existing);

        boolean hierarchyChanged = !Objects.equals(oldParentId, newParentId) || oldLevel != existing.getLevel();
        if (hierarchyChanged) {
            rebuildCategoryHierarchy(existing, categoryMap);
        } else {
            String categoryPath = buildCategoryPath(existing.getId(), categoryMap);
            assetMapper.updateCategoryPathForCategory(existing.getId(), categoryPath);
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        DictCategory category = Optional.ofNullable(dictCategoryMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "类别不存在"));
        if (dictCategoryMapper.countChildren(id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请先删除子类别");
        }
        String categoryLike = buildCategoryLikePattern(id, true);
        String categorySuffix = buildCategoryLikePattern(id, false);
        if (assetMapper.countByCategory(id, categoryLike, categorySuffix) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "存在关联物品，无法删除");
        }
        dictCategoryMapper.delete(id);
    }

    @Override
    public List<PlatformDTO> listPlatforms() {
        return dictPlatformMapper.findAll().stream()
                .map(platform -> new PlatformDTO(platform.getId(), platform.getName(), platform.getLink(), platform.getSort()))
                .toList();
    }

    @Override
    @Transactional
    public Long createPlatform(PlatformRequest request) {
        if (dictPlatformMapper.countByName(request.getName(), null) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "平台名称已存在");
        }
        DictPlatform platform = new DictPlatform();
        platform.setName(request.getName());
        platform.setLink(request.getLink());
        platform.setSort(Optional.ofNullable(request.getSort()).orElse(0));
        dictPlatformMapper.insert(platform);
        return platform.getId();
    }

    @Override
    @Transactional
    public void updatePlatform(Long id, PlatformRequest request) {
        DictPlatform existing = Optional.ofNullable(dictPlatformMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "平台不存在"));
        if (dictPlatformMapper.countByName(request.getName(), id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "平台名称已存在");
        }
        existing.setName(request.getName());
        existing.setLink(request.getLink());
        existing.setSort(Optional.ofNullable(request.getSort()).orElse(0));
        dictPlatformMapper.update(existing);
    }

    @Override
    @Transactional
    public void deletePlatform(Long id) {
        DictPlatform existing = Optional.ofNullable(dictPlatformMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "平台不存在"));
        if (purchaseMapper.countByPlatform(id) > 0 || saleMapper.countByPlatform(id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "平台已被使用，无法删除");
        }
        dictPlatformMapper.delete(existing.getId());
    }

    @Override
    public List<TagTreeNodeDTO> getTagTree() {
        List<DictTag> tags = dictTagMapper.findAll();
        Map<Long, TagTreeNodeDTO> nodeMap = new LinkedHashMap<>();
        List<TagTreeNodeDTO> roots = new ArrayList<>();
        for (DictTag tag : tags) {
            TagTreeNodeDTO node = new TagTreeNodeDTO(
                    tag.getId(),
                    tag.getName(),
                    tag.getParentId(),
                    tag.getColor(),
                    tag.getIcon(),
                    tag.getSort()
            );
            nodeMap.put(tag.getId(), node);
        }
        for (TagTreeNodeDTO node : nodeMap.values()) {
            if (node.getParentId() == null) {
                roots.add(node);
            } else {
                TagTreeNodeDTO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        sortTagNodes(roots);
        return roots;
    }

    @Override
    @Transactional
    public Long createTag(TagRequest request) {
        if (request.getParentId() != null) {
            Optional.ofNullable(dictTagMapper.findById(request.getParentId()))
                    .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "父级标签不存在"));
        }
        if (dictTagMapper.countByNameAndParent(request.getParentId(), request.getName(), null) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "同级标签名称已存在");
        }
        DictTag tag = new DictTag();
        tag.setName(request.getName());
        tag.setParentId(request.getParentId());
        tag.setColor(request.getColor());
        tag.setIcon(request.getIcon());
        tag.setSort(Optional.ofNullable(request.getSort()).orElse(0));
        dictTagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    @Transactional
    public void updateTag(Long id, TagRequest request) {
        DictTag existing = Optional.ofNullable(dictTagMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "标签不存在"));
        if (Objects.equals(id, request.getParentId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "父级标签不能为自身");
        }
        if (dictTagMapper.countByNameAndParent(request.getParentId(), request.getName(), id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "同级标签名称已存在");
        }
        if (request.getParentId() != null) {
            ensureNoTagCycle(id, request.getParentId());
        }
        existing.setName(request.getName());
        existing.setParentId(request.getParentId());
        existing.setColor(request.getColor());
        existing.setIcon(request.getIcon());
        existing.setSort(Optional.ofNullable(request.getSort()).orElse(0));
        dictTagMapper.update(existing);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        DictTag tag = Optional.ofNullable(dictTagMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "标签不存在"));
        if (dictTagMapper.countChildren(id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请先删除子标签");
        }
        if (assetTagMapMapper.countByTagId(id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "标签已关联物品，无法删除");
        }
        dictTagMapper.delete(tag.getId());
    }

    private void sortCategoryNodes(List<CategoryTreeNodeDTO> nodes) {
        nodes.sort(Comparator.comparing(CategoryTreeNodeDTO::getSort).thenComparing(CategoryTreeNodeDTO::getId));
        for (CategoryTreeNodeDTO node : nodes) {
            sortCategoryNodes(node.getChildren());
        }
    }

    private void sortTagNodes(List<TagTreeNodeDTO> nodes) {
        nodes.sort(Comparator.comparing(TagTreeNodeDTO::getSort).thenComparing(TagTreeNodeDTO::getId));
        for (TagTreeNodeDTO node : nodes) {
            sortTagNodes(node.getChildren());
        }
    }

    private void rebuildCategoryHierarchy(DictCategory root, Map<Long, DictCategory> categoryMap) {
        Map<Long, List<DictCategory>> childrenMap = new HashMap<>();
        for (DictCategory category : categoryMap.values()) {
            if (category.getParentId() != null) {
                childrenMap.computeIfAbsent(category.getParentId(), k -> new ArrayList<>()).add(category);
            }
        }
        Deque<DictCategory> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            DictCategory current = stack.pop();
            String categoryPath = buildCategoryPath(current.getId(), categoryMap);
            assetMapper.updateCategoryPathForCategory(current.getId(), categoryPath);
            List<DictCategory> children = childrenMap.getOrDefault(current.getId(), List.of());
            for (DictCategory child : children) {
                int newLevel = current.getLevel() + 1;
                if (!Objects.equals(child.getLevel(), newLevel)) {
                    child.setLevel(newLevel);
                    dictCategoryMapper.update(child);
                    categoryMap.put(child.getId(), child);
                }
                stack.push(child);
            }
        }
    }

    private void ensureNoTagCycle(Long tagId, Long newParentId) {
        Set<Long> visited = new HashSet<>();

        Long current = newParentId;
        while (current != null) {
            if (!visited.add(current)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "标签层级存在循环");
            }
            if (Objects.equals(current, tagId)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "父级标签不能为子节点");
            }
            DictTag parent = dictTagMapper.findById(current);
            if (parent == null) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "父级标签不存在");
            }
            current = parent.getParentId();
        }
    }

    private void ensureNoCategoryCycle(Map<Long, DictCategory> categoryMap, Long categoryId, Long newParentId) {
        if (newParentId == null) {
            return;
        }
        Set<Long> visited = new HashSet<>();


        Long current = newParentId;
        while (current != null) {
            if (!visited.add(current)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "类别层级存在循环");
            }
            if (Objects.equals(current, categoryId)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "父级类别不能为子节点");
            }
            DictCategory parent = categoryMap.get(current);
            if (parent == null) {
                parent = dictCategoryMapper.findById(current);
                if (parent == null) {
                    throw new BizException(ErrorCode.VALIDATION_ERROR, "父级类别不存在");
                }
                categoryMap.put(parent.getId(), parent);
            }
            current = parent.getParentId();
        }
    }

    private String buildCategoryPath(Long categoryId, Map<Long, DictCategory> categoryMap) {
        List<Long> path = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        DictCategory current = categoryMap.get(categoryId);
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "类别层级存在循环");
            }
            path.add(current.getId());
            Long parentId = current.getParentId();
            if (parentId == null) {
                break;
            }
            current = categoryMap.get(parentId);
            if (current == null) {
                current = Optional.ofNullable(dictCategoryMapper.findById(parentId))
                        .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "类别层级不完整"));
                categoryMap.put(current.getId(), current);
            }
        }
        Collections.reverse(path);
        return path.stream().map(String::valueOf).collect(Collectors.joining("/", "/", ""));
    }


    private String buildCategoryLikePattern(Long categoryId, boolean includeDescendants) {
        if (categoryId == null) {
            return null;
        }
        if (includeDescendants) {
            return "%/" + categoryId + "/%";
        }
        return "%/" + categoryId;
    }
}
