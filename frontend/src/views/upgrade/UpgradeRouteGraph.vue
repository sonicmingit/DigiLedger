<template>
  <div class="upgrade-graph-page">
    <el-card class="mb">
      <div class="graph-header">
        <div>
          <h2 class="title">{{ graph?.routeName || '加载中…' }}</h2>
          <p class="subtitle">{{ graph?.remark || '围绕同一装备族群规划多代升级路线' }}</p>
          <p class="meta">共 {{ graph?.nodes.length || 0 }} 个节点 / {{ graph?.links.length || 0 }} 条升级关系</p>
        </div>
        <div class="actions">
          <el-button type="primary" @click="nodeDialogVisible = true">
            <el-icon class="mr-1"><plus /></el-icon>新增节点
          </el-button>
          <el-button type="success" :disabled="!graph?.nodes.length" @click="linkDialogVisible = true">
            <el-icon class="mr-1"><connection /></el-icon>新增升级关系
          </el-button>
          <el-button :loading="loading" @click="loadGraph">刷新</el-button>
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="18">
        <el-card class="graph-card" :loading="loading">
          <div v-if="levelList.length" class="graph-flow">
            <div v-for="level in levelList" :key="level.level" class="level-column">
              <div class="level-title">第 {{ level.level }} 代</div>
              <div class="node-list">
                <div v-for="node in level.nodes" :key="node.nodeId" class="node-card">
                  <div class="node-header">
                    <div class="node-title">{{ node.name }}</div>
                    <el-tag size="small" type="info">{{ node.status }}</el-tag>
                  </div>
                  <div class="node-body">
                    <el-image v-if="buildOssUrl(node.coverImageUrl)" :src="buildOssUrl(node.coverImageUrl)" fit="cover" class="node-cover" />
                    <div class="node-info">
                      <div>购入：¥ {{ formatNumber(node.purchasePrice) }}</div>
                      <div v-if="node.sold">售出：¥ {{ formatNumber(node.salePrice) }}</div>
                      <div v-else class="text-muted">未售出</div>
                      <div class="text-muted">节点标记：{{ node.label || '无' }}</div>
                    </div>
                  </div>
                  <div class="node-actions">
                    <el-button size="small" type="primary" link @click="goAsset(node.assetId)">物品详情</el-button>
                    <el-popconfirm title="确定删除该节点？相关升级关系会一并移除" @confirm="removeNode(node.nodeId)">
                      <template #reference>
                        <el-button size="small" type="danger" link>删除</el-button>
                      </template>
                    </el-popconfirm>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无节点，先添加一条装备吧" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="summary-card" :loading="loading">
          <div class="summary-header">升级关系</div>
          <el-table
            v-if="graph && graph.links.length"
            :data="graph.links"
            size="small"
            border
            :show-header="true"
            empty-text="暂无关系"
            height="480px"
          >
            <el-table-column label="前代" min-width="120">
              <template #default="{ row }">{{ nodeNameMap.get(row.fromNodeId) || '-' }}</template>
            </el-table-column>
            <el-table-column label="后代" min-width="120">
              <template #default="{ row }">{{ nodeNameMap.get(row.toNodeId) || '-' }}</template>
            </el-table-column>
            <el-table-column label="单步花费" width="120">
              <template #default="{ row }">
                <el-tag type="success">¥ {{ formatNumber(row.stepCost) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-popconfirm title="删除该关系？" @confirm="removeLink(row.linkId)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无升级关系" />
          <div class="summary-footer" v-if="graph && graph.links.length">
            <span>总步进花费：¥ {{ formatNumber(totalStepCost) }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="nodeDialogVisible" title="新增升级节点" width="520px" @closed="resetNodeForm">
      <el-form :model="nodeForm" :rules="nodeRules" ref="nodeFormRef" label-width="100px">
        <el-form-item label="关联物品" prop="assetId">
          <el-select
            v-model="nodeForm.assetId"
            placeholder="选择已有物品"
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
        <el-form-item label="层级" prop="level">
          <el-input-number v-model="nodeForm.level" :min="1" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="nodeForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="节点标签">
          <el-input v-model="nodeForm.label" placeholder="如：备选方案/跳级配置" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="nodeForm.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitNode">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="linkDialogVisible" title="新增升级关系" width="520px" @closed="resetLinkForm">
      <el-form :model="linkForm" :rules="linkRules" ref="linkFormRef" label-width="100px">
        <el-form-item label="前代节点" prop="fromNodeId">
          <el-select v-model="linkForm.fromNodeId" placeholder="选择前代节点">
            <el-option v-for="node in graph?.nodes || []" :key="node.nodeId" :label="node.name" :value="node.nodeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="后代节点" prop="toNodeId">
          <el-select v-model="linkForm.toNodeId" placeholder="选择后代节点">
            <el-option v-for="node in graph?.nodes || []" :key="node.nodeId" :label="node.name" :value="node.nodeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="linkForm.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitLink">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Connection } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  fetchUpgradeGraph,
  addUpgradeNode,
  addUpgradeLink,
  deleteUpgradeNode,
  deleteUpgradeLink
} from '@/api/upgrade'
import { fetchAssets } from '@/api/asset'
import type { AssetSummary, UpgradeRouteGraph } from '@/types'
import { buildOssUrl } from '@/utils/storage'

const route = useRoute()
const router = useRouter()
const routeId = Number(route.params.id)

const graph = ref<UpgradeRouteGraph | null>(null)
const loading = ref(false)
const saving = ref(false)
const assetLoading = ref(false)
const assetOptions = ref<AssetSummary[]>([])

const nodeDialogVisible = ref(false)
const linkDialogVisible = ref(false)

const nodeFormRef = ref<FormInstance>()
const linkFormRef = ref<FormInstance>()

const nodeForm = reactive({
  assetId: undefined as number | undefined,
  level: 1,
  sort: 0,
  label: '',
  remark: ''
})

const linkForm = reactive({
  fromNodeId: undefined as number | undefined,
  toNodeId: undefined as number | undefined,
  remark: ''
})

const nodeRules: FormRules = {
  assetId: [{ required: true, message: '请选择关联物品', trigger: 'change' }],
  level: [{ required: true, message: '请输入层级', trigger: 'change' }]
}

const linkRules: FormRules = {
  fromNodeId: [{ required: true, message: '请选择前代节点', trigger: 'change' }],
  toNodeId: [{ required: true, message: '请选择后代节点', trigger: 'change' }]
}

const levelList = computed(() => {
  if (!graph.value) return [] as Array<{ level: number; nodes: UpgradeRouteGraph['nodes'] }>
  const grouped = new Map<number, typeof graph.value.nodes>()
  graph.value.nodes.forEach((node) => {
    const lvl = node.level || 1
    if (!grouped.has(lvl)) {
      grouped.set(lvl, [])
    }
    grouped.get(lvl)?.push(node)
  })
  return Array.from(grouped.entries())
    .sort((a, b) => a[0] - b[0])
    .map(([level, nodes]) => ({ level, nodes: nodes.sort((a, b) => (a.sort || 0) - (b.sort || 0)) }))
})

const nodeNameMap = computed(() => {
  const map = new Map<number, string>()
  graph.value?.nodes.forEach((n) => map.set(n.nodeId, n.name))
  return map
})

const totalStepCost = computed(() => {
  if (!graph.value) return 0
  return graph.value.links.reduce((sum, item) => sum + Number(item.stepCost || 0), 0)
})

const formatNumber = (val?: number | string | null) => {
  const num = Number(val || 0)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const loadGraph = async () => {
  loading.value = true
  try {
    graph.value = await fetchUpgradeGraph(routeId)
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

const resetNodeForm = () => {
  nodeForm.assetId = undefined
  nodeForm.level = 1
  nodeForm.sort = 0
  nodeForm.label = ''
  nodeForm.remark = ''
  nodeFormRef.value?.clearValidate()
}

const resetLinkForm = () => {
  linkForm.fromNodeId = undefined
  linkForm.toNodeId = undefined
  linkForm.remark = ''
  linkFormRef.value?.clearValidate()
}

const submitNode = () => {
  nodeFormRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await addUpgradeNode(routeId, {
        assetId: nodeForm.assetId!,
        level: nodeForm.level,
        sort: nodeForm.sort,
        label: nodeForm.label,
        remark: nodeForm.remark
      })
      ElMessage.success('新增节点成功')
      nodeDialogVisible.value = false
      await loadGraph()
    } finally {
      saving.value = false
    }
  })
}

const submitLink = () => {
  linkFormRef.value?.validate(async (valid) => {
    if (!valid) return
    if (linkForm.fromNodeId === linkForm.toNodeId) {
      ElMessage.warning('前后代节点不能相同')
      return
    }
    saving.value = true
    try {
      await addUpgradeLink(routeId, {
        fromNodeId: linkForm.fromNodeId!,
        toNodeId: linkForm.toNodeId!,
        remark: linkForm.remark
      })
      ElMessage.success('新增关系成功')
      linkDialogVisible.value = false
      await loadGraph()
    } finally {
      saving.value = false
    }
  })
}

const removeNode = async (nodeId: number) => {
  await deleteUpgradeNode(routeId, nodeId)
  ElMessage.success('节点已删除')
  await loadGraph()
}

const removeLink = async (linkId: number) => {
  await deleteUpgradeLink(routeId, linkId)
  ElMessage.success('关系已删除')
  await loadGraph()
}

const goAsset = (assetId: number) => {
  router.push({ path: `/assets/${assetId}` })
}

const goBack = () => {
  router.push({ path: '/upgrade-routes' })
}

onMounted(() => {
  loadGraph()
  loadAssets()
})
</script>

<style scoped>
.upgrade-graph-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  margin: 0;
  font-size: 20px;
}

.subtitle {
  margin: 4px 0 0;
  color: var(--dl-muted);
}

.meta {
  margin: 4px 0 0;
  color: var(--dl-muted);
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 8px;
}

.graph-card {
  min-height: 480px;
}

.graph-flow {
  display: flex;
  gap: 12px;
  align-items: stretch;
  overflow-x: auto;
  padding-bottom: 8px;
}

.level-column {
  background: var(--dl-bg);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dl-radius-md);
  padding: 12px;
  min-width: 240px;
}

.level-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.node-card {
  background: var(--dl-card);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dl-radius-md);
  padding: 10px;
  box-shadow: var(--dl-shadow-sm);
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.node-title {
  font-weight: 600;
  font-size: 15px;
}

.node-body {
  display: flex;
  gap: 10px;
  align-items: center;
}

.node-cover {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  background: var(--dl-bg-alt);
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}

.text-muted {
  color: var(--dl-muted);
}

.node-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 6px;
}

.summary-card .summary-header {
  font-weight: 600;
  margin-bottom: 10px;
}

.summary-footer {
  margin-top: 12px;
  text-align: right;
  font-weight: 600;
}
</style>
