import { http } from './http'
export const uploadFile = (file: File) => {
  const form = new FormData(); form.append('file', file)
  return http.post<{ url: string; objectKey: string }>('/files/upload', form)
}
