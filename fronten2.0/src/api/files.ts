import { http, postBlob } from './http'
import { buildOssUrl, normalizeObjectUrlResponse } from '@/utils/storage'

export const uploadFile = async (file: File) => {
  const form = new FormData(); form.append('file', file)
  const result = await http.post<{ url: string; objectKey: string }>('/files/upload', form)
  return normalizeObjectUrlResponse(result)
}

export interface UnusedAttachment { objectKey: string; url?: string }
export const fetchUnusedAttachments = async () => {
  const items = await http.get<UnusedAttachment[]>('/cleanup/unused-attachments')
  return items.map((item) => ({ ...item, url: buildOssUrl(item.url || item.objectKey) }))
}
export const cleanupUnusedAttachments = (objectKeys: string[]) => http.post<void>('/cleanup/unused-attachments', objectKeys)

export interface RemoveBackgroundPreviewPayload { assetId?: number; attachmentId?: number; coverUrl?: string }
export const previewRemoveBackground = (payload: RemoveBackgroundPreviewPayload) => postBlob('/files/remove-bg-preview-binary', payload)
export const importRemoteImage = async (sourceUrl: string) => {
  const result = await http.post<{ url: string; objectKey: string }>('/files/import-remote-image', { sourceUrl })
  return normalizeObjectUrlResponse(result)
}
