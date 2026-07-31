# DigiLedger PC 2.0

Vue 3 + TypeScript + Vite PC client for the DigiLedger 2.0 desktop experience.

```bash
npm install
npm run dev
npm run build
```

The development server proxies `/api` to `VITE_PROXY_TARGET` (defaults to `http://localhost:8080`). Set `VITE_API_BASE` when the API is hosted under a different prefix.
