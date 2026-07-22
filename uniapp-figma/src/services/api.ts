import { apiRequest } from "./http";
export type Asset = {
  id: number;
  name: string;
  status?: string;
  categoryName?: string;
  brandName?: string;
  model?: string;
  coverImageUrl?: string;
  purchaseDate?: string;
  warrantyExpireDate?: string;
  notes?: string;
  totalCost?: number;
  currentValue?: number;
  dailyCost?: number;
  purchases?: unknown[];
  sales?: unknown[];
};
export type Wishlist = {
  id: number;
  name: string;
  priority?: number;
  expectedPrice?: number;
  currentPrice?: number;
  categoryName?: string;
  notes?: string;
  imageUrl?: string;
  status?: string;
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
  statusDistribution: unknown[];
  valueTrend: unknown[];
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
  deleteAsset: (id: number) =>
    apiRequest<void>(`/assets/${id}`, { method: "DELETE" }),
  sellAsset: (id: number, p: unknown) =>
    apiRequest<unknown>(`/assets/${id}/sell`, { method: "POST", data: p }),
  wishlist: () => apiRequest<Wishlist[]>("/wishlist"),
  wishlistDetail: (id: number) => apiRequest<Wishlist>(`/wishlist/${id}`),
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
  categories: () => apiRequest<any[]>("/dict/categories/tree"),
  brands: () => apiRequest<any[]>("/dict/brands"),
  tags: () => apiRequest<any[]>("/dict/tags/tree"),
  platforms: () => apiRequest<any[]>("/dict/platforms"),
  preferences: () => apiRequest<any>("/settings/preferences"),
  savePreferences: (p: unknown) =>
    apiRequest<void>("/settings/preferences", { method: "PUT", data: p }),
};
