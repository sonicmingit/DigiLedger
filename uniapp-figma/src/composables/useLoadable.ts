import { ref } from "vue";
/** Small async-state primitive shared by pages for consistent loading/error presentation. */
export function useLoadable<T>(initial: T) {
  const data = ref(initial),
    loading = ref(false),
    error = ref("");
  async function run(loader: () => Promise<T>) {
    loading.value = true;
    error.value = "";
    try {
      data.value = (await loader()) as any;
    } catch (e) {
      error.value = (e as Error).message || "加载失败";
    } finally {
      loading.value = false;
    }
  }
  return { data, loading, error, run };
}
