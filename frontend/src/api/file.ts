import http from './http'

// 上传文件到 MinIO，返回可访问的 URL
export const uploadFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<{ url: string; objectKey: string }>('/files/upload', formData)
}

export interface UnusedAttachment {
  objectKey: string
  url: string
}

// 获取未使用的附件列表
export const getUnusedAttachments = () => {
  return http.get<UnusedAttachment[]>('/cleanup/unused-attachments')
}

// 执行清理未使用的附件
export const cleanupUnusedAttachments = (objectKeys: string[]) => {
  return http.post<void>('/cleanup/unused-attachments', objectKeys)
}
