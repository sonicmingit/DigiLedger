import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const read = (path) => readFileSync(resolve(root, path), "utf8");
const pages = read("src/pages.json");
const api = read("src/services/api.ts");
const assetSearch = read("src/pages/assets/search.vue");
const assetEditor = read("src/pages/assets/editor.vue");
const assetDetail = read("src/pages/assets/detail.vue");
const wishlistIndex = read("src/pages/wishlist/index.vue");
const wishlistEditor = read("src/pages/wishlist/editor.vue");
const wishlistDetail = read("src/pages/wishlist/detail.vue");
const routeDetail = read("src/pages/routes/detail.vue");
const settings = read("src/pages/settings/index.vue");
const dictionary = read("src/pages/settings/dictionary.vue");
const uiSource = [
  assetSearch,
  assetEditor,
  assetDetail,
  wishlistIndex,
  wishlistEditor,
  wishlistDetail,
  routeDetail,
  settings,
  dictionary,
].join("\n");

/**
 * 该脚本验证本轮 H5 对齐中的结构性约束。视觉尺寸和真实触控仍需浏览器
 * 或真机验收，但这些断言可以防止路由、查询条件或只读边界在后续改动中回退。
 */
for (const path of [
  "pages/routes/index",
  "pages/routes/detail",
  "pages/settings/dictionary",
  "pages/wishlist/editor",
  "pages/wishlist/detail",
]) {
  assert.ok(pages.includes(path), `missing registered page: ${path}`);
}

for (const queryField of [
  "category_id",
  "brand_id",
  "platform_id",
  "tag_ids",
  "purchase-desc",
  "cost-desc",
]) {
  assert.ok(assetSearch.includes(queryField), `missing asset query: ${queryField}`);
}
assert.ok(wishlistIndex.includes("keyword"), "wishlist keyword query is missing");
assert.ok(wishlistIndex.includes("purchased"), "wishlist status query is missing");

for (const editor of [assetEditor, wishlistEditor]) {
  assert.ok(editor.includes('"拍照"'), "camera source is missing");
  assert.ok(editor.includes('"从相册选择"'), "album source is missing");
  assert.ok(editor.includes("sourceType: [source]"), "explicit image source is missing");
}
assert.ok(!uiSource.includes("外接服务搜图"), "external image search must stay out of H5");
assert.ok(!uiSource.includes("搜索图片"), "image search copy must stay out of H5");

assert.ok(api.includes('apiRequest<UpgradeRoute[]>("/upgrade-routes")'));
assert.ok(api.includes("upgradeRouteGraph"));
assert.ok(!api.includes('apiRequest<number>("/upgrade-routes"'), "H5 route writes are not allowed");
assert.ok(routeDetail.includes("generation"), "mobile route generation view is missing");

for (const action of [
  "createCategory",
  "updateCategory",
  "deleteCategory",
  "createBrand",
  "updateBrand",
  "deleteBrand",
  "createTag",
  "updateTag",
  "deleteTag",
]) {
  assert.ok(api.includes(action), `missing dictionary action: ${action}`);
}
assert.ok(dictionary.includes("editor-sheet"), "mobile dictionary editor is missing");
assert.ok(!settings.includes("偏好"), "preference settings must not appear on H5");

for (const detailCopy of [
  "购买记录",
  "出售记录",
  "相关链接",
  "价格历史",
]) {
  assert.ok(uiSource.includes(detailCopy), `missing detail query: ${detailCopy}`);
}
assert.ok(!uiSource.includes("资产"), 'user-facing copy must use "物品"');

console.log("H5 alignment structure verified.");
