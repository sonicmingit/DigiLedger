// Local visual preview only. Never used by production. Data lives in memory.
import { createServer } from 'node:http'
import { readFile } from 'node:fs/promises'

const categories = [
  { id: 1, name: '影音设备', parentId: null, level: 1, sort: 1, children: [{ id: 3, name: '耳机', parentId: 1, level: 2, sort: 1, children: [] }] },
  { id: 4, name: '手机', parentId: null, level: 1, sort: 2, children: [] }
]
const primary = (id, price, date) => ({ id, type: 'PRIMARY', price, purchaseDate: date, shippingCost: 0, quantity: 1, platformId: 1, platformName: '京东', attachments: [] })
const makeAsset = (id, name, price, date, cover, status = '使用中') => ({
  id, name, model: name.replace('Sony ', ''), categoryId: 3, categoryPath: '/1/3', brand: { id: 1, name: 'Sony' }, brandName: 'Sony',
  status, purchaseDate: date, specifications: '无线降噪耳机 · 黑色', coverImageUrl: cover ? `/oss/digiledger/${cover}.png` : null,
  totalInvest: price ?? 0, avgCostPerDay: 3.49, useDays: 687, lastNetIncome: 0, tags: [], relatedLinks: [], purchases: price === null ? [] : [primary(id * 10, price, date)], sales: []
})
const assets = new Map([
  [1, { ...makeAsset(1, 'Sony WH-1000XM5', 2399, '2025-03-20', 'xm5'), predecessorAssetId: 2 }],
  [2, makeAsset(2, 'Sony WH-1000XM4', 1999, '2023-03-20', 'xm4', '已出售')],
  [3, makeAsset(3, 'Sony WH-1000XM3', 1599, '2021-03-20', null, '已闲置')],
  [4, { ...makeAsset(4, '手机示例', 4999, '2024-01-01', null), categoryId: 4, categoryPath: '/4' }],
  [5, makeAsset(5, '尚未录入购买信息的耳机', null, null, null)]
])
function summary(asset, current) {
  const previous = asset.purchases.find(p => p.type === 'PRIMARY')
  return { id: asset.id, name: asset.name, categoryId: asset.categoryId, categoryPath: asset.categoryPath, brandName: asset.brandName,
    model: asset.model, status: asset.status, coverImageUrl: asset.coverImageUrl, primaryPrice: previous?.price ?? null,
    primaryPurchaseDate: previous?.purchaseDate ?? null,
    primaryPriceDelta: current?.price != null && previous?.price != null ? Math.round((current.price - previous.price) * 100) / 100 : null,
    purchaseGapDays: current?.purchaseDate && previous?.purchaseDate ? Math.round((Date.parse(current.purchaseDate) - Date.parse(previous.purchaseDate)) / 86400000) : null }
}
function detail(asset) {
  const predecessor = assets.get(asset.predecessorAssetId)
  return { ...asset, predecessorAsset: predecessor ? summary(predecessor, asset.purchases.find(p => p.type === 'PRIMARY')) : null }
}
createServer(async (req, res) => {
  const url = new URL(req.url, 'http://127.0.0.1')
  const send = (data, code = 200, msg = '成功') => { res.writeHead(code === 200 ? 200 : 400, { 'Content-Type': 'application/json; charset=utf-8' }); res.end(JSON.stringify({ code, data, msg })) }
  try {
    if (/^\/oss\/digiledger\/(xm4|xm5)\.png$/.test(url.pathname)) {
      res.writeHead(200, { 'Content-Type': 'image/png' }); res.end(await readFile(new URL(`./assets/${url.pathname.split('/').pop()}`, import.meta.url))); return
    }
    const dictionaries = { '/api/dict/categories/tree': categories, '/api/dict/brands': [{ id: 1, name: 'Sony' }], '/api/dict/tags/tree': [], '/api/dict/platforms': [{ id: 1, name: '京东' }], '/api/image-search/providers': { providers: [], enabledProviders: [] } }
    if (url.pathname in dictionaries) return send(dictionaries[url.pathname])
    if (url.pathname === '/api/assets/predecessor-options') {
      const categoryId = Number(url.searchParams.get('category_id')), exclude = Number(url.searchParams.get('exclude_asset_id')), q = (url.searchParams.get('q') || '').toLowerCase()
      return send([...assets.values()].filter(a => a.categoryId === categoryId && a.id !== exclude && a.predecessorAssetId !== exclude && `${a.name} ${a.model}`.toLowerCase().includes(q)).map(a => summary(a)))
    }
    const match = url.pathname.match(/^\/api\/assets\/(\d+)$/)
    if (match) {
      const id = Number(match[1]), asset = assets.get(id)
      if (!asset) return send(null, 404, '物品不存在')
      if (req.method === 'PUT') {
        let body = ''; for await (const chunk of req) body += chunk
        const payload = JSON.parse(body), previous = assets.get(payload.predecessorAssetId)
        if (previous && (previous.categoryId !== payload.categoryId || previous.id === id)) return send(null, 400, '上代产品必须与当前物品同类别且不能为自身')
        Object.assign(asset, payload)
        asset.brand = { id: 1, name: 'Sony' }
        asset.totalInvest = asset.purchases.reduce((sum, p) => sum + Number(p.price || 0) + Number(p.shippingCost || 0), 0)
        return send(null)
      }
      return send(detail(asset))
    }
    return send(null, 404, '此演示仅支持上代产品相关页面')
  } catch (error) { send(null, 500, error.message) }
}).listen(18081, '127.0.0.1', () => console.log('Predecessor visual preview fixtures: http://127.0.0.1:18081'))
