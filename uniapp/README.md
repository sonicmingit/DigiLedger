# DigiLedger uni-app client

Figma-driven Vue 3 + uni-app client for H5 and Android. Configure API nodes in Settings before connecting a physical device.

```bash
npm install
npm run dev:h5
npm run build:h5
npm run build:app-android
```

## 打包 APK

每次发布请带上语义化版本号。脚本会同步 Web、uni-app 与 Android 的版本信息，生成
`dist/apk/digiLedger.vx.x.x.apk`：

```powershell
.\scripts\build-apk.ps1 -Version 1.0.1 -Notes "修复物品编辑"
# 或 npm run build:apk:version -- 1.0.1 "修复物品编辑"
```

`-Notes` 为必填项；每次成功打包后，内容会自动追加到 `CHANGELOG.md`。

Android release signing, final application id and production icons are intentionally left for the release pipeline.
