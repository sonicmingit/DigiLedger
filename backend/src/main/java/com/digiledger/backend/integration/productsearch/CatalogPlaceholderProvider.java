package com.digiledger.backend.integration.productsearch;

import com.digiledger.backend.integration.cover.ImageSearchProvider;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigService;
import com.digiledger.backend.model.cover.CoverCandidate;
import java.util.List;

/** Shared connector shell for signed product catalog APIs. Concrete clients are enabled only after credentials are configured. */
abstract class CatalogPlaceholderProvider implements ImageSearchProvider {
  final ExternalApiConfigService configs; CatalogPlaceholderProvider(ExternalApiConfigService configs){this.configs=configs;}
  public List<CoverCandidate> search(String query,int limit){ configs.requireConfiguredEnabled(getName()); return List.of(); }
}
