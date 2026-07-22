# DigiLedger PC Figma Rebuild

Vue 3 + TypeScript + Vite PC client rebuilt from the DigiLedger Figma source.

```bash
npm install
npm run dev
npm run build
```

The development server proxies `/api` to `VITE_PROXY_TARGET` (defaults to `http://localhost:8080`). Set `VITE_API_BASE` when the API is hosted under a different prefix.

