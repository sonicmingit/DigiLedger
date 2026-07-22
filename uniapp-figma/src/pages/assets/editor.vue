<template>
  <view class="page editor"
    ><view class="top"
      ><view class="back touch" @click="back"><view class="back-icon" /></view
      ><text class="title">{{ id ? "编辑物品" : "新增物品" }}</text
      ><view class="save touch" @click="submit">保存</view></view
    ><view class="cover" @click="chooseImage"
      ><image
        v-if="form.coverImageUrl"
        :src="form.coverImageUrl"
        mode="aspectFill"
      /><template v-else
        ><image class="image-icon" src="/static/icons/image.svg" /><text
          class="cover-title"
          >添加封面图</text
        ><text class="hint">支持 JPG / PNG，建议 1:1</text></template
      ></view
    ><view class="field"
      ><text>物品名称 *</text
      ><input v-model="form.name" placeholder="请输入物品名称" /></view
    ><view class="field"
      ><text>分类 *</text
      ><picker :range="categories" range-key="name" @change="pickCategory"
        ><view class="picker">{{ categoryName || "请选择分类" }}</view></picker
      ></view
    ><view class="field"
      ><text>购买日期</text
      ><input
        v-model="form.purchaseDate"
        maxlength="10"
        placeholder="YYYY-MM-DD" /></view
    ><view class="field"
      ><text>购买金额</text
      ><input v-model="cost" type="digit" placeholder="0.00" /></view
    ><view class="field"
      ><text>当前状态</text
      ><view class="states"
        ><view
          v-for="s in states"
          :key="s"
          class="pill"
          :class="{ active: form.status === s }"
          @click="form.status = s"
          >{{ s }}</view
        ></view
      ></view
    ><button
      class="primary submit"
      :loading="saving"
      :disabled="saving"
      @click="submit"
    >
      保存物品</button
    ><text class="footnote"
      >必填字段用于统计与筛选，其余信息可稍后补充</text
    ></view
  >
</template>
<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { api, type AssetPayload } from "@/services/api";
import { uploadFile } from "@/services/http";
const id = ref(0),
  saving = ref(false),
  cost = ref(""),
  categories = ref<any[]>([]),
  states = ["使用中", "已闲置", "待出售"];
const today = new Date().toISOString().slice(0, 10);
const form = reactive<AssetPayload>({
  name: "",
  categoryId: 0,
  status: "使用中",
  purchaseDate: today,
  coverImageUrl: "",
  model: "",
  notes: "",
});
const categoryName = computed(
  () => categories.value.find((x) => x.id === form.categoryId)?.name || "",
);
function pickCategory(e: any) {
  form.categoryId = categories.value[Number(e.detail.value)]?.id || 0;
}
function back() {
  uni.navigateBack();
}
async function chooseImage() {
  const r = await uni.chooseImage({ count: 1, sizeType: ["compressed"] });
  uni.showLoading({ title: "上传中" });
  try {
    const out = await uploadFile(r.tempFilePaths[0]);
    form.coverImageUrl = out.url;
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    uni.hideLoading();
  }
}
async function submit() {
  if (!form.name.trim() || !form.categoryId)
    return uni.showToast({ title: "请填写名称并选择分类", icon: "none" });
  saving.value = true;
  const amount = Number(cost.value || 0),
    payload = {
      ...form,
      targetCostValue: amount,
      targetCostStrategy: "CUSTOM",
      purchases: amount
        ? [
            {
              type: "PRIMARY",
              price: amount,
              purchaseDate: form.purchaseDate || today,
              quantity: 1,
            },
          ]
        : undefined,
    };
  try {
    id.value
      ? await api.updateAsset(id.value, payload)
      : await api.createAsset(payload);
    uni.showToast({ title: "保存成功", icon: "success" });
    setTimeout(back, 500);
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}
onLoad(async (q) => {
  id.value = Number(q?.id || 0);
  categories.value = await api.categories().catch(() => []);
  if (id.value) {
    const a = await api.asset(id.value);
    Object.assign(form, a, { categoryId: (a as any).categoryId || 0 });
    cost.value = String(a.totalCost || "");
  }
});
</script>
<style scoped>
.editor {
  padding-bottom: 42px;
}
.top {
  height: 48px;
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.back {
  width: 30px;
  justify-content: flex-start;
}
.back-icon {
  width: 10px;
  height: 10px;
  border-left: 2px solid var(--dl-text);
  border-bottom: 2px solid var(--dl-text);
  transform: rotate(45deg);
}
.title {
  font-size: 22px;
  font-weight: 700;
  margin-left: 10px;
}
.save {
  margin-left: auto;
  color: var(--dl-text-secondary);
  font-size: 13px;
}
.cover {
  height: 130px;
  border-radius: 28px;
  background: var(--dl-accent-soft);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.cover > image:not(.image-icon) {
  width: 100%;
  height: 100%;
}
.image-icon {
  width: 24px;
  height: 24px;
}
.cover-title {
  font-weight: 700;
  margin-top: 8px;
}
.hint {
  font-size: 11px;
  color: var(--dl-text-secondary);
  margin-top: 5px;
}
.field {
  margin-top: 18px;
}
.field > text {
  display: block;
  font-size: 13px;
  color: var(--dl-text-secondary);
  margin-bottom: 7px;
}
.field input,
.picker,
.field textarea {
  width: 100%;
  min-height: 48px;
  padding: 0 16px;
  border-radius: 12px;
  background: #fff;
  font-size: 14px;
}
.picker {
  display: flex;
  align-items: center;
}
.field textarea {
  height: 88px;
  padding-top: 14px;
}
.states {
  display: flex;
  gap: 10px;
}
.states .pill {
  flex: 1;
  min-height: 38px;
}
.submit {
  width: 100%;
  margin-top: 38px;
}
.footnote {
  display: block;
  text-align: center;
  margin-top: 10px;
  font-size: 10px;
  color: var(--dl-muted);
}
</style>
