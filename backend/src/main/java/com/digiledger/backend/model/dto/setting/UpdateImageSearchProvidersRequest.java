package com.digiledger.backend.model.dto.setting;

import java.util.List;

/** 更新物品封面搜图弹窗允许使用的服务。 */
public record UpdateImageSearchProvidersRequest(List<String> providers) {
}
