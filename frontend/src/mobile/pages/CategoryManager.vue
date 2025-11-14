<template>
  <div class="mobile-scroll">
    <header class="mobile-topbar">
      <div class="mobile-topbar-header">
        <button type="button" class="icon-btn" @click="goBack">←</button>
        <h1>分类管理</h1>
        <span></span>
      </div>
    </header>

    <section class="mobile-card-section">
      <button type="button" class="create-btn" @click="create">创建分类</button>
      <div class="mobile-category-list">
        <div
          v-for="(item, index) in categories"
          :key="item.id"
          class="mobile-category-item"
          draggable="true"
          @dragstart="onDragStart(index)"
          @dragover.prevent
          @drop="onDrop(index)"
        >
          <span>{{ item.name }}</span>
          <div class="mobile-category-actions">
            <button type="button" @click="edit(item)">编辑</button>
            <button type="button" @click="remove(item)">删除</button>
            <span class="drag-handle">☰</span>
          </div>
        </div>
      </div>
    </section>

    <div v-if="toast" class="mobile-toast">{{ toast }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createCategory,
  deleteCategory,
  fetchCategoryTree,
  updateCategory,
  type CategoryNode
} from '@/api/dict'

const router = useRouter()
const categories = ref<CategoryNode[]>([])
const toast = ref('')
const draggingIndex = ref<number | null>(null)

const goBack = () => router.back()

const load = async () => {
  const data = await fetchCategoryTree()
  categories.value = data
}

const create = async () => {
  const name = prompt('请输入分类名称')
  if (!name) return
  try {
    await createCategory({ name })
    toast.value = '创建成功'
    await load()
  } catch (error) {
    toast.value = '创建失败，请稍后再试'
  } finally {
    setTimeout(() => (toast.value = ''), 1800)
  }
}

const edit = async (item: CategoryNode) => {
  const name = prompt('请输入新的分类名称', item.name)
  if (!name || name === item.name) return
  try {
    await updateCategory(item.id, { name, parentId: item.parentId, sort: item.sort })
    toast.value = '已更新'
    await load()
  } catch (error) {
    toast.value = '更新失败'
  } finally {
    setTimeout(() => (toast.value = ''), 1800)
  }
}

const remove = async (item: CategoryNode) => {
  if (!confirm(`确定删除分类「${item.name}」吗？`)) return
  try {
    await deleteCategory(item.id)
    toast.value = '已删除'
    await load()
  } catch (error) {
    toast.value = '删除失败，请先清理关联资产'
  } finally {
    setTimeout(() => (toast.value = ''), 1800)
  }
}

const onDragStart = (index: number) => {
  draggingIndex.value = index
}

const onDrop = async (index: number) => {
  if (draggingIndex.value === null) return
  const list = [...categories.value]
  const [moved] = list.splice(draggingIndex.value, 1)
  list.splice(index, 0, moved)
  categories.value = list
  draggingIndex.value = null
  await persistOrder()
}

const persistOrder = async () => {
  try {
    await Promise.all(
      categories.value.map((item, order) =>
        updateCategory(item.id, { name: item.name, parentId: item.parentId, sort: order + 1 })
      )
    )
    toast.value = '排序已更新'
  } catch (error) {
    toast.value = '排序失败，请稍后重试'
  } finally {
    setTimeout(() => (toast.value = ''), 1800)
  }
}

onMounted(load)
</script>

<style scoped>
.create-btn {
  width: 100%;
  border-radius: 14px;
  border: none;
  padding: 12px 0;
  background: linear-gradient(120deg, var(--mobile-green-start), var(--mobile-green-end));
  color: #fff;
  margin-bottom: 12px;
}

.drag-handle {
  cursor: grab;
}
</style>
