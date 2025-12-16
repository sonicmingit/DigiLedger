<template>
  <div class="category-manager" v-loading="loading">
    <div class="toolbar">
      <el-input
        v-model="filterText"
        placeholder="搜索分类"
        clearable
        size="small"
        class="toolbar-search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div class="toolbar-actions">
        <el-button text size="small" @click="expandAll">展开全部</el-button>
        <el-button text size="small" @click="collapseAll">折叠全部</el-button>
        <el-button text size="small" @click="refreshDicts">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="addRoot">
          <el-icon class="mr-1"><Plus /></el-icon>
          新增根类别
        </el-button>
      </div>
    </div>
    <el-tree
      ref="treeRef"
      :data="categoryTree"
      node-key="id"
      default-expand-all
      draggable
      :props="{ children: 'children', label: 'name' }"
      :filter-node-method="filterNode"
      empty-text="暂无类别"
      @node-drop="handleDrop"
    >
      <template #default="{ data }">
        <div class="node-row">
          <span class="node-label">{{ data.name }}</span>
          <span class="node-actions">
            <el-tooltip content="新增子类" placement="top">
              <el-button circle text type="success" @click.stop="addChild(data)">
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="重命名" placement="top">
              <el-button circle text @click.stop="editNode(data)">
                <el-icon><EditPen /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button circle text type="danger" @click.stop="removeNode(data)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </span>
        </div>
      </template>
    </el-tree>
    <CategoryCreateDialog
      v-model="createDialogVisible"
      :default-parent-id="createDialogParentId"
      @success="handleCreateSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, EditPen, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { updateCategory, deleteCategory, type CategoryNode } from '@/api/dict'
import { useDictionaries } from '@/composables/useDictionaries'
import CategoryCreateDialog from '@/components/CategoryCreateDialog.vue'

const { categoryTree, refresh: refreshDicts, load: loadDicts } = useDictionaries()
const loading = ref(false)
const treeRef = ref()
const createDialogVisible = ref(false)
const createDialogParentId = ref<number | null>(null)
const filterText = ref('')

const promptName = async (title: string, defaultValue = ''): Promise<string | null> => {
  try {
    const { value } = await ElMessageBox.prompt('请输入类别名称', title, {
      inputValue: defaultValue,
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (val: string) => (val && val.trim().length ? true : '名称不能为空')
    })
    return value.trim()
  } catch {
    return null
  }
}

const handleFilter = (value: string) => {
  const tree = treeRef.value as any
  tree?.filter(value)
}

const filterNode = (value: string, data: CategoryNode) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

const openCreateDialog = (parentId: number | null) => {
  createDialogParentId.value = parentId
  createDialogVisible.value = true
}

const toggleAll = (expand: boolean) => {
  const tree = treeRef.value as any
  if (!tree?.store?.nodesMap) return
  Object.values(tree.store.nodesMap).forEach((node: any) => {
    node.expanded = expand
  })
}

const expandAll = () => toggleAll(true)
const collapseAll = () => toggleAll(false)

const addRoot = () => {
  openCreateDialog(null)
}

const addChild = (node: CategoryNode) => {
  openCreateDialog(node.id)
}

const editNode = async (node: CategoryNode) => {
  const name = await promptName('重命名类别', node.name)
  if (!name || name === node.name) return
  loading.value = true
  try {
    await updateCategory(node.id, { name, parentId: node.parentId ?? null, sort: node.sort })
    ElMessage.success('类别已更新')
    await refreshDicts()
  } catch (err: any) {
    ElMessage.error(err.message || '更新失败')
  } finally {
    loading.value = false
  }
}

const removeNode = async (node: CategoryNode) => {
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
    await deleteCategory(node.id)
    ElMessage.success('已删除类别')
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
      const data: CategoryNode = sibling.data
      const parentId = sibling.parent?.data?.id ?? null
      await updateCategory(data.id, {
        name: data.name,
        parentId,
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

const handleCreateSuccess = (payload: { id: number }) => {
  nextTick(() => {
    const tree = treeRef.value as any
    if (!tree) return
    tree.setCurrentKey(payload.id)
    const current = tree.getNode?.(payload.id)
    let parent = current?.parent
    while (parent && parent !== tree.root) {
      parent.expanded = true
      parent = parent.parent
    }
  })
}

loadDicts()

watch(filterText, (value) => handleFilter(value))
</script>

<style scoped>
.category-manager {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 16px;
  min-height: 360px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.toolbar-search {
  max-width: 240px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.toolbar-actions :deep(.el-button--primary) {
  border-radius: 10px;
}

.node-label {
  font-weight: 500;
}

.node-actions {
  display: inline-flex;
  gap: 6px;
}

.node-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  border-radius: 10px;
  transition: background-color 0.2s ease;
}

.node-row:hover {
  background: #ecfdf3;
}
</style>
