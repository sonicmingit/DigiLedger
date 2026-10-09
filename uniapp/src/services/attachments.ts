import { checkUploadSize, uploadFile } from "./http";

type SelectedAttachment = { path: string; size?: number };

/** The Android package embeds the H5 build in Capacitor's WebView. */
export async function chooseAndUploadAttachments(
  remaining: number,
  add: (url: string) => void,
) {
  if (remaining <= 0) {
    uni.showToast({ title: "最多添加 10 个附件", icon: "none" });
    return;
  }
  const source = await new Promise<number | null>((resolve) => {
    uni.showActionSheet({
      itemList: ["拍照", "从相册选择", "从文件选择"],
      success: (result) => resolve(result.tapIndex),
      fail: () => resolve(null),
    });
  });
  if (source == null) return;

  let files: SelectedAttachment[];
  try {
    if (source === 2) {
      const selected = await uni.chooseFile({ count: remaining, type: "all" });
      const paths = Array.isArray(selected.tempFilePaths) ? selected.tempFilePaths : [selected.tempFilePaths];
      const entries = Array.isArray(selected.tempFiles) ? selected.tempFiles : [selected.tempFiles];
      files = paths.slice(0, remaining).map((path, index) => ({
        path,
        size: entries[index]?.size,
      }));
    } else {
      const selected = await uni.chooseImage({
        count: remaining,
        sizeType: ["compressed"],
        sourceType: [source === 0 ? "camera" : "album"],
      });
      const paths = Array.isArray(selected.tempFilePaths) ? selected.tempFilePaths : [selected.tempFilePaths];
      const entries = Array.isArray(selected.tempFiles) ? selected.tempFiles : [selected.tempFiles];
      files = paths.slice(0, remaining).map((path, index) => ({
        path,
        size: entries[index]?.size,
      }));
    }
    files.forEach((file) => checkUploadSize(file.size));
  } catch (error) {
    if (String((error as Error).message).includes("cancel")) return;
    uni.showToast({ title: (error as Error).message, icon: "none" });
    return;
  }
  if (!files.length) return;

  uni.showLoading({ title: "上传中" });
  let uploaded = 0;
  try {
    for (const file of files) {
      const result = await uploadFile(file.path, undefined, file.size);
      add(result.url);
      uploaded++;
    }
    uni.showToast({ title: `已添加 ${uploaded} 个附件`, icon: "success" });
  } catch (error) {
    uni.showToast({
      title: uploaded ? `已添加 ${uploaded} 个，后续上传失败` : (error as Error).message,
      icon: "none",
    });
  } finally {
    uni.hideLoading();
  }
}
