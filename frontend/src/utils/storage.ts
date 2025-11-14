const OSS_BUCKET = 'digiledger'
const OSS_PREFIX = `/oss/${OSS_BUCKET}/`

const isAbsoluteUrl = (value: string) =>
  value.startsWith('http://') || value.startsWith('https://') || value.startsWith('//')

const normalizeBase = () => {
  const raw = (import.meta.env.VITE_API_BASE as string | undefined) || ''
  if (!raw) return ''
  let base = raw.trim()
  if (base.endsWith('/')) {
    base = base.slice(0, -1)
  }
  if (base.endsWith('/api')) {
    base = base.slice(0, -4)
  }
  return base
}

const removeHost = (value: string) => {
  if (value.startsWith('http://') || value.startsWith('https://')) {
    try {
      const url = new URL(value)
      return url.pathname
    } catch {
      return value.replace(/^https?:\/\/[^/]+/i, '')
    }
  }
  if (value.startsWith('//')) {
    try {
      const url = new URL(`http:${value}`)
      return url.pathname
    } catch {
      return value.replace(/^\/\//, '')
    }
  }
  return value
}

export const extractObjectKey = (value?: string | null): string => {
  if (!value) return ''
  let input = removeHost(value.trim())
  if (!input) return ''
  const base = normalizeBase()
  if (base && input.startsWith(base)) {
    input = input.slice(base.length)
  }
  if (!input.startsWith('/')) {
    input = `/${input}`
  }
  const ossIndex = input.indexOf(OSS_PREFIX)
  if (ossIndex >= 0) {
    return input.slice(ossIndex + OSS_PREFIX.length)
  }
  const bucketSegment = `/${OSS_BUCKET}/`
  const bucketIndex = input.indexOf(bucketSegment)
  if (bucketIndex >= 0) {
    return input.slice(bucketIndex + bucketSegment.length)
  }
  return input.replace(/^\//, '')
}

export const buildOssUrl = (value?: string | null): string => {
  if (!value) return ''
  const trimmed = value.trim()
  if (!trimmed) return ''
  if (isAbsoluteUrl(trimmed)) {
    return trimmed
  }
  const objectKey = extractObjectKey(trimmed)
  if (!objectKey) return ''
  const base = normalizeBase()
  const prefix = base ? `${base}${OSS_PREFIX}` : OSS_PREFIX
  return `${prefix}${objectKey}`.replace(/([^:]\/)\/+/g, '$1')
}

export const buildOssUrls = (values?: (string | null | undefined)[]): string[] => {
  if (!values || !values.length) return []
  return values
    .map((item) => buildOssUrl(item))
    .filter((item): item is string => !!item)
}

export const extractObjectKeys = (values?: (string | null | undefined)[]): string[] => {
  if (!values || !values.length) return []
  return values
    .map((item) => extractObjectKey(item))
    .filter((item) => !!item)
}

export const OSS_OBJECT_PREFIX = OSS_PREFIX
