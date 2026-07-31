<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { ensureServerProfile } from "@/services/server-profile";

onLaunch(() => ensureServerProfile());
</script>

<style lang="scss">
page {
  background: var(--dl-bg);
  color: var(--dl-text);
  font-family: var(--font-body);
  font-size: var(--text-base);
  font-variant-numeric: tabular-nums;
}
</style>
