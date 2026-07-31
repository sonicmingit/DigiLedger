import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.sonic.digiledger",
  appName: "DigiLedger",
  webDir: "dist/build/h5",
  bundledWebRuntime: false,
  android: {
    allowMixedContent: true,
  },
};

export default config;
