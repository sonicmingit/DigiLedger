<template>
  <view class="nav-shell" role="navigation" aria-label="主要导航">
    <view
      v-for="item in items"
      :key="item.key"
      class="nav-item"
      :class="[item.key, { active: active === item.key }]"
      role="button"
      tabindex="0"
      :aria-current="active === item.key ? 'page' : undefined"
      @click="go(item.path)"
      @keyup.enter="go(item.path)"
      @keyup.space="go(item.path)"
    >
      <image
        class="nav-icon"
        :src="item.icon"
        mode="aspectFit"
        aria-hidden="true"
      /><text>{{
        item.label
      }}</text>
    </view>
  </view>
</template>
<script setup lang="ts">
defineProps<{
  active: "assets" | "wishlist" | "routes" | "statistics" | "settings";
}>();
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
    key: "routes",
    label: "路线",
    path: "/pages/routes/index",
    icon: "/static/icons/upgrade.svg",
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
  z-index: var(--z-sticky);
  left: 50%;
  right: auto;
  width: calc(100% - (var(--space-md) * 2));
  max-width: var(--app-control-max);
  bottom: calc(var(--space-xs) + env(safe-area-inset-bottom));
  height: 64px;
  padding: var(--space-2xs);
  border: var(--rule-hairline);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  box-shadow: var(--shadow-raised);
  display: flex;
  align-items: center;
  justify-content: space-between;
  transform: translateX(-50%);
}
.nav-item {
  min-width: 0;
  flex: 1;
  height: 54px;
  border-radius: var(--radius-pill);
  color: var(--color-ink-2);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2xs);
  font-size: var(--text-xs);
  white-space: nowrap;
  -webkit-tap-highlight-color: transparent;
  transition:
    transform var(--dur-micro) var(--ease-out),
    background-color var(--dur-short) var(--ease-out);
}
.nav-item.active {
  background: var(--color-accent);
  color: var(--color-accent-ink);
  font-weight: 700;
}
.nav-item:active {
  transform: translateY(2px);
}
.nav-item:focus-visible {
  outline: 3px solid var(--color-focus);
  outline-offset: 2px;
}
.nav-icon {
  width: 17px;
  height: 17px;
  opacity: 0.68;
  transition: opacity var(--dur-short) var(--ease-out);
}
.nav-item:not(.assets) .nav-icon {
  filter: invert(1);
}
.nav-item.active .nav-icon {
  opacity: 1;
}
@media (hover: hover) and (pointer: fine) {
  .nav-item:not(.active):hover {
    background: var(--color-paper-2);
  }
}
@media (prefers-reduced-motion: reduce) {
  .nav-item:active,
  .nav-icon {
    transform: none;
    transition: none;
  }
}
</style>
