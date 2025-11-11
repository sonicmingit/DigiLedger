<template>
  <div class="category-manager" v-loading="loading">
    <div class="toolbar">
      <el-button type="primary" @click="addRoot">新增根类别</el-button>
      <el-button @click="refreshDicts">刷新</el-button>
    </div>
    <el-tree
      ref="treeRef"
      :data="categoryTree"
      node-key="id"
      default-expand-all
      draggable
      :props="{ children: 'children', label: 'name' }"
      empty-text="暂无类别"
      @node-drop="handleDrop"
    >
      <template #default="{ data }">
        <span class="node-label">{{ data.name }}</span>
        <span class="node-actions">
          <el-button link type="primary" @click.stop="addChild(data)">新增子类</el-button>
          <el-button link @click.stop="editNode(data)">重命名</el-button>
          <el-button link type="danger" @click.stop="removeNode(data)">删除</el-button>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCategory, updateCategory, deleteCategory, type CategoryNode } from '@/api/dict'
import { useDictionaries } from '@/composables/useDictionaries'

const { categoryTree, refresh: refreshDicts, load: loadDicts } = useDictionaries()
const loading = ref(false)
const treeRef = ref()

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

const nextSort = (nodes: CategoryNode[] = []) => nodes.reduce((max, item) => Math.max(max, item.sort ?? 0), 0) + 10

const addRoot = async () => {
  const name = await promptName('新增根类别')
  if (!name) return
  loading.value = true
  try {
    await createCategory({ name, parentId: null, sort: nextSort(categoryTree.value) })
    ElMessage.success('已创建类别')
    await refreshDicts()
  } catch (err: any) {
    ElMessage.error(err.message || '创建失败')
  } finally {
    loading.value = false
  }
}

const addChild = async (node: CategoryNode) => {
  const name = await promptName('新增子类别')
  if (!name) return
  loading.value = true
  try {
    await createCategory({ name, parentId: node.id, sort: nextSort(node.children || []) })
    ElMessage.success('已创建子类别')
    await refreshDicts()
  } catch (err: any) {
    ElMessage.error(err.message || '创建失败')
  } finally {
    loading.value = false
  }
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

loadDicts()
</script>

<style scoped>
.category-manager {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 16px;
  min-height: 360px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.node-label {
  font-weight: 500;
}

.node-actions {
  margin-left: 12px;
  display: inline-flex;
  gap: 8px;
}
</style>
