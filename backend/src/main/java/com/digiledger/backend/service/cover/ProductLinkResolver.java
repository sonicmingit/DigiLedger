package com.digiledger.backend.service.cover;

import com.digiledger.backend.model.cover.ProductInfo;

import java.util.Optional;

/**
 * 商品链接解析接口，可按平台扩展不同实现。
 */
public interface ProductLinkResolver {

    /**
     * 当前解析器是否支持该链接。
     */
    boolean supports(String url);

    /**
     * 执行解析，返回商品信息。
     */
    Optional<ProductInfo> resolve(String url);

    /**
     * 来源标识，例如 PURCHASE_LINK_JD。
     */
    String getSource();

    /**
     * 来源展示名称，例如 京东。
     */
    String getDisplayName();
}
