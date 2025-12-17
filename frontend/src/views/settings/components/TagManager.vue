<template>
  <div class="tag-manager" v-loading="loading">
    <div class="toolbar">
      <el-button type="primary" @click="openCreateRoot">新增根标签</el-button>
      <el-button @click="refreshDicts">刷新</el-button>
      <el-input v-model="filterText" placeholder="搜索标签" clearable size="small" class="toolbar-search">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <div class="content">
      <el-tree
        ref="treeRef"
        :data="tagTree"
        node-key="id"
        default-expand-all
        draggable
        :props="{ children: 'children', label: 'name' }"
        :filter-node-method="filterNode"
        empty-text="暂无标签"
        @node-drop="handleDrop"
      >
        <template #default="{ data }">
          <span class="tag-node">
            <span v-if="data.color" class="color-dot" :style="{ backgroundColor: data.color }"></span>
            <IconRenderer :icon="data.icon" />
            <span class="name">{{ data.name }}</span>
          </span>
          <span class="node-actions">
            <el-button link type="primary" @click.stop="openCreateChild(data)">新增子标签</el-button>
            <el-button link @click.stop="openEdit(data)">编辑</el-button>
            <el-button link type="danger" @click.stop="removeTag(data)">删除</el-button>
          </span>
        </template>
      </el-tree>
    </div>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑标签' : '新增标签'" width="520px" @closed="reset">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" status-icon>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="标签名称" />
        </el-form-item>
        <el-form-item label="父级标签">
          <el-input v-model="parentLabel" disabled />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" show-alpha />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="如 ri-star-fill (也可以点击下方选择)" />
          <div class="icon-picker">
            <div
              v-for="icon in availableIcons"
              :key="icon"
              class="icon-tile"
              :class="{ selected: form.icon === icon }"
              @click="() => (form.icon = icon)"
              role="button"
              tabindex="0"
            >
              <IconRenderer :icon="icon" />
              <div class="icon-label">{{ icon }}</div>
            </div>
          </div>
        </el-form-item>
        <div style="margin-top:8px; display:flex; align-items:center; gap:12px;">
          <a href="https://materialdesignicons.com/cdn/1.6.50-dev/" target="_blank" rel="noreferrer">Icons</a>
          <el-button size="small" type="primary" @click="downloadDialogVisible = true">从 MDI 下载图标</el-button>
        </div>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="downloadDialogVisible" title="从 MDI 下载图标" width="520px">
      <div>
        <p>输入 MDI 名称（用空格或逗号分隔），例如 <code>football</code> 或 <code>account star</code>。</p>
        <el-input v-model="downloadNames" placeholder="例如: football star" clearable />
        <p style="margin-top:8px">操作：在项目 `frontend` 目录运行以下命令将把 SVG 下载到 <code>public/mdi/</code>：</p>
        <el-input type="textarea" :rows="2" :model-value="fetchCommand()" readonly />
      </div>
      <template #footer>
        <el-button @click="downloadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="copyFetchCommand">复制命令</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { createTag, updateTag, deleteTag, type TagNode } from '@/api/dict'
import { useDictionaries } from '@/composables/useDictionaries'
import IconRenderer from '@/components/IconRenderer.vue'

const { tagTree, tagMap, refresh: refreshDicts, load: loadDicts } = useDictionaries()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const treeRef = ref()
const parentLabel = ref('根标签')
const filterText = ref('')

const form = reactive({
  id: 0,
  name: '',
  parentId: null as number | null,
  color: '',
  icon: '',
  sort: 0
})

const rules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

// 将可能的 rgb/rgba 颜色字符串转换为十六进制（#rrggbb）
const normalizeColorToHex = (color?: string | null) => {
  if (!color) return ''
  const s = color.trim()
  if (s.startsWith('#')) return s
  // match rgb(...) or rgba(...)
  const m = s.match(/rgba?\s*\(([^)]+)\)/i)
  if (m) {
    const parts = m[1].split(',').map((p) => p.trim())
    const r = Number(parts[0]) || 0
    const g = Number(parts[1]) || 0
    const b = Number(parts[2]) || 0
    // ignore alpha for hex representation
    const toHex = (n: number) => (n < 16 ? '0' : '') + n.toString(16)
    return `#${toHex(r)}${toHex(g)}${toHex(b)}`.toLowerCase()
  }
  // fallback: return original string
  return s
}

const reset = () => {
  form.id = 0
  form.name = ''
  form.parentId = null
  form.color = ''
  form.icon = ''
  form.sort = nextSort(tagTree.value)
  parentLabel.value = '根标签'
}

const downloadDialogVisible = ref(false)
const downloadNames = ref('')
const fetchCommand = () => `npm run fetch-mdi -- ${downloadNames.value.trim()}`
const copyFetchCommand = async () => {
  try {
    await navigator.clipboard.writeText(fetchCommand())
    ElMessage.success('命令已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const availableIcons = [
  'mdi-account',
  'mdi-star',
  'mdi-heart',
  'mdi-bookmark',
  'mdi-tag',
  'mdi-folder',
  'mdi-cart',
  'mdi-search',
  'mdi-image',
  'mdi-user',
]

const handleFilter = (value: string) => {
  const tree = treeRef.value as any
  tree?.filter(value)
}

const filterNode = (value: string, data: TagNode) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

const nextSort = (nodes: TagNode[] = []) => nodes.reduce((max, node) => Math.max(max, node.sort ?? 0), 0) + 10

const openCreateRoot = () => {
  isEdit.value = false
  reset()
  dialogVisible.value = true
}

const openCreateChild = (node: TagNode) => {
  isEdit.value = false
  reset()
  form.parentId = node.id
  form.sort = nextSort(node.children || [])
  parentLabel.value = node.name
  dialogVisible.value = true
}

const openEdit = (node: TagNode) => {
  isEdit.value = true
  form.id = node.id
  form.name = node.name
  form.parentId = node.parentId ?? null
  form.color = normalizeColorToHex(node.color || '')
  form.icon = node.icon || ''
  form.sort = node.sort ?? 0
  parentLabel.value = node.parentId ? tagMap.value.get(node.parentId)?.name || '根标签' : '根标签'
  dialogVisible.value = true
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        name: form.name,
        parentId: form.parentId ?? null,
        color: normalizeColorToHex(form.color) || undefined,
        icon: form.icon || undefined,
        sort: form.sort
      }
      if (isEdit.value) {
        await updateTag(form.id, payload)
        ElMessage.success('标签已更新')
      } else {
        await createTag(payload)
        ElMessage.success('标签已创建')
      }
      dialogVisible.value = false
      await refreshDicts()
    } catch (err: any) {
      ElMessage.error(err.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

const removeTag = async (node: TagNode) => {
  try {
    await ElMessageBox.confirm(`确认删除“${node.name}”吗？`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  loading.value = true
  try {
    await deleteTag(node.id)
    ElMessage.success('已删除标签')
    await refreshDicts()
  } catch (err: any) {
    ElMessage.error(err.message || '删除失败')
  } finally {
    loading.value = false
  }
}

const handleDrop = async (_dragging: any, dropNode: any, dropType: string) => {
  const tree = treeRef.value as any
  const siblings = dropType === 'inner'
    ? dropNode.childNodes
    : (dropNode.parent ? dropNode.parent.childNodes : tree?.root?.childNodes) || []
  loading.value = true
  try {
      for (let index = 0; index < siblings.length; index += 1) {
      const sibling = siblings[index]
      const data: TagNode = sibling.data
      const parentId = sibling.parent?.data?.id ?? null
      await updateTag(data.id, {
        name: data.name,
          parentId,
          color: normalizeColorToHex(data.color) || undefined,
        icon: data.icon || undefined,
        sort: index * 10
      })
    }
    ElMessage.success('排序已更新')
    await refreshDicts()
  } catch (err: any) {
    ElMessage.error(err.message || '排序更新失败')
  } finally {
    loading.value = false
  }
}

loadDicts()
reset()
watch(filterText, (value) => handleFilter(value))
</script>

<style scoped>
.tag-manager {
  background: var(--dl-card);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 16px;
  min-height: 360px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.toolbar-search {
  margin-left: auto;
  max-width: 240px;
}

.content {
  background: var(--dl-bg-alt);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  padding: 8px;
}

:deep(.el-tree) {
  background: transparent;
}

:deep(.el-tree-node__content) {
  border-radius: 8px;
  padding: 6px 10px;
}

:deep(.el-tree-node__content:hover) {
  background: rgba(148, 163, 184, 0.14);
}

.tag-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid var(--el-border-color-light);
}

.tag-icon {
  color: #fcd34d;
}

.node-actions {
  margin-left: auto;
  display: inline-flex;
  gap: 8px;
}

.icon-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.icon-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 56px;
  background: var(--dl-card);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  cursor: pointer;
  padding: 6px;
}
.icon-tile .tag-icon {
  font-size: 20px;
}
.tag-icon-svg {
  width: 20px;
  height: 20px;
  color: #fcd34d;
}
.icon-tile .icon-label {
  font-size: 11px;
  margin-top: 6px;
  color: var(--dl-muted);
  text-align: center;
  word-break: break-all;
}
.icon-tile.selected {
  border-color: var(--dl-accent);
  box-shadow: 0 0 0 2px var(--dl-accent-soft);
}
</style>
