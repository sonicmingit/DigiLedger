<template>
  <section class="open-api-settings" v-loading="loading">
    <div v-if="loadError" class="error-line" role="alert">{{ loadError }} <el-button text @click="load">重试</el-button></div>
    <template v-else>
      <header class="intro"><div><span class="eyebrow">DIGILEDGER · AGENT ACCESS</span><h2>让 Agent 帮你管理物品</h2><p>查询库存、整理设备信息，或把一句话中的购买信息直接录入账本。</p></div><span class="state-badge" :class="{ enabled: savedEnabled }">{{ savedEnabled ? '开放 API 已启用' : '开放 API 未启用' }}</span></header>
      <div class="access-grid">
        <section class="access-card">
          <h3>访问配置</h3><p class="hint">所有开放接口均需 Bearer Token。先生成 Token，再启用访问。</p>
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="开放 API 地址"><el-input v-model="connectionUrl" /><span class="hint">填写 Agent 能访问的系统地址，保留 /api/open/v1 后缀。</span></el-form-item>
            <el-form-item label="访问权限"><el-radio-group v-model="settings.allowWrite" :disabled="saving"><el-radio :value="false" :label="false">只读 · 查询物品</el-radio><el-radio :value="true" :label="true">读写 · 新增、编辑、删除</el-radio></el-radio-group></el-form-item>
            <div class="toggle-row"><div><strong>启用开放 API</strong><p class="hint">关闭后，所有对外物品请求都会被拒绝。</p></div><el-switch v-model="settings.enabled" :disabled="!settings.tokenConfigured || saving" /></div>
            <div class="token-status"><span>{{ settings.tokenConfigured ? `已配置 Token：${settings.tokenPrefix || ''}…` : '尚未生成 Token' }}</span><el-button :loading="rotating" :disabled="saving || loading" @click="rotate">{{ settings.tokenConfigured ? '重新生成 Token' : '生成 Token' }}</el-button></div>
            <div v-if="token" class="token-box"><strong>请保存此 Token，离开页面后无法再次查看</strong><el-input :model-value="token" type="password" show-password readonly autocomplete="off" /><div><el-button size="small" @click="copy(token)">复制 Token</el-button><el-button size="small" text @click="token=''">已保存，隐藏</el-button></div></div>
            <div class="save-row"><span class="hint">{{ settings.allowWrite ? '读写权限可修改当前账本的所有物品。' : '只读权限适合日常查询。' }}</span><el-button type="primary" :loading="saving" :disabled="loading || rotating" @click="save">保存配置</el-button></div>
          </el-form>
        </section>
        <section class="access-card install-card">
          <h3>安装 DigiLedger Skill</h3><p class="hint">支持读取 SKILL.md 的 Agent；调用脚本需要 Python 3.9+，无需额外依赖。</p>
          <ol class="steps">
            <li><strong>下载并解压</strong><p>解压技能包，得到 <code>digiledger</code> 文件夹。</p><a class="download-link" :href="resourceUrl('digiledger-skill.zip')" download>下载 Skill 技能包 ↗</a></li>
            <li><strong>放入 Agent 的技能目录</strong><p>Codex：<code>~/.agents/skills/digiledger/</code></p><p>Windows：<code>%USERPROFILE%\.agents\skills\digiledger\</code></p><p>该目录内应直接包含 <code>SKILL.md</code>。重新加载技能或重启 Agent；其他 Agent 使用其对应的技能目录。</p></li>
            <li><strong>配置连接</strong><p>在 Agent 运行环境设置以下变量；从该环境启动 Agent 后生效。</p><el-radio-group v-model="shell" size="small"><el-radio-button value="powershell" label="powershell">PowerShell</el-radio-button><el-radio-button value="bash" label="bash">macOS / Linux</el-radio-button></el-radio-group><div class="code-block"><pre>{{ envExample }}</pre><el-button size="small" @click="copy(envExample)">复制示例</el-button></div><p>将占位符换成本机 Token，凭据不需要写进技能文件。</p></li>
            <li><strong>开始使用</strong><p class="example">“查一下我的相机有哪些。”</p><p class="example">“帮我录入一台索尼 A7C II，12999 元，10 月 9 日买的。”</p><p>Agent 会查询真实分类并按提供的信息录入。新增、编辑需启用读写权限。</p></li>
          </ol>
        </section>
      </div>
      <section class="access-card api-reference"><div class="reference-heading"><div><h3>对外 API 文档</h3><p class="hint">基地址 /api/open/v1 · JSON 响应 {code,data,msg}</p></div><div class="doc-links"><a :href="resourceUrl('api.md')" target="_blank" rel="noopener">完整接口说明 ↗</a><a :href="resourceUrl('openapi.json')" target="_blank" rel="noopener">OpenAPI JSON ↗</a></div></div>
        <div class="table-scroll"><table><thead><tr><th>方法</th><th>路径</th><th>说明</th><th>权限</th></tr></thead><tbody><tr v-for="endpoint in endpoints" :key="endpoint.method+endpoint.path"><td><code class="method">{{ endpoint.method }}</code></td><td><code>{{ endpoint.path }}</code></td><td>{{ endpoint.description }}</td><td>{{ endpoint.write ? '读写' : '只读' }}</td></tr></tbody></table></div>
        <p class="boundary-note">Token 仅保护开放接口，当前账本内的物品共享访问。公网接入请使用 HTTPS，并将现有管理页面及内部 API 放在可信网络或网关认证之后。</p>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

interface AccessSettings { enabled: boolean; allowWrite: boolean; tokenConfigured: boolean; tokenPrefix?: string }
const apiRoot = (import.meta.env.VITE_API_BASE || '/api').replace(/\/+$/, '')
const apiUrl = new URL(`${apiRoot}/`, window.location.href)
const resourceUrl = (file: string) => new URL(`agent-resources/${file}`, apiUrl).href
const connectionUrl = ref(new URL('open/v1', apiUrl).href)
const settings = reactive<AccessSettings>({ enabled: false, allowWrite: false, tokenConfigured: false })
const token = ref(''), loading = ref(true), saving = ref(false), rotating = ref(false), loadError = ref('')
const savedEnabled = ref(false), shell = ref('powershell')
const envExample = computed(() => shell.value === 'powershell'
  ? `$env:DIGILEDGER_API_URL = '${connectionUrl.value.replace(/'/g, "''")}'\n$env:DIGILEDGER_API_TOKEN = '<你的 Token>'`
  : `export DIGILEDGER_API_URL='${connectionUrl.value.replace(/'/g, "'\"'\"'")}'\nexport DIGILEDGER_API_TOKEN='<你的 Token>'`)
const endpoints = [
  { method: 'GET', path: '/assets', description: '分页查询，支持关键词、状态与字典筛选', write: false },
  { method: 'GET', path: '/assets/{id}', description: '详情、购买记录与投入成本', write: false },
  { method: 'POST', path: '/assets', description: '新增物品，可同时录入购买记录', write: true },
  { method: 'PATCH', path: '/assets/{id}', description: '只修改提交字段，保留其他信息', write: true },
  { method: 'DELETE', path: '/assets/{id}', description: '删除物品，检查关联记录', write: true },
  { method: 'GET', path: '/dict/categories/tree', description: '查询分类树', write: false },
  { method: 'GET', path: '/dict/brands', description: '查询品牌', write: false },
  { method: 'GET', path: '/dict/platforms', description: '查询平台', write: false },
  { method: 'GET', path: '/dict/tags/tree', description: '查询标签树', write: false }
]
async function request<T>(method: 'GET' | 'POST' | 'PUT', data?: unknown): Promise<T> {
  const response = await axios.request<{ code: number; data: T; msg: string }>({ method, url: `${apiRoot}/settings/open-api${method==='POST' ? '/token' : ''}`, data, timeout: 15000 })
  if (response.data.code !== 200) throw new Error(response.data.msg || '操作失败')
  return response.data.data
}
function errorMessage(error: unknown) { return axios.isAxiosError(error) ? error.response?.data?.msg || error.message : (error as Error).message }
async function load() {
  loading.value = true; loadError.value = ''
  try { Object.assign(settings, await request<AccessSettings>('GET')); savedEnabled.value = settings.enabled }
  catch (error) { loadError.value = errorMessage(error) }
  finally { loading.value = false }
}
async function save() {
  saving.value = true
  try { await request('PUT', { enabled: settings.enabled, allowWrite: settings.allowWrite }); await load(); ElMessage.success('开放 API 配置已保存') }
  catch (error) { ElMessage.error(errorMessage(error)) }
  finally { saving.value = false }
}
async function rotate() {
  if (settings.tokenConfigured) {
    try { await ElMessageBox.confirm('重新生成后，旧 Token 会立即失效。请同步更新 Agent 的连接配置。', '重新生成 Token', { confirmButtonText: '重新生成', cancelButtonText: '取消', type: 'warning' }) }
    catch { return }
  }
  rotating.value = true
  try { token.value = (await request<{ token: string }>('POST')).token; settings.tokenConfigured = true; settings.tokenPrefix = token.value.slice(0,11); ElMessage.success('Token 已生成，请复制并妥善保存') }
  catch (error) { ElMessage.error(errorMessage(error)) }
  finally { rotating.value = false }
}
async function copy(value: string) {
  try { await navigator.clipboard.writeText(value); ElMessage.success('已复制') }
  catch { ElMessage.warning('浏览器不支持复制，请选中文字手动复制') }
}
onMounted(load)
</script>

<style scoped>
.open-api-settings{color:var(--dl-text,var(--el-text-color-primary));display:grid;gap:22px}.intro{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;padding:8px 0 6px}.eyebrow{font-size:10px;letter-spacing:1.6px;color:var(--dl-text-secondary,var(--el-text-color-secondary));font-weight:700}.intro h2{font-size:25px;margin:10px 0}.intro p,.hint{font-size:12px;line-height:1.7;color:var(--dl-text-secondary,var(--el-text-color-secondary));margin:5px 0}.state-badge{font-size:11px;white-space:nowrap;border:1px solid var(--el-border-color-light);padding:8px 12px;border-radius:20px}.state-badge.enabled{background:var(--dl-accent-soft,#eaf7de);border-color:var(--el-color-success-light-5)}.access-grid{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1.2fr);gap:22px}.access-card{border:1px solid var(--el-border-color-light);border-radius:18px;padding:24px;background:var(--dl-surface,var(--el-bg-color))}.access-card h3{font-size:16px;margin:0 0 8px}.access-card .el-form{margin-top:22px}.toggle-row,.token-status,.save-row{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-top:22px}.toggle-row strong{font-size:13px}.token-status{font-size:12px;flex-wrap:wrap;border-top:1px solid var(--el-border-color-lighter);padding-top:20px}.token-box{display:grid;gap:12px;padding:16px;margin-top:16px;border-radius:12px;background:var(--dl-accent-soft,var(--el-fill-color-light));font-size:12px}.save-row{margin-top:26px}.steps{padding-left:22px;font-size:13px;margin:22px 0 0}.steps li{padding-left:8px;margin-bottom:24px}.steps li:last-child{margin-bottom:0}.steps p{color:var(--dl-text-secondary,var(--el-text-color-secondary));font-size:12px;line-height:1.7;margin:7px 0;overflow-wrap:anywhere}code,pre{font-family:Consolas,'SFMono-Regular',monospace;font-size:12px}.download-link,.doc-links a{color:var(--dl-text,var(--el-color-primary));font-size:12px;text-decoration:underline;text-underline-offset:4px}.download-link{display:inline-block;margin-top:7px;font-weight:600}.code-block{padding:12px 14px;background:var(--dl-bg-alt,var(--el-fill-color-light));border-radius:10px}.code-block pre{white-space:pre-wrap;overflow-wrap:anywhere;margin:0 0 10px;line-height:1.7}.steps .example{color:var(--dl-text,var(--el-text-color-primary));border-left:2px solid var(--dl-lime,var(--el-color-primary));padding-left:10px}.reference-heading{display:flex;justify-content:space-between;align-items:center;gap:16px}.doc-links{display:flex;gap:20px;flex-wrap:wrap}.table-scroll{overflow:auto;margin-top:20px}table{width:100%;border-collapse:collapse;font-size:12px;text-align:left}th{color:var(--dl-text-secondary,var(--el-text-color-secondary));font-size:11px;font-weight:500}th,td{padding:12px;border-bottom:1px solid var(--el-border-color-lighter)}td:first-child,td:nth-child(2){white-space:nowrap}.method{font-weight:700}.boundary-note{font-size:11px;line-height:1.8;color:var(--dl-text-secondary,var(--el-text-color-secondary));margin:18px 0 0}.error-line{color:var(--el-color-danger);padding:20px}@media(max-width:1100px){.access-grid{grid-template-columns:1fr}.reference-heading{align-items:flex-start;flex-direction:column}.intro h2{font-size:22px}}
</style>
