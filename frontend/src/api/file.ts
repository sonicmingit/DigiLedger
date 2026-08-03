import { ElMessage } from 'element-plus'
import http from './http'
import { buildOssUrl, normalizeObjectUrlResponse } from '@/utils/storage'

const MAX_IMAGE_UPLOAD_BYTES = 5 * 1024 * 1024

const loadImage = (file: File) =>
  new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片读取失败'))
    }
    img.src = url
  })

const canvasToBlob = (canvas: HTMLCanvasElement, quality: number) =>
  new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error('图片压缩失败'))
          return
        }
        resolve(blob)
      },
      'image/jpeg',
      quality
    )
  })

const compressImageFile = async (file: File, maxBytes: number) => {
  const img = await loadImage(file)
  const baseWidth = img.naturalWidth || img.width
  const baseHeight = img.naturalHeight || img.height
  if (!baseWidth || !baseHeight) {
    throw new Error('图片尺寸异常')
  }

  let scale = 1
  let quality = 0.9
  let lastBlob: Blob | null = null

  for (let i = 0; i < 10; i += 1) {
    const width = Math.max(1, Math.round(baseWidth * scale))
    const height = Math.max(1, Math.round(baseHeight * scale))
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      throw new Error('图片压缩失败')
    }
    ctx.drawImage(img, 0, 0, width, height)
    const blob = await canvasToBlob(canvas, quality)
    lastBlob = blob
    if (blob.size <= maxBytes) {
      const name = file.name.replace(/\.[^.]+$/, '') || 'image'
      return new File([blob], `${name}.jpg`, { type: blob.type })
    }
    if (quality > 0.6) {
      quality -= 0.1
    } else {
      scale *= 0.85
      quality = 0.85
    }
  }

  if (lastBlob) {
    const name = file.name.replace(/\.[^.]+$/, '') || 'image'
    return new File([lastBlob], `${name}.jpg`, { type: lastBlob.type })
  }
  throw new Error('图片压缩失败')
}

// 上传文件到对象存储，返回应用代理 URL
export const uploadFile = async (file: File) => {
  let uploadTarget = file
  if (file.size > MAX_IMAGE_UPLOAD_BYTES) {
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('文件超过 5MB，请压缩后再上传')
      throw new Error('文件过大')
    }
    ElMessage.info('图片超过 5MB，系统将自动压缩后上传')
    uploadTarget = await compressImageFile(file, MAX_IMAGE_UPLOAD_BYTES)
    if (uploadTarget.size > MAX_IMAGE_UPLOAD_BYTES) {
      ElMessage.warning('压缩后仍超过 5MB，请手动压缩后再上传')
      throw new Error('文件过大')
    }
  }
  const formData = new FormData()
  formData.append('file', uploadTarget)
  const result = await http.post<{ url: string; objectKey: string }>('/files/upload', formData)
  return normalizeObjectUrlResponse(result)
}

export interface UnusedAttachment {
  objectKey: string
  url: string
}

// 获取未使用的附件列表
export const getUnusedAttachments = () => {
  return http
    .get<UnusedAttachment[]>('/cleanup/unused-attachments')
    .then((items) => items.map((item) => ({ ...item, url: buildOssUrl(item.url || item.objectKey) })))
}

// 执行清理未使用的附件
export const cleanupUnusedAttachments = (objectKeys: string[]) => {
  return http.post<void>('/cleanup/unused-attachments', objectKeys)
}
