import http from './http'

// 上传文件到 MinIO，返回可访问的 URL
export const uploadFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<{ url: string; objectKey: string }>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
