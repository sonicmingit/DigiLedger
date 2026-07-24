<template>
  <div class="upload-tester">
    <section class="card tester-card">
      <div class="tester-heading"><div><h2>对象存储上传测试</h2><p>验证当前环境的文件上传和访问地址是否正常。</p></div></div>
      <AttachmentDropzone panel label="选择测试文件" hint="拖拽文件、点击选择，或直接粘贴截图" @files="upload" />
      <div v-if="uploading" class="tester-status">正在上传测试文件…</div>
      <div v-else-if="uploaded" class="upload-result">
        <img v-if="isImage(uploaded.url)" :src="uploaded.url" alt="上传测试预览" />
        <div><strong>上传成功</strong><span>对象键：{{ uploaded.objectKey }}</span><a :href="uploaded.url" target="_blank" rel="noopener">打开访问地址</a></div>
      </div>
    </section>

    <section class="card tester-card cleanup-card">
      <div class="tester-heading"><div><h2>手动清理未使用文件</h2><p>扫描未被任何记录引用的附件；删除前可逐项确认。</p></div><button class="secondary-button" :disabled="scanning" @click="scan">{{ scanning ? '扫描中' : '扫描附件' }}</button></div>
      <div v-if="scanned" class="cleanup-content">
        <p v-if="!unused.length" class="empty-note">没有发现未使用的附件。</p>
        <template v-else>
          <label class="select-all"><input v-model="allSelected" type="checkbox" /> 全选 {{ unused.length }} 个文件</label>
          <div class="unused-list"><label v-for="file in unused" :key="file.objectKey" class="unused-item"><input v-model="selectedKeys" type="checkbox" :value="file.objectKey" /><img v-if="isImage(file.url)" :src="file.url" alt="" /><span v-else class="file-icon">文件</span><span><strong>{{ fileName(file.objectKey) }}</strong><small>{{ file.objectKey }}</small></span></label></div>
          <div class="cleanup-actions"><span>已选择 {{ selectedKeys.length }} 个</span><button class="secondary-button danger-button" :disabled="!selectedKeys.length || deleting" @click="removeSelected">{{ deleting ? '删除中' : '删除选中项' }}</button></div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cleanupUnusedAttachments, fetchUnusedAttachments, uploadFile, type UnusedAttachment } from '@/api/files'
import AttachmentDropzone from './AttachmentDropzone.vue'

const uploading = ref(false), scanned = ref(false), scanning = ref(false), deleting = ref(false)
const uploaded = ref<{ url: string; objectKey: string }>()
const unused = ref<UnusedAttachment[]>([]), selectedKeys = ref<string[]>([])
const allSelected = computed({ get: () => unused.value.length > 0 && selectedKeys.value.length === unused.value.length, set: value => { selectedKeys.value = value ? unused.value.map(v => v.objectKey) : [] } })
const isImage = (url?: string) => !!url && /\.(png|jpe?g|gif|webp|bmp|svg)(?:\?|$)/i.test(url)
const fileName = (key: string) => key.split('/').pop() || key
async function upload(files: File[]) { const file = files[0]; if (!file) return; uploading.value = true; try { uploaded.value = await uploadFile(file); ElMessage.success('测试文件上传成功') } catch (e) { ElMessage.error((e as Error).message || '上传失败') } finally { uploading.value = false } }
async function scan() { scanning.value = true; try { unused.value = await fetchUnusedAttachments(); selectedKeys.value = []; scanned.value = true; ElMessage.success(`扫描完成，发现 ${unused.value.length} 个未使用文件`) } catch (e) { ElMessage.error((e as Error).message || '扫描失败') } finally { scanning.value = false } }
async function removeSelected() { try { await ElMessageBox.confirm(`将永久删除选中的 ${selectedKeys.value.length} 个文件，确认继续？`, '确认清理未使用文件', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }); deleting.value = true; await cleanupUnusedAttachments(selectedKeys.value); ElMessage.success('已清理选中文件'); await scan() } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error((e as Error).message || '清理失败') } finally { deleting.value = false } }
</script>

<style scoped>
.upload-tester{display:grid;gap:24px;max-width:980px}.tester-card{padding:24px}.tester-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;margin-bottom:18px}.tester-heading h2{margin:0;font-size:18px}.tester-heading p{margin:7px 0 0;color:var(--dl-text-secondary);font-size:12px}.tester-status,.empty-note{color:var(--dl-text-secondary);font-size:12px}.upload-result{display:flex;gap:14px;align-items:center;margin-top:16px;padding:12px;border-radius:14px;background:var(--dl-bg-alt)}.upload-result img{width:72px;height:72px;object-fit:cover;border-radius:10px}.upload-result div{display:grid;gap:5px;min-width:0;font-size:12px}.upload-result span{color:var(--dl-text-secondary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.upload-result a{color:var(--dl-success);font-weight:600;text-decoration:none}.cleanup-content{border-top:1px solid var(--dl-border);padding-top:16px}.select-all{display:flex;gap:8px;align-items:center;font-size:12px;font-weight:600}.unused-list{display:grid;gap:8px;margin-top:12px;max-height:330px;overflow:auto}.unused-item{display:flex;align-items:center;gap:10px;padding:9px 10px;border-radius:11px;background:var(--dl-bg-alt);font-size:12px;cursor:pointer}.unused-item img,.file-icon{width:38px;height:38px;border-radius:8px;object-fit:cover}.file-icon{display:grid;place-items:center;background:#e8f2d5;color:#60713b;font-size:10px}.unused-item span:last-child{display:grid;gap:3px;min-width:0}.unused-item strong,.unused-item small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.unused-item small{color:var(--dl-text-secondary)}.cleanup-actions{display:flex;justify-content:space-between;align-items:center;margin-top:16px;color:var(--dl-text-secondary);font-size:12px}.danger-button{color:var(--dl-danger);background:#fff2f0}.danger-button:disabled{opacity:.5}
</style>
