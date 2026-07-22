<template>
  <view class="nav-shell">
    <view
      v-for="item in items"
      :key="item.key"
      class="nav-item"
      :class="[item.key, { active: active === item.key }]"
      @click="go(item.path)"
    >
      <image class="nav-icon" :src="item.icon" mode="aspectFit" /><text>{{
        item.label
      }}</text>
    </view>
  </view>
</template>
<script setup lang="ts">
defineProps<{ active: "assets" | "wishlist" | "statistics" | "settings" }>();
const items = [
  {
    key: "assets",
    label: "物品",
    path: "/pages/assets/home",
    icon: "/static/icons/box.svg",
  },
  {
    key: "wishlist",
    label: "心愿",
    path: "/pages/wishlist/index",
    icon: "/static/icons/heart.svg",
  },
  {
    key: "statistics",
    label: "统计",
    path: "/pages/statistics/index",
    icon: "/static/icons/stats.svg",
  },
  {
    key: "settings",
    label: "设置",
    path: "/pages/settings/index",
    icon: "/static/icons/settings.svg",
  },
];
function go(path: string) {
  const pages = getCurrentPages();
  if (pages[pages.length - 1]?.route === path.slice(1)) return;
  uni.reLaunch({ url: path });
}
</script>
<style scoped>
.nav-shell {
  position: fixed;
  z-index: 50;
  left: max(17px, env(safe-area-inset-left));
  right: max(17px, env(safe-area-inset-right));
  bottom: calc(10px + env(safe-area-inset-bottom));
  height: 68px;
  padding: 7px 8px;
  border-radius: 999px;
  background: var(--dl-black);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-item {
  width: 23%;
  height: 54px;
  border-radius: 999px;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 10px;
}
.nav-item.active {
  background: var(--dl-lime);
  color: var(--dl-text);
}
.nav-icon {
  width: 16px;
  height: 16px;
}
.nav-item:not(.active).assets .nav-icon,
.nav-item.active:not(.assets) .nav-icon {
  filter: invert(1);
}
</style>
