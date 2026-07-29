import { apiRequest } from "./http";
export type Asset = {
  id: number;
  name: string;
  status?: string;
  categoryId?: number;
  categoryName?: string;
  categoryPath?: string;
  brandId?: number;
  brandName?: string;
  brand?: { id?: number; name?: string };
  model?: string;
  serialNo?: string;
  coverImageUrl?: string;
  purchaseDate?: string;
  warrantyExpireDate?: string;
  notes?: string;
  totalCost?: number;
  totalInvest?: number;
  currentValue?: number;
  dailyCost?: number;
  avgCostPerDay?: number;
  useDays?: number;
  lastNetIncome?: number;
  manualUseMonths?: number;
  tags?: DictionaryTag[];
  relatedLinks?: Array<{ url: string; description?: string }>;
  purchases?: PurchaseRecord[];
  sales?: SaleRecord[];
};
export type PurchaseRecord = {
  id?: number;
  type: "PRIMARY" | "ACCESSORY" | "SERVICE";
  name?: string;
  platformId?: number;
  platformName?: string;
  seller?: string;
  price: number;
  shippingCost?: number;
  quantity?: number;
  purchaseDate: string;
  warrantyMonths?: number;
  warrantyExpireDate?: string;
  productLink?: string;
  attachments?: string[];
  notes?: string;
};
export type SaleRecord = {
  id: number;
  saleScope: "ASSET" | "ACCESSORY";
  purchaseId?: number;
  platformName?: string;
  buyer?: string;
  salePrice: number;
  fee?: number;
  shippingCost?: number;
  otherCost?: number;
  netIncome?: number;
  saleDate: string;
  useDays?: number;
  lossAmount?: number;
  dailyUsageCost?: number;
  monthlyUsageCost?: number;
  attachments?: string[];
  notes?: string;
};
export type Wishlist = {
  id: number;
  name: string;
  priority?: number;
  expectedPrice?: number;
  currentPrice?: number;
  categoryId?: number;
  categoryName?: string;
  brandId?: number;
  brandName?: string;
  model?: string;
  source?: string;
  link?: string;
  notes?: string;
  imageUrl?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
  lastPriceAt?: string;
  purchasedAt?: string;
  purchasedPrice?: number;
  purchasePriceDiff?: number;
  convertedAssetId?: number;
  tags?: DictionaryTag[];
};
export type PricePoint = { price: number; capturedAt: string };
export type CategoryNode = {
  id: number;
  name: string;
  parentId?: number | null;
  level?: number;
  sort?: number;
  children?: CategoryNode[];
};
export type DictionaryBrand = {
  id: number;
  name: string;
  alias?: string;
  initial?: string;
  sort?: number;
};
export type DictionaryTag = {
  id: number;
  name: string;
  parentId?: number | null;
  color?: string;
  icon?: string;
  sort?: number;
  children?: DictionaryTag[];
};
export type DictionaryPlatform = {
  id: number;
  name: string;
  link?: string;
  sort?: number;
};
export type UpgradeRoute = {
  id: number;
  name: string;
  remark?: string | null;
  rootAssetName?: string | null;
  periodStart?: string | null;
  periodEnd?: string | null;
  coverImageUrls?: string[];
  status?: "ACTIVE" | "COMPLETED" | "ARCHIVED";
  routeType?: "ACTUAL" | "PLAN" | "MIXED";
  updatedAt?: string;
  actualSummary?: UpgradeActualSummary;
  planSummary?: UpgradePlanSummary;
};
export type UpgradeActualSummary = {
  assetCount?: number;
  totalSpend?: number;
  totalIncome?: number;
  netInvestment?: number;
  dailyCost?: number;
};
export type UpgradePlanSummary = {
  plannedBudget?: number;
  expectedRecovery?: number;
  expectedNetInvestment?: number;
};
export type UpgradeNode = {
  nodeId: number;
  assetId?: number;
  wishlistId?: number;
  name?: string;
  title?: string;
  targetName?: string;
  brandName?: string;
  model?: string;
  coverImageUrl?: string;
  status?: string;
  assetStatus?: string;
  nodeType?: "ASSET" | "WISHLIST" | "PLANNED";
  purchaseDate?: string;
  primaryPurchaseAmount?: number;
  totalInvest?: number;
  useDays?: number;
  level?: number;
  sort?: number;
  mainline?: boolean;
  alternativePurpose?: string;
  remark?: string | null;
};
export type UpgradeLink = {
  linkId: number;
  fromNodeId: number;
  toNodeId: number;
  relationType?: "SEQUENCE" | "ALTERNATIVE";
  purchaseGapDays?: number | null;
  purchasePriceDelta?: number | null;
  replacementNetOutflow?: number | null;
};
export type UpgradeGraph = {
  routeId: number;
  routeName: string;
  remark?: string | null;
  route?: UpgradeRoute;
  nodes: UpgradeNode[];
  links: UpgradeLink[];
  actualSummary?: UpgradeActualSummary;
  planSummary?: UpgradePlanSummary;
  warnings?: string[];
};
export type Dashboard = {
  totalAssetValue: number;
  assetCount: number;
  activeCount: number;
  idleCount: number;
  pendingSaleCount: number;
  avgDailyCost: number;
  monthValueChangeRate: number;
  monthCostChangeRate: number;
  categoryDistribution: Array<{
    categoryName: string;
    value: number;
    count: number;
  }>;
  statusDistribution: Array<{ status: string; count: number }>;
  valueTrend: Array<{ month: string; value: number }>;
  recentAssets: Asset[];
};
export type AssetPayload = {
  name: string;
  categoryId: number;
  brandId?: number;
  model?: string;
  status: string;
  purchaseDate?: string;
  coverImageUrl?: string;
  notes?: string;
  tagIds?: number[];
  targetCostValue?: number;
  targetCostStrategy?: string;
  purchases?: Array<{
    type: string;
    price: number;
    purchaseDate: string;
    quantity: number;
  }>;
};

const query = (params: Record<string, unknown>) => {
  const entries = Object.entries(params).filter(
    ([, v]) => v !== undefined && v !== "" && v !== null,
  );
  return entries.length
    ? "?" +
        entries
          .map(
            ([k, v]) =>
              `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`,
          )
          .join("&")
    : "";
};
export const api = {
  assets: (p: Record<string, unknown> = {}) =>
    apiRequest<Asset[]>(`/assets${query(p)}`),
  asset: (id: number) => apiRequest<Asset>(`/assets/${id}`),
  createAsset: (p: AssetPayload) =>
    apiRequest<number>("/assets", { method: "POST", data: p }),
  updateAsset: (id: number, p: AssetPayload) =>
    apiRequest<void>(`/assets/${id}`, { method: "PUT", data: p }),
  updateAssetStatus: (id: number, status: string) =>
    apiRequest<void>(`/assets/${id}/status`, {
      method: "PATCH",
      data: { status },
    }),
  deleteAsset: (id: number) =>
    apiRequest<void>(`/assets/${id}`, { method: "DELETE" }),
  sellAsset: (id: number, p: unknown) =>
    apiRequest<unknown>(`/assets/${id}/sell`, { method: "POST", data: p }),
  wishlist: () => apiRequest<Wishlist[]>("/wishlist"),
  wishlistDetail: (id: number) => apiRequest<Wishlist>(`/wishlist/${id}`),
  wishlistPriceHistory: (id: number) =>
    apiRequest<PricePoint[]>(`/wishlist/${id}/price-history`),
  createWishlist: (p: unknown) =>
    apiRequest<number>("/wishlist", { method: "POST", data: p }),
  updateWishlist: (id: number, p: unknown) =>
    apiRequest<void>(`/wishlist/${id}`, { method: "PUT", data: p }),
  deleteWishlist: (id: number) =>
    apiRequest<void>(`/wishlist/${id}`, { method: "DELETE" }),
  markPurchased: (id: number) =>
    apiRequest<void>(`/wishlist/${id}/mark-purchased`, { method: "POST" }),
  convertWishlist: (id: number, p: AssetPayload) =>
    apiRequest<number>(`/wishlist/${id}/convert`, { method: "POST", data: p }),
  dashboard: () => apiRequest<Dashboard>("/dashboard/summary"),
  categories: () => apiRequest<CategoryNode[]>("/dict/categories/tree"),
  createCategory: (p: Partial<CategoryNode>) =>
    apiRequest<number>("/dict/categories", { method: "POST", data: p }),
  updateCategory: (id: number, p: Partial<CategoryNode>) =>
    apiRequest<void>(`/dict/categories/${id}`, { method: "PUT", data: p }),
  deleteCategory: (id: number) =>
    apiRequest<void>(`/dict/categories/${id}`, { method: "DELETE" }),
  brands: () => apiRequest<DictionaryBrand[]>("/dict/brands"),
  createBrand: (p: Partial<DictionaryBrand>) =>
    apiRequest<number>("/dict/brands", { method: "POST", data: p }),
  updateBrand: (id: number, p: Partial<DictionaryBrand>) =>
    apiRequest<void>(`/dict/brands/${id}`, { method: "PUT", data: p }),
  deleteBrand: (id: number) =>
    apiRequest<void>(`/dict/brands/${id}`, { method: "DELETE" }),
  tags: () => apiRequest<DictionaryTag[]>("/dict/tags/tree"),
  createTag: (p: Partial<DictionaryTag>) =>
    apiRequest<number>("/dict/tags", { method: "POST", data: p }),
  updateTag: (id: number, p: Partial<DictionaryTag>) =>
    apiRequest<void>(`/dict/tags/${id}`, { method: "PUT", data: p }),
  deleteTag: (id: number) =>
    apiRequest<void>(`/dict/tags/${id}`, { method: "DELETE" }),
  platforms: () => apiRequest<DictionaryPlatform[]>("/dict/platforms"),
  upgradeRoutes: () => apiRequest<UpgradeRoute[]>("/upgrade-routes"),
  upgradeRouteGraph: (id: number) =>
    apiRequest<UpgradeGraph>(`/upgrade-routes/${id}/graph`),
};
