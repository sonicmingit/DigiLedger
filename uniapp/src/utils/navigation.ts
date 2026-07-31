const fallbackByRoute: Record<string, string> = {
  "pages/assets/search": "/pages/assets/home",
  "pages/assets/editor": "/pages/assets/home",
  "pages/assets/detail": "/pages/assets/home",
  "pages/wishlist/editor": "/pages/wishlist/index",
  "pages/wishlist/detail": "/pages/wishlist/index",
  "pages/routes/index": "/pages/assets/home",
  "pages/routes/detail": "/pages/routes/index",
  "pages/settings/dictionary": "/pages/settings/index",
};

export function returnToPreviousPage(fallback?: string) {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
    return true;
  }

  const route = pages[pages.length - 1]?.route || "";
  const target = fallback || fallbackByRoute[route];
  if (target) {
    uni.reLaunch({ url: target });
    return true;
  }
  return false;
}

export function registerAndroidBackHandler() {
  if (typeof window === "undefined") return;
  let lastRootBackAt = 0;
  window.addEventListener("digiledger-backbutton", () => {
    if (returnToPreviousPage()) return;

    const now = Date.now();
    if (now - lastRootBackAt <= 2000) {
      (window as typeof window & { __digiledgerBackResult?: string })
        .__digiledgerBackResult = "exit";
      return;
    }
    lastRootBackAt = now;
    uni.showToast({ title: "再按一次退出应用", icon: "none", duration: 2000 });
  });
}
