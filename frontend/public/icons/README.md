# 移动端图标获取说明

以下 Base64 文本文件用于手动恢复 PWA 图标，请在需要时执行以下步骤：

1. 将 `icon-192.base64.txt` 或 `icon-512.base64.txt` 的全部内容复制到新建的同名 `.txt` 文件中。
2. 使用命令行转码生成 PNG，例如：
   ```bash
   base64 -d icon-192.base64.txt > icon-192.png
   base64 -d icon-512.base64.txt > icon-512.png
   ```
3. 把生成的 PNG 图标放回 `frontend/public/icons/` 目录即可。

如需在 Mac 上使用，可将命令中的 `base64 -d` 替换为 `base64 -D`。
