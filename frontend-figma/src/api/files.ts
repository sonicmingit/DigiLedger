import { http } from './http'
export const uploadFile = (file: File) => {
  const form = new FormData(); form.append('file', file)
  return http.post<{ url: string; objectKey: string }>('/files/upload', form)
}

export interface UnusedAttachment { objectKey: string; url?: string }
export const fetchUnusedAttachments = () => http.get<UnusedAttachment[]>('/cleanup/unused-attachments')
export const cleanupUnusedAttachments = (objectKeys: string[]) => http.post<void>('/cleanup/unused-attachments', objectKeys)
