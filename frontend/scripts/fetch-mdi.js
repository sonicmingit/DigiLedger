#!/usr/bin/env node
// Usage: node scripts/fetch-mdi.js football account
// or: npm run fetch-mdi -- football account

const fs = require('fs')
const path = require('path')
const https = require('https')

const CDN_BASE = 'https://materialdesignicons.com/cdn/1.6.50-dev/svg'

function download(name) {
  return new Promise((resolve, reject) => {
    const url = `${CDN_BASE}/${name}.svg`
    console.log(`Downloading ${url} ...`)
    https.get(url, (res) => {
      if (res.statusCode !== 200) {
        reject(new Error(`Failed to fetch ${url}: status ${res.statusCode}`))
        res.resume()
        return
      }
      const chunks = []
      res.on('data', (c) => chunks.push(c))
      res.on('end', () => {
        const body = Buffer.concat(chunks)
        const outDir = path.resolve(__dirname, '..', 'public', 'mdi')
        if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true })
        const outPath = path.join(outDir, `mdi-${name}.svg`)
        fs.writeFileSync(outPath, body)
        console.log(`Saved ${outPath}`)
        resolve(outPath)
      })
    }).on('error', (err) => reject(err))
  })
}

async function main() {
  const args = process.argv.slice(2).filter(Boolean)
  if (!args.length) {
    console.error('Please provide one or more icon names, e.g. node scripts/fetch-mdi.js football account')
    process.exit(1)
  }
  for (const name of args) {
    try {
      await download(name)
    } catch (err) {
      console.error(`Failed to download ${name}:`, err.message)
    }
  }
}

main()
