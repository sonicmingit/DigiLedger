import { createSSRApp } from "vue";
import App from "./App.vue";
import "../tokens.css";
import "./styles/theme.scss";
import { registerAndroidBackHandler } from "./utils/navigation";

export function createApp() {
  const app = createSSRApp(App);
  registerAndroidBackHandler();
  return { app };
}
