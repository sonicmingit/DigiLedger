package com.digiledger.backend.service.cover;

import com.digiledger.backend.model.cover.CoverCandidate;

import java.util.List;

/**
 * 图片搜索提供方抽象，便于后续扩展 Google 等能力。
 */
public interface ImageSearchProvider {

    /**
     * 提供方名称，例如 BING_IMAGE_SEARCH。
     */
    String getName();

    /** ����չʾ���ƣ�Ĭ�Ϸ��� getName */
    default String getDisplayName() {
        return getName();
    }

    /** �����û����ʾ������ѡ */
    default String getDescription() {
        return null;
    }

    /**
     * 按关键字搜索封面候选。
     */
    List<CoverCandidate> search(String query, int limit);
}
