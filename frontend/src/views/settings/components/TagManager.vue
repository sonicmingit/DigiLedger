<template>
  <div class="tag-manager" v-loading="loading">
    <div class="toolbar">
      <el-button type="primary" @click="openCreateRoot">新增根标签</el-button>
      <el-button @click="refreshDicts">刷新</el-button>
    </div>
    <el-tree
      ref="treeRef"
      :data="tagTree"
      node-key="id"
      default-expand-all
      draggable
      :props="{ children: 'children', label: 'name' }"
      empty-text="暂无标签"
      @node-drop="handleDrop"
    >
      <template #default="{ data }">
        <span class="tag-node">
          <span v-if="data.color" class="color-dot" :style="{ backgroundColor: data.color }"></span>
          <i v-if="data.icon" :class="['tag-icon', data.icon]" />
          <span class="name">{{ data.name }}</span>
        </span>
        <span class="node-actions">
          <el-button link type="primary" @click.stop="openCreateChild(data)">新增子标签</el-button>
          <el-button link @click.stop="openEdit(data)">编辑</el-button>
          <el-button link type="danger" @click.stop="removeTag(data)">删除</el-button>
        </span>
      </template>
    </el-tree>
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
          <el-input v-model="form.icon" placeholder="如 ri-star-fill" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { createTag, updateTag, deleteTag, type TagNode } from '@/api/dict'
import { useDictionaries } from '@/composables/useDictionaries'

const { tagTree, tagMap, refresh: refreshDicts, load: loadDicts } = useDictionaries()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const treeRef = ref()
const parentLabel = ref('根标签')

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

const reset = () => {
  form.id = 0
  form.name = ''
  form.parentId = null
  form.color = ''
  form.icon = ''
  form.sort = nextSort(tagTree.value)
  parentLabel.value = '根标签'
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
  form.color = node.color || ''
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
        color: form.color || undefined,
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
        color: data.color || undefined,
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
</script>

<style scoped>
.tag-manager {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
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
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.tag-icon {
  color: #fcd34d;
}

.node-actions {
  margin-left: 12px;
  display: inline-flex;
  gap: 8px;
}
</style>
