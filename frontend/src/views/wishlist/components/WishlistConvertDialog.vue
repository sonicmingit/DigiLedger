<template>
  <el-dialog v-model="visible" title="转为物品" width="560px" @closed="reset">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" status-icon>
      <el-form-item label="物品名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入物品名称" />
      </el-form-item>
      <el-form-item label="物品类别" prop="categoryId">
        <el-tree-select
          v-model="form.categoryId"
          :data="categoryOptions"
          :props="treeProps"
          check-strictly
          filterable
          placeholder="请选择类别（叶子节点）"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="品牌">
        <el-select
          v-model="form.brand"
          filterable
          allow-create
          default-first-option
          placeholder="选择或输入品牌"
          style="width: 100%"
        >
          <el-option v-for="item in brandOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="型号">
        <el-input v-model="form.model" placeholder="型号（可选）" />
      </el-form-item>
      <el-form-item label="启用日期" prop="enabledDate">
        <el-date-picker v-model="form.enabledDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="标签">
        <el-tree-select
          v-model="form.tagIds"
          :data="tagOptions"
          :props="treeProps"
          multiple
          show-checkbox
          collapse-tags
          filterable
          placeholder="选择标签"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" rows="3" placeholder="可补充说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">创建物品</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { convertWishlist } from '@/api/wishlist'
import type { WishlistItem } from '@/types'
import type { CategoryNode, TagNode } from '@/api/dict'
import { useDictionaries } from '@/composables/useDictionaries'
import { extractObjectKey } from '@/utils/storage'

const emit = defineEmits<{ (e: 'success'): void }>()

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const current = ref<WishlistItem | null>(null)

const today = () => new Date().toISOString().slice(0, 10)

const form = reactive({
  name: '',
  categoryId: null as number | null,
  brand: '',
  model: '',
  enabledDate: today(),
  tagIds: [] as number[],
  notes: '',
  imageKey: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类别', trigger: 'change' }],
  enabledDate: [{ required: true, message: '请选择启用日期', trigger: 'change' }]
}

const { load: loadDicts, categoryTree, tagTree } = useDictionaries()
const brandOptions = ref<string[]>([])

const ensureBrandOption = (value: string) => {
  if (!value) return
  if (!brandOptions.value.includes(value)) {
    brandOptions.value.push(value)
  }
}

const treeProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  disabled: 'disabled'
}

const buildCategoryOptions = (nodes: CategoryNode[]): any[] =>
  nodes.map((node) => ({
    value: node.id,
    label: node.name,
    disabled: node.children?.length ? true : false,
    children: node.children ? buildCategoryOptions(node.children) : []
  }))

const buildTagOptions = (nodes: TagNode[]): any[] =>
  nodes.map((node) => ({
    value: node.id,
    label: node.name,
    children: node.children ? buildTagOptions(node.children) : []
  }))

const categoryOptions = computed(() => buildCategoryOptions(categoryTree.value))
const tagOptions = computed(() => buildTagOptions(tagTree.value))

const open = (item: WishlistItem) => {
  current.value = item
  form.name = item.name
  form.brand = item.brand || ''
  ensureBrandOption(form.brand)
  form.model = item.model || ''
  form.notes = item.notes || ''
  form.categoryId = null
  form.tagIds = []
  form.enabledDate = today()
  form.imageKey = extractObjectKey(item.imageUrl)
  visible.value = true
}

const reset = () => {
  current.value = null
  form.name = ''
  form.brand = ''
  form.model = ''
  form.notes = ''
  form.categoryId = null
  form.tagIds = []
  form.enabledDate = today()
  form.imageKey = ''
}

const submit = () => {
  if (!current.value) return
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await convertWishlist(current.value!.id, {
        name: form.name,
        categoryId: form.categoryId!,
        brand: form.brand || undefined,
        model: form.model || undefined,
        status: '使用中',
        enabledDate: form.enabledDate,
        notes: form.notes || undefined,
        tagIds: form.tagIds,
        coverImageUrl: form.imageKey || undefined,
        purchases: []
      })
      ElMessage.success('已转为物品')
      visible.value = false
      emit('success')
    } finally {
      loading.value = false
    }
  })
}

defineExpose({ open })

onMounted(async () => {
  await loadDicts()
})
</script>

<style scoped>
</style>
