const DEFAULT_OSS_BUCKET = 'digiledger'
const OSS_BUCKET = ((import.meta.env.VITE_OSS_BUCKET as string | undefined) || DEFAULT_OSS_BUCKET).trim() || DEFAULT_OSS_BUCKET
const OSS_PREFIX = `/oss/${OSS_BUCKET}/`

const isHttpUrl = (value: string) => /^https?:\/\//i.test(value)
const isProtocolRelativeUrl = (value: string) => value.startsWith('//')
const isNonHttpUrl = (value: string) => /^(?:blob:|data:|mailto:|javascript:)/i.test(value)

const getApiBase = () => ((import.meta.env.VITE_API_BASE as string | undefined) || '').trim()

const normalizeApiRoot = () => {
  const raw = getApiBase()
  if (!raw) return ''

  if (isHttpUrl(raw)) {
    try {
      const url = new URL(raw)
      let pathname = url.pathname.replace(/\/+$/, '')
      if (pathname === '/api') pathname = ''
      else if (pathname.endsWith('/api')) pathname = pathname.slice(0, -4)
      return `${url.origin}${pathname}`.replace(/\/$/, '')
    } catch {
      // Fall through to path-only normalization for an invalid env value.
    }
  }

  let base = raw.replace(/\/+$/, '')
  if (base === '/api') return ''
  if (base.endsWith('/api')) base = base.slice(0, -4)
  return base.replace(/\/$/, '')
}

const normalizePath = (value: string) => value.split(/[?#]/, 1)[0].replace(/\\/g, '/')

const parseUrl = (value: string) => {
  try {
    return new URL(isProtocolRelativeUrl(value) ? `http:${value}` : value)
  } catch {
    return null
  }
}

const configuredHostnames = () => {
  const values = [getApiBase(), import.meta.env.VITE_OSS_ENDPOINT as string | undefined]
  if (typeof window !== 'undefined') values.push(window.location.origin)

  return values
    .filter((value): value is string => !!value && (isHttpUrl(value) || isProtocolRelativeUrl(value)))
    .map((value) => parseUrl(value)?.hostname.toLowerCase())
    .filter((value): value is string => !!value)
}

const isPrivateHostname = (hostname: string) => {
  const value = hostname.toLowerCase()
  if (value === 'localhost' || value === '::1' || value === '127.0.0.1') return true
  if (/^10\./.test(value) || /^192\.168\./.test(value)) return true
  const match = value.match(/^172\.(\d+)\./)
  return !!match && Number(match[1]) >= 16 && Number(match[1]) <= 31
}

const isLikelyStorageHost = (url: URL) => {
  const hostname = url.hostname.toLowerCase()
  return (
    configuredHostnames().includes(hostname) ||
    isPrivateHostname(hostname) ||
    /(?:^|[.-])(minio|s3|storage)(?:[.-]|$)/i.test(hostname) ||
    url.port === '9000' ||
    url.port === '19000'
  )
}

const extractKeyFromPath = (path: string) => {
  const normalized = normalizePath(path)
  const proxyIndex = normalized.indexOf(OSS_PREFIX)
  if (proxyIndex >= 0) {
    return normalized.slice(proxyIndex + OSS_PREFIX.length).replace(/^\/+/, '')
  }

  const bucketMarker = `/${OSS_BUCKET}/`
  if (normalized.startsWith(bucketMarker)) return normalized.slice(bucketMarker.length).replace(/^\/+/, '')
  if (normalized.startsWith(`${OSS_BUCKET}/`)) return normalized.slice(OSS_BUCKET.length + 1).replace(/^\/+/, '')
  return ''
}

const resolveLocalObjectKey = (value: string) => {
  if (isNonHttpUrl(value)) return ''

  if (isHttpUrl(value) || isProtocolRelativeUrl(value)) {
    const url = parseUrl(value)
    if (!url || !isLikelyStorageHost(url)) return ''
    return extractKeyFromPath(url.pathname)
  }

  const path = normalizePath(value)
  return extractKeyFromPath(path) || path.replace(/^\/+/, '')
}

/** Convert local object references to the same-origin application proxy. */
export const buildOssUrl = (value?: string | null): string => {
  if (value == null) return ''
  const trimmed = value.trim()
  if (!trimmed || isNonHttpUrl(trimmed)) return trimmed

  const objectKey = resolveLocalObjectKey(trimmed)
  if (!objectKey) return trimmed

  const base = normalizeApiRoot()
  const prefix = base ? `${base}${OSS_PREFIX}` : OSS_PREFIX
  return `${prefix}${objectKey}`.replace(/([^:]\/)\/+/g, '$1')
}

export const extractObjectKey = (value?: string | null): string => {
  if (value == null) return ''
  const trimmed = value.trim()
  if (!trimmed || isNonHttpUrl(trimmed)) return ''
  return resolveLocalObjectKey(trimmed)
}

export const buildOssUrls = (values?: (string | null | undefined)[]): string[] =>
  (values || []).map((value) => buildOssUrl(value)).filter(Boolean)

export const extractObjectKeys = (values?: (string | null | undefined)[]): string[] =>
  (values || []).map((value) => extractObjectKey(value)).filter(Boolean)

export const normalizeMediaUrls = <T>(value: T): T => {
  if (Array.isArray(value)) return value.map((item) => normalizeMediaUrls(item)) as T
  if (!value || typeof value !== 'object') return value

  const result: Record<string, unknown> = {}
  for (const [key, item] of Object.entries(value as Record<string, unknown>)) {
    if (key === 'imageUrl' || key === 'coverImageUrl') {
      result[key] = typeof item === 'string' ? buildOssUrl(item) : item
    } else if (key === 'coverImageUrls' || key === 'attachments') {
      result[key] = Array.isArray(item)
        ? item.map((entry) => (typeof entry === 'string' ? buildOssUrl(entry) : entry))
        : item
    } else {
      result[key] = normalizeMediaUrls(item)
    }
  }
  return result as T
}

export const normalizeObjectUrlResponse = <T extends { url?: string | null; objectKey?: string | null }>(value: T): T => {
  const normalizedUrl = buildOssUrl(value.url || value.objectKey)
  return { ...value, url: normalizedUrl || value.url || '' }
}

export const OSS_OBJECT_PREFIX = OSS_PREFIX
