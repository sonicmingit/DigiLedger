<template>
  <div class="upgrade-route-page">
    <el-card class="mb">
      <div class="actions">
        <div>
          <h2 class="title">装备升级路线图</h2>
          <p class="subtitle">以现有物品为节点，梳理多代升级路线并计算净投入</p>
        </div>
        <div class="action-buttons">
          <el-button type="primary" @click="openDialog()">
            <el-icon class="mr-1"><plus /></el-icon>新建路线
          </el-button>
          <el-button :loading="loading" @click="loadRoutes">刷新</el-button>
        </div>
      </div>
    </el-card>
    <el-card>
      <el-table :data="routes" stripe :loading="loading" empty-text="暂无路线" row-key="id">
        <el-table-column prop="name" label="路线名称" min-width="180" />
        <el-table-column prop="rootAssetName" label="起点装备" min-width="200">
          <template #default="{ row }">
            <span v-if="row.rootAssetName">{{ row.rootAssetName }} (ID: {{ row.rootAssetId }})</span>
            <span v-else class="text-muted">未指定</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220">
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="goGraph(row.id)">查看路线图</el-button>
            <el-button link type="info" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该路线？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑升级路线' : '新建升级路线'" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="路线名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：桌面主力电脑升级线" />
        </el-form-item>
        <el-form-item label="起点装备">
          <el-select
            v-model="form.rootAssetId"
            placeholder="可选"
            filterable
            remote
            reserve-keyword
            clearable
            :remote-method="(keyword) => loadAssets(keyword as string)"
            :loading="assetLoading"
          >
            <el-option
              v-for="item in assetOptions"
              :key="item.id"
              :label="`${item.name}（${item.status}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="3" placeholder="补充路线规划或选择理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { fetchUpgradeRoutes, createUpgradeRoute, updateUpgradeRoute, deleteUpgradeRoute } from '@/api/upgrade'
import { fetchAssets } from '@/api/asset'
import type { AssetSummary, UpgradeRouteItem } from '@/types'

const router = useRouter()
const routes = ref<UpgradeRouteItem[]>([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const assetLoading = ref(false)
const assetOptions = ref<AssetSummary[]>([])
const formRef = ref<FormInstance>()

const form = reactive<{ id?: number | null; name: string; rootAssetId?: number | null; remark?: string | null }>(
  {
    id: null,
    name: '',
    rootAssetId: null,
    remark: ''
  }
)

const rules: FormRules = {
  name: [
    { required: true, message: '请输入路线名称', trigger: 'blur' },
    { min: 1, max: 200, message: '长度需在 1-200 字', trigger: 'blur' }
  ],
  remark: [{ max: 2000, message: '备注长度需在 2000 字以内', trigger: 'blur' }]
}

const loadRoutes = async () => {
  loading.value = true
  try {
    routes.value = await fetchUpgradeRoutes()
  } finally {
    loading.value = false
  }
}

const loadAssets = async (keyword?: string) => {
  assetLoading.value = true
  try {
    assetOptions.value = await fetchAssets({ keyword })
  } finally {
    assetLoading.value = false
  }
}

const openDialog = (route?: UpgradeRouteItem) => {
  form.id = route?.id ?? null
  form.name = route?.name ?? ''
  form.rootAssetId = route?.rootAssetId ?? null
  form.remark = route?.remark ?? ''
  visible.value = true
  loadAssets()
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.rootAssetId = null
  form.remark = ''
  formRef.value?.clearValidate()
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { name: form.name, rootAssetId: form.rootAssetId, remark: form.remark }
      if (form.id) {
        await updateUpgradeRoute(form.id, payload)
        ElMessage.success('更新成功')
      } else {
        await createUpgradeRoute(payload)
        ElMessage.success('创建成功')
      }
      visible.value = false
      await loadRoutes()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = async (id: number) => {
  await deleteUpgradeRoute(id)
  ElMessage.success('已删除')
  await loadRoutes()
}

const goGraph = (id: number) => {
  router.push({ path: `/upgrade-routes/${id}` })
}

onMounted(() => {
  loadRoutes()
})
</script>

<style scoped>
.upgrade-route-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  margin: 0;
  font-size: 20px;
  color: var(--color-text);
}

.subtitle {
  margin: 4px 0 0;
  color: var(--color-muted);
  font-size: 14px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.mb {
  margin-bottom: 8px;
}

.text-muted {
  color: var(--color-muted);
}
</style>
