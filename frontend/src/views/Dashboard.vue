<template>
  <div class="dashboard">
    <header class="hero">
      <div class="hero-text">
        <h1>DigiLedger 数码资产管家</h1>
        <p>围绕购买、维护与售出全流程，帮助你掌控设备价值与成本。</p>
        <div class="hero-actions">
          <el-button type="primary" @click="openCreateAsset">新建资产</el-button>
          <el-button @click="loadAssets" :loading="loading">刷新数据</el-button>
        </div>
      </div>
      <img src="/hero.svg" alt="hero" />
    </header>

    <section class="overview" v-if="assets.length">
      <el-card class="metric-card">
        <div class="metric-value">{{ totalAssets }}</div>
        <div class="metric-label">资产总数</div>
      </el-card>
      <el-card class="metric-card">
        <div class="metric-value">¥ {{ formatCurrency(totalDepreciation) }}</div>
        <div class="metric-label">折旧总额</div>
      </el-card>
      <el-card class="metric-card">
        <div class="metric-value">¥ {{ formatCurrency(totalAvgCostPerDay) }}</div>
        <div class="metric-label">日均成本总和</div>
      </el-card>
    </section>

    <section v-if="assets.length" class="summary">
      <h2 class="section-title">资产速览</h2>
      <div class="summary-grid">
        <asset-summary-card v-for="item in assets" :key="item.id" :asset="item" />
      </div>
    </section>

    <el-card v-if="assets.length" class="asset-table-card">
      <template #header>
        <div class="card-header">
          <h2 class="section-title">资产列表</h2>
          <div class="card-actions">
            <el-button type="primary" size="small" @click="openCreateAsset">新建资产</el-button>
            <el-button size="small" @click="loadAssets" :loading="loading">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="assets"
        border
        stripe
        :row-class-name="rowClassName"
        @row-click="handleRowClick"
        height="360"
      >
        <el-table-column type="index" width="60" label="#" />
        <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="产品型号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="purchaseDate" label="购买日期" width="120" />
        <el-table-column prop="bookValue" label="账面价值" width="140">
          <template #default="{ row }">¥ {{ formatCurrency(row.bookValue) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" @click.stop="selectAsset(row)">查看</el-button>
            <el-button type="primary" size="small" @click.stop="openEditAsset(row)">编辑</el-button>
            <el-button type="danger" size="small" @click.stop="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-empty v-else description="暂无资产，点击“新建资产”开始记录" />

    <el-row v-if="assets.length" :gutter="20" class="details-row">
      <el-col :xs="24" :md="14">
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <h2 class="section-title">资产维护</h2>
              <div v-if="activeAsset" class="card-actions">
                <el-button size="small" @click="openMaintenanceForm">添加维护</el-button>
                <el-button size="small" type="warning" @click="openSaleForm">记录售出</el-button>
              </div>
            </div>
          </template>
          <div v-if="activeAsset" class="detail-body">
            <h3 class="block-title">折旧详情</h3>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="当前折旧">¥ {{ formatCurrency(activeAsset.accumulatedDepreciation) }}</el-descriptions-item>
              <el-descriptions-item label="剩余价值">¥ {{ formatCurrency(activeAsset.bookValue) }}</el-descriptions-item>
              <el-descriptions-item label="启用天数">{{ activeAsset.useDays }} 天</el-descriptions-item>
              <el-descriptions-item label="年折旧率">{{ formatPercent(activeAsset.annualRate) }}</el-descriptions-item>
              <el-descriptions-item label="日均成本">¥ {{ formatCurrency(activeAsset.avgCostPerDay) }}</el-descriptions-item>
              <el-descriptions-item label="心愿状态" v-if="activeAsset.wishStatus">{{ wishStatusLabel(activeAsset.wishStatus) }}</el-descriptions-item>
            </el-descriptions>

            <h3 class="block-title">售出记录</h3>
            <el-table :data="activeAsset.saleRecords" size="small" empty-text="暂无售出记录" border>
              <el-table-column prop="saleDate" label="售出日期" width="120" />
              <el-table-column prop="platform" label="平台" width="120" />
              <el-table-column prop="salePrice" label="售出价格" width="120">
                <template #default="{ row }">¥ {{ formatCurrency(row.salePrice) }}</template>
              </el-table-column>
              <el-table-column prop="netIncome" label="净收入" width="120">
                <template #default="{ row }">¥ {{ formatCurrency(row.netIncome) }}</template>
              </el-table-column>
              <el-table-column prop="realizedPnl" label="盈亏" width="120">
                <template #default="{ row }">
                  <span :class="{ profit: row.realizedPnl >= 0, loss: row.realizedPnl < 0 }">
                    ¥ {{ formatCurrency(row.realizedPnl) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>

            <h3 class="block-title">维护记录</h3>
            <el-table :data="activeAsset.maintenanceRecords" size="small" empty-text="暂无维护记录" border>
              <el-table-column prop="date" label="维护日期" width="120" />
              <el-table-column prop="title" label="内容" min-width="160" show-overflow-tooltip />
              <el-table-column prop="type" label="类型" width="120" />
              <el-table-column prop="cost" label="费用" width="120">
                <template #default="{ row }">¥ {{ formatCurrency(row.cost) }}</template>
              </el-table-column>
              <el-table-column prop="notes" label="备注" min-width="160" show-overflow-tooltip />
            </el-table>

            <h3 class="block-title">产品信息</h3>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="名称">{{ activeAsset.name }}</el-descriptions-item>
              <el-descriptions-item label="分类">{{ activeAsset.category }}</el-descriptions-item>
              <el-descriptions-item label="品牌" v-if="activeAsset.brand">{{ activeAsset.brand }}</el-descriptions-item>
              <el-descriptions-item label="型号" v-if="activeAsset.model">{{ activeAsset.model }}</el-descriptions-item>
              <el-descriptions-item label="序列号" v-if="activeAsset.serialNo">{{ activeAsset.serialNo }}</el-descriptions-item>
              <el-descriptions-item label="启用日期">{{ activeAsset.enabledDate }}</el-descriptions-item>
              <el-descriptions-item label="购买平台" v-if="activeAsset.purchasePlatform">{{ activeAsset.purchasePlatform }}</el-descriptions-item>
              <el-descriptions-item label="保修渠道" v-if="activeAsset.warrantyChannel">{{ activeAsset.warrantyChannel }}</el-descriptions-item>
              <el-descriptions-item label="保修到期" v-if="activeAsset.warrantyExpireDate">{{ activeAsset.warrantyExpireDate }}</el-descriptions-item>
              <el-descriptions-item label="标签" v-if="activeAsset.tags?.length">
                <el-tag v-for="tag in activeAsset.tags" :key="tag" size="small" class="tag">{{ tag }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="关联设备" v-if="relatedAssetNames.length">
                <el-tag v-for="name in relatedAssetNames" :key="name" size="small" class="tag">{{ name }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="购买凭证" v-if="activeAsset.purchaseEvidence">
                <a :href="activeAsset.purchaseEvidence" target="_blank" rel="noopener">查看附件</a>
              </el-descriptions-item>
            </el-descriptions>

            <el-alert
              v-if="activeAsset.notes"
              :title="activeAsset.notes"
              type="info"
              :closable="false"
              class="notes"
            />
          </div>
          <el-empty v-else description="从左侧列表选择资产查看维护信息" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card class="purchase-card">
          <template #header>
            <div class="card-header">
              <h2 class="section-title">资产购置</h2>
              <el-button v-if="activeAsset" size="small" type="primary" @click="openPurchaseForm">追加配件</el-button>
            </div>
          </template>
          <div v-if="activeAsset">
            <el-table :data="activeAsset.purchases" size="small" empty-text="暂无购买记录" border>
              <el-table-column prop="type" label="类型" width="110">
                <template #default="{ row }">{{ purchaseTypeLabel(row.type) }}</template>
              </el-table-column>
              <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="platform" label="平台" width="120" show-overflow-tooltip />
              <el-table-column prop="purchaseDate" label="购买日期" width="120" />
              <el-table-column prop="price" label="价格" width="120">
                <template #default="{ row }">¥ {{ formatCurrency(row.price) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="选择资产后可查看购买记录" />
        </el-card>

        <el-card class="reminder-card">
          <template #header>
            <div class="card-header">
              <h2 class="section-title">小提醒</h2>
            </div>
          </template>
          <el-empty v-if="!reminders.length" description="暂无需要关注的事项" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="item in reminders"
              :key="`${item.type}-${item.assetId}-${item.dueDate}`"
              :type="item.daysLeft <= 7 ? 'danger' : item.daysLeft <= 30 ? 'warning' : 'primary'"
              :timestamp="`${item.dueDate} · 剩余 ${item.daysLeft} 天`"
            >
              <div class="reminder-item">
                <strong>{{ item.name }}</strong>
                <p>{{ item.message }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="assetDialogVisible" :title="assetFormMode === 'create' ? '新建资产' : '编辑资产'" width="640px">
      <el-form ref="assetFormRef" :model="assetForm" :rules="assetFormRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="assetForm.name" placeholder="请输入资产名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-input v-model="assetForm.category" placeholder="如：笔记本电脑" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="assetForm.brand" placeholder="如：Apple" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="assetForm.model" placeholder="如：MacBook Pro 14" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序列号">
              <el-input v-model="assetForm.serialNo" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="assetForm.status" placeholder="选择状态">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="心愿状态">
              <el-select v-model="assetForm.wishStatus" placeholder="选择心愿状态" clearable>
                <el-option v-for="item in wishStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购买平台">
              <el-input v-model="assetForm.purchasePlatform" placeholder="如：京东" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购买日期" prop="purchaseDate">
              <el-date-picker v-model="assetForm.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用日期" prop="enabledDate">
              <el-date-picker v-model="assetForm.enabledDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="assetForm.price" :min="0" :precision="2" :step="100" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运费">
              <el-input-number v-model="assetForm.shippingCost" :min="0" :precision="2" :step="10" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年折旧率" prop="annualRate">
              <el-input-number v-model="assetForm.annualRate" :min="0" :max="1" :step="0.05" :precision="2" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保修渠道">
              <el-input v-model="assetForm.warrantyChannel" placeholder="如：Apple Care" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保修到期">
              <el-date-picker v-model="assetForm.warrantyExpireDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联设备">
              <el-select v-model="assetForm.relatedAssets" multiple filterable clearable placeholder="选择关联资产">
                <el-option
                  v-for="item in relatedAssetOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签">
              <el-select v-model="assetForm.tags" multiple filterable allow-create default-first-option placeholder="添加标签">
                <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="购买凭证">
              <el-input v-model="assetForm.purchaseEvidence" placeholder="填写发票/截图链接" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="assetForm.notes" type="textarea" :rows="3" placeholder="补充说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="assetDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAssetForm">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="purchaseDialogVisible" title="追加购买记录" width="520px">
      <el-form ref="purchaseFormRef" :model="purchaseForm" :rules="purchaseFormRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="purchaseForm.name" placeholder="如：Apple Pencil" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="purchaseForm.type" placeholder="选择类型">
            <el-option label="主购买" value="PRIMARY" />
            <el-option label="配件" value="ACCESSORY" />
            <el-option label="服务" value="SERVICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台">
          <el-input v-model="purchaseForm.platform" placeholder="如：天猫" />
        </el-form-item>
        <el-form-item label="购买日期" prop="purchaseDate">
          <el-date-picker v-model="purchaseForm.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="purchaseForm.price" :min="0" :precision="2" :step="50" controls-position="right" class="full-width" />
        </el-form-item>
        <el-form-item label="运费">
          <el-input-number v-model="purchaseForm.shippingCost" :min="0" :precision="2" :step="10" controls-position="right" class="full-width" />
        </el-form-item>
        <el-form-item label="保修到期">
          <el-date-picker v-model="purchaseForm.warrantyExpireDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="保修渠道">
          <el-input v-model="purchaseForm.warrantyChannel" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="purchaseForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="purchaseDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitPurchaseForm">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="maintenanceDialogVisible" title="添加维护记录" width="520px">
      <el-form ref="maintenanceFormRef" :model="maintenanceForm" :rules="maintenanceFormRules" label-width="100px">
        <el-form-item label="维护项目" prop="title">
          <el-input v-model="maintenanceForm.title" placeholder="如：更换电池" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="maintenanceForm.type" placeholder="如：维修/配件" />
        </el-form-item>
        <el-form-item label="费用" prop="cost">
          <el-input-number v-model="maintenanceForm.cost" :min="0" :precision="2" :step="50" controls-position="right" class="full-width" />
        </el-form-item>
        <el-form-item label="维护日期" prop="date">
          <el-date-picker v-model="maintenanceForm.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="maintenanceForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="maintenanceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitMaintenanceForm">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="saleDialogVisible" title="记录售出" width="520px">
      <el-form ref="saleFormRef" :model="saleForm" :rules="saleFormRules" label-width="100px">
        <el-form-item label="售出平台">
          <el-input v-model="saleForm.platform" placeholder="如：闲鱼" />
        </el-form-item>
        <el-form-item label="售出日期" prop="saleDate">
          <el-date-picker v-model="saleForm.saleDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="售出价格" prop="salePrice">
          <el-input-number v-model="saleForm.salePrice" :min="0" :precision="2" :step="100" controls-position="right" class="full-width" />
        </el-form-item>
        <el-form-item label="平台费用">
          <el-input-number v-model="saleForm.fee" :min="0" :precision="2" :step="10" controls-position="right" class="full-width" />
        </el-form-item>
        <el-form-item label="运费">
          <el-input-number v-model="saleForm.shippingCost" :min="0" :precision="2" :step="10" controls-position="right" class="full-width" />
        </el-form-item>
        <el-form-item label="其他成本">
          <el-input-number v-model="saleForm.otherCost" :min="0" :precision="2" :step="10" controls-position="right" class="full-width" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="saleForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="saleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitSaleForm">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import http from '@/api/http'
import AssetSummaryCard from '@/components/AssetSummaryCard.vue'
import type {
  AssetStatus,
  AssetSummary,
  MaintenanceRecord,
  PurchaseRecord,
  PurchaseType,
  ReminderItem,
  SaleRecord
} from '@/types'

interface AssetForm {
  id?: number
  name: string
  category: string
  brand: string
  model: string
  serialNo: string
  status: AssetStatus
  wishStatus: string
  purchasePlatform: string
  purchaseDate: string
  enabledDate: string
  annualRate: number
  price: number
  shippingCost: number
  warrantyChannel: string
  warrantyExpireDate: string
  purchaseEvidence: string
  tags: string[]
  notes: string
  relatedAssets: number[]
}

interface PurchaseForm extends Omit<PurchaseRecord, 'id' | 'assetId'> {
  assetId?: number
}

interface MaintenanceFormState extends Omit<MaintenanceRecord, 'id' | 'assetId'> {
  assetId?: number
}

interface SaleFormState extends Omit<SaleRecord, 'id' | 'assetId' | 'netIncome' | 'realizedPnl'> {
  assetId?: number
}

// 仪表盘的核心状态：资产列表、加载状态与当前选中资产
const assets = ref<AssetSummary[]>([])
const loading = ref(false)
const activeAssetId = ref<number | null>(null)

// 弹窗与表单引用，用于控制 CRUD 交互
const assetDialogVisible = ref(false)
const purchaseDialogVisible = ref(false)
const maintenanceDialogVisible = ref(false)
const saleDialogVisible = ref(false)
const assetFormMode = ref<'create' | 'edit'>('create')
const assetFormRef = ref<FormInstance>()
const purchaseFormRef = ref<FormInstance>()
const maintenanceFormRef = ref<FormInstance>()
const saleFormRef = ref<FormInstance>()
const defaultAnnualRate = 0.5

// CRUD 表单的默认数据模型
const assetForm = reactive<AssetForm>(createDefaultAssetForm())
const purchaseForm = reactive<PurchaseForm>(createDefaultPurchaseForm())
const maintenanceForm = reactive<MaintenanceFormState>(createDefaultMaintenanceForm())
const saleForm = reactive<SaleFormState>(createDefaultSaleForm())

const statusOptions = [
  { label: '使用中', value: 'IN_USE' },
  { label: '闲置', value: 'IDLE' },
  { label: '待售', value: 'FOR_SALE' },
  { label: '已售出', value: 'SOLD' },
  { label: '已退役', value: 'RETIRED' }
] satisfies { label: string; value: AssetStatus }[]

const wishStatusOptions = [
  { label: '非常满意', value: 'VERY_SATISFIED' },
  { label: '满意', value: 'SATISFIED' },
  { label: '中性', value: 'NEUTRAL' },
  { label: '想出售', value: 'WANT_TO_SELL' },
  { label: '必须出售', value: 'MUST_SELL' }
]

// 资产主档校验规则
const assetFormRules: FormRules<AssetForm> = {
  name: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择购买日期', trigger: 'change' }],
  enabledDate: [{ required: true, message: '请选择启用日期', trigger: 'change' }],
  annualRate: [{ required: true, message: '请输入年折旧率', trigger: 'change', type: 'number', min: 0 }],
  price: [{ required: true, message: '请输入价格', trigger: 'change', type: 'number', min: 0 }]
}

// 购买记录校验规则
const purchaseFormRules: FormRules<PurchaseForm> = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择购买日期', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'change', type: 'number', min: 0 }]
}

// 维护记录校验规则
const maintenanceFormRules: FormRules<MaintenanceFormState> = {
  title: [{ required: true, message: '请输入维护项目', trigger: 'blur' }],
  cost: [{ required: true, message: '请输入费用', trigger: 'change', type: 'number', min: 0 }],
  date: [{ required: true, message: '请选择维护日期', trigger: 'change' }]
}

// 售出记录校验规则
const saleFormRules: FormRules<SaleFormState> = {
  saleDate: [{ required: true, message: '请选择售出日期', trigger: 'change' }],
  salePrice: [{ required: true, message: '请输入售出价格', trigger: 'change', type: 'number', min: 0 }]
}

// 当前界面需要展示的选中资产
const activeAsset = computed(() => assets.value.find((item) => item.id === activeAssetId.value) ?? null)

// KPI 区域使用的聚合指标
const totalAssets = computed(() => assets.value.length)
const totalDepreciation = computed(() =>
  assets.value.reduce((sum, asset) => sum + (Number(asset.accumulatedDepreciation) || 0), 0)
)
const totalAvgCostPerDay = computed(() =>
  assets.value.reduce((sum, asset) => sum + (Number(asset.avgCostPerDay) || 0), 0)
)

// 所有资产的标签汇总，便于下拉选择
const tagOptions = computed(() => {
  const set = new Set<string>()
  assets.value.forEach((asset) => asset.tags?.forEach((tag) => set.add(tag)))
  return Array.from(set)
})

// 关联设备下拉列表选项（排除自身）
const relatedAssetOptions = computed(() =>
  assets.value
    .filter((asset) => asset.id !== assetForm.id)
    .map((asset) => ({ label: asset.name, value: asset.id }))
)

// 根据 ID 映射出关联资产名称
const relatedAssetNames = computed(() => {
  if (!activeAsset.value?.relatedAssets?.length) return [] as string[]
  const map = new Map(assets.value.map((item) => [item.id, item.name]))
  return activeAsset.value.relatedAssets.map((id) => map.get(id)).filter(Boolean) as string[]
})

// 根据保修期限和待售状态生成提醒时间线
const reminders = computed<ReminderItem[]>(() => {
  const today = new Date()
  return assets.value
    .flatMap((asset) => {
      const list: ReminderItem[] = []
      if (asset.warrantyExpireDate) {
        const daysLeft = calcDaysBetween(today, parseDate(asset.warrantyExpireDate)) - 1
        if (daysLeft >= 0 && daysLeft <= 60) {
          list.push({
            assetId: asset.id,
            name: asset.name,
            type: 'WARRANTY',
            dueDate: asset.warrantyExpireDate,
            daysLeft,
            message: '保修即将到期，请留意续保或备份数据'
          })
        }
      }
      if (asset.status === 'FOR_SALE') {
        list.push({
          assetId: asset.id,
          name: asset.name,
          type: 'SALE',
          dueDate: asset.enabledDate,
          daysLeft: 0,
          message: '资产已进入待售状态，记得同步更新售出记录'
        })
      }
      return list
    })
    .sort((a, b) => a.daysLeft - b.daysLeft)
})

// 从后端加载资产数据，失败时回退到内置示例数据
const loadAssets = async () => {
  loading.value = true
  try {
    const resp = await http.get<any[]>('/assets')
    if (Array.isArray(resp) && resp.length) {
      assets.value = resp.map((item) => normalizeAsset(item))
      if (!activeAssetId.value && assets.value.length) {
        activeAssetId.value = assets.value[0].id
      }
    } else if (Array.isArray(resp) && !resp.length) {
      assets.value = []
      activeAssetId.value = null
    } else {
      useMockAssets()
    }
  } catch (error) {
    useMockAssets()
  } finally {
    loading.value = false
  }
}

// 使用内置示例数据，方便演示交互
const useMockAssets = () => {
  assets.value = createMockAssets()
  activeAssetId.value = assets.value[0]?.id ?? null
}

// 打开新建资产弹窗
const openCreateAsset = () => {
  assetFormMode.value = 'create'
  Object.assign(assetForm, createDefaultAssetForm())
  assetDialogVisible.value = true
}

// 打开编辑资产弹窗并填充已有数据
const openEditAsset = (asset: AssetSummary) => {
  assetFormMode.value = 'edit'
  Object.assign(assetForm, {
    id: asset.id,
    name: asset.name,
    category: asset.category,
    brand: asset.brand ?? '',
    model: asset.model ?? '',
    serialNo: asset.serialNo ?? '',
    status: asset.status,
    wishStatus: asset.wishStatus ?? '',
    purchasePlatform: asset.purchasePlatform ?? '',
    purchaseDate: asset.purchaseDate ?? asset.enabledDate,
    enabledDate: asset.enabledDate,
    annualRate: asset.annualRate,
    price: findPrimaryPurchase(asset)?.price ?? asset.totalInvest ?? 0,
    shippingCost: findPrimaryPurchase(asset)?.shippingCost ?? 0,
    warrantyChannel: asset.warrantyChannel ?? '',
    warrantyExpireDate: asset.warrantyExpireDate ?? '',
    purchaseEvidence: asset.purchaseEvidence ?? '',
    tags: asset.tags ? [...asset.tags] : [],
    notes: asset.notes ?? '',
    relatedAssets: asset.relatedAssets ? [...asset.relatedAssets] : []
  })
  assetDialogVisible.value = true
}

const selectAsset = (asset: AssetSummary) => {
  activeAssetId.value = asset.id
}

// 表格行点击时切换选中资产
const handleRowClick = (asset: AssetSummary) => {
  selectAsset(asset)
}

// 高亮当前选中行
const rowClassName = ({ row }: { row: AssetSummary }) =>
  row.id === activeAssetId.value ? 'active-row' : ''

// 创建或更新资产主档，并在前端计算折旧相关指标
const submitAssetForm = async () => {
  const form = assetFormRef.value
  if (!form) return
  await form.validate()
  if (assetFormMode.value === 'create') {
    const id = generateId()
    const newAsset: AssetSummary = {
      id,
      name: assetForm.name,
      category: assetForm.category,
      brand: assetForm.brand || undefined,
      model: assetForm.model || undefined,
      serialNo: assetForm.serialNo || undefined,
      status: assetForm.status,
      wishStatus: assetForm.wishStatus || undefined,
      totalInvest: 0,
      avgCostPerDay: 0,
      accumulatedDepreciation: 0,
      bookValue: 0,
      enabledDate: assetForm.enabledDate,
      purchaseDate: assetForm.purchaseDate,
      purchasePlatform: assetForm.purchasePlatform || undefined,
      annualRate: assetForm.annualRate,
      useDays: 1,
      warrantyChannel: assetForm.warrantyChannel || undefined,
      warrantyExpireDate: assetForm.warrantyExpireDate || undefined,
      tags: assetForm.tags.length ? [...assetForm.tags] : [],
      relatedAssets: assetForm.relatedAssets.length ? [...assetForm.relatedAssets] : [],
      purchaseEvidence: assetForm.purchaseEvidence || undefined,
      coverImageUrl: undefined,
      notes: assetForm.notes || undefined,
      retiredDate: undefined,
      purchases: [],
      maintenanceRecords: [],
      saleRecords: []
    }
    if (assetForm.price > 0 || assetForm.shippingCost > 0) {
      newAsset.purchases.push({
        id: generateId(),
        assetId: id,
        name: assetForm.name,
        type: 'PRIMARY',
        platform: assetForm.purchasePlatform || undefined,
        price: assetForm.price,
        shippingCost: assetForm.shippingCost,
        purchaseDate: assetForm.purchaseDate,
        warrantyExpireDate: assetForm.warrantyExpireDate || undefined,
        warrantyChannel: assetForm.warrantyChannel || undefined,
        notes: undefined
      })
    }
    recalcAssetFinancials(newAsset)
    assets.value = [newAsset, ...assets.value]
    activeAssetId.value = id
  } else if (assetForm.id) {
    const index = assets.value.findIndex((item) => item.id === assetForm.id)
    if (index !== -1) {
      const asset = { ...assets.value[index] }
      asset.name = assetForm.name
      asset.category = assetForm.category
      asset.brand = assetForm.brand || undefined
      asset.model = assetForm.model || undefined
      asset.serialNo = assetForm.serialNo || undefined
      asset.status = assetForm.status
      asset.wishStatus = assetForm.wishStatus || undefined
      asset.purchasePlatform = assetForm.purchasePlatform || undefined
      asset.purchaseDate = assetForm.purchaseDate
      asset.enabledDate = assetForm.enabledDate
      asset.annualRate = assetForm.annualRate
      asset.warrantyChannel = assetForm.warrantyChannel || undefined
      asset.warrantyExpireDate = assetForm.warrantyExpireDate || undefined
      asset.purchaseEvidence = assetForm.purchaseEvidence || undefined
      asset.tags = assetForm.tags.length ? [...assetForm.tags] : []
      asset.notes = assetForm.notes || undefined
      asset.relatedAssets = assetForm.relatedAssets.length ? [...assetForm.relatedAssets] : []
      const primary = findPrimaryPurchase(asset)
      if (primary) {
        primary.name = assetForm.name
        primary.platform = assetForm.purchasePlatform || undefined
        primary.price = assetForm.price
        primary.shippingCost = assetForm.shippingCost
        primary.purchaseDate = assetForm.purchaseDate
        primary.warrantyExpireDate = assetForm.warrantyExpireDate || undefined
        primary.warrantyChannel = assetForm.warrantyChannel || undefined
      } else if (assetForm.price > 0 || assetForm.shippingCost > 0) {
        asset.purchases = [
          {
            id: generateId(),
            assetId: asset.id,
            name: assetForm.name,
            type: 'PRIMARY',
            platform: assetForm.purchasePlatform || undefined,
            price: assetForm.price,
            shippingCost: assetForm.shippingCost,
            purchaseDate: assetForm.purchaseDate,
            warrantyExpireDate: assetForm.warrantyExpireDate || undefined,
            warrantyChannel: assetForm.warrantyChannel || undefined,
            notes: undefined
          },
          ...asset.purchases
        ]
      }
      recalcAssetFinancials(asset)
      assets.value.splice(index, 1, asset)
      activeAssetId.value = asset.id
    }
  }
  assetDialogVisible.value = false
  ElMessage.success('资产已保存')
}

// 删除资产前弹出确认提示
const confirmDelete = (asset: AssetSummary) => {
  ElMessageBox.confirm(`确定要删除资产 “${asset.name}” 吗？`, '提示', {
    type: 'warning'
  })
    .then(() => {
      assets.value = assets.value.filter((item) => item.id !== asset.id)
      if (activeAssetId.value === asset.id) {
        activeAssetId.value = assets.value[0]?.id ?? null
      }
      ElMessage.success('资产已删除')
    })
    .catch(() => void 0)
}

const openPurchaseForm = () => {
  if (!activeAsset.value) return
  Object.assign(purchaseForm, createDefaultPurchaseForm(), { assetId: activeAsset.value.id })
  purchaseDialogVisible.value = true
}

// 追加购买记录后同步折旧计算
const submitPurchaseForm = async () => {
  const form = purchaseFormRef.value
  const asset = activeAsset.value
  if (!form || !asset) return
  await form.validate()
  const record: PurchaseRecord = {
    id: generateId(),
    assetId: asset.id,
    name: purchaseForm.name,
    type: purchaseForm.type,
    platform: purchaseForm.platform || undefined,
    price: purchaseForm.price,
    shippingCost: purchaseForm.shippingCost,
    purchaseDate: purchaseForm.purchaseDate,
    warrantyExpireDate: purchaseForm.warrantyExpireDate || undefined,
    warrantyChannel: purchaseForm.warrantyChannel || undefined,
    notes: purchaseForm.notes || undefined
  }
  asset.purchases = [...asset.purchases, record]
  recalcAssetFinancials(asset)
  replaceAsset(asset)
  purchaseDialogVisible.value = false
  ElMessage.success('购买记录已添加')
}

const openMaintenanceForm = () => {
  if (!activeAsset.value) return
  Object.assign(maintenanceForm, createDefaultMaintenanceForm(), { assetId: activeAsset.value.id })
  maintenanceDialogVisible.value = true
}

// 添加维护记录并保留原有列表
const submitMaintenanceForm = async () => {
  const form = maintenanceFormRef.value
  const asset = activeAsset.value
  if (!form || !asset) return
  await form.validate()
  const record: MaintenanceRecord = {
    id: generateId(),
    assetId: asset.id,
    title: maintenanceForm.title,
    cost: maintenanceForm.cost,
    date: maintenanceForm.date,
    notes: maintenanceForm.notes || undefined,
    type: maintenanceForm.type || undefined
  }
  asset.maintenanceRecords = [...asset.maintenanceRecords, record]
  replaceAsset(asset)
  maintenanceDialogVisible.value = false
  ElMessage.success('维护记录已添加')
}

const openSaleForm = () => {
  if (!activeAsset.value) return
  Object.assign(saleForm, createDefaultSaleForm(), { assetId: activeAsset.value.id, saleDate: formatDate(new Date()) })
  saleDialogVisible.value = true
}

// 保存售出记录并更新资产状态
const submitSaleForm = async () => {
  const form = saleFormRef.value
  const asset = activeAsset.value
  if (!form || !asset) return
  await form.validate()
  const netIncome = saleForm.salePrice - (saleForm.fee || 0) - (saleForm.shippingCost || 0) - (saleForm.otherCost || 0)
  const realizedPnl = netIncome - asset.bookValue
  const record: SaleRecord = {
    id: generateId(),
    assetId: asset.id,
    platform: saleForm.platform || undefined,
    saleDate: saleForm.saleDate,
    salePrice: saleForm.salePrice,
    fee: saleForm.fee || undefined,
    shippingCost: saleForm.shippingCost || undefined,
    otherCost: saleForm.otherCost || undefined,
    netIncome,
    realizedPnl,
    notes: saleForm.notes || undefined
  }
  asset.saleRecords = [...asset.saleRecords, record]
  asset.status = 'SOLD'
  asset.bookValue = 0
  asset.accumulatedDepreciation = asset.totalInvest
  asset.useDays = calcUseDays(asset)
  replaceAsset(asset)
  saleDialogVisible.value = false
  ElMessage.success('售出记录已添加')
}

// 更新 assets 数组中的对应元素，触发视图刷新
const replaceAsset = (asset: AssetSummary) => {
  const index = assets.value.findIndex((item) => item.id === asset.id)
  if (index !== -1) {
    assets.value.splice(index, 1, { ...asset })
  }
}

// 金额格式化工具函数
const formatCurrency = (value: number | string) => {
  const num = Number(value)
  if (Number.isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 比例格式化工具函数
const formatPercent = (value: number | string) => {
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return `${(num * 100).toFixed(1)}%`
}

// 状态中文映射
const statusLabel = (status: AssetStatus) => {
  const item = statusOptions.find((opt) => opt.value === status)
  return item ? item.label : status
}

// 状态对应的标签类型
const statusTagType = (status: AssetStatus) => {
  switch (status) {
    case 'IN_USE':
      return 'success'
    case 'IDLE':
      return 'info'
    case 'FOR_SALE':
      return 'warning'
    case 'SOLD':
      return 'danger'
    case 'RETIRED':
      return 'default'
    default:
      return 'info'
  }
}

const wishStatusLabel = (status: string) => {
  const item = wishStatusOptions.find((opt) => opt.value === status)
  return item ? item.label : status
}

// 购买类型中文映射
const purchaseTypeLabel = (type: PurchaseType) => {
  switch (type) {
    case 'PRIMARY':
      return '主购买'
    case 'ACCESSORY':
      return '配件'
    case 'SERVICE':
      return '服务'
    default:
      return type
  }
}

// 获取资产的主购买记录，便于同步基础金额
const findPrimaryPurchase = (asset: AssetSummary) =>
  asset.purchases.find((item) => item.type === 'PRIMARY') || null

// 根据当前购买/售出记录重新计算折旧、账面价值等指标
const recalcAssetFinancials = (asset: AssetSummary) => {
  const totalInvest = asset.purchases.reduce(
    (sum, item) => sum + (Number(item.price) || 0) + (Number(item.shippingCost) || 0),
    0
  )
  asset.totalInvest = totalInvest || asset.totalInvest
  asset.useDays = calcUseDays(asset)
  const depreciation = Math.min(asset.totalInvest * asset.annualRate * (asset.useDays / 365), asset.totalInvest)
  asset.accumulatedDepreciation = round(depreciation)
  asset.bookValue = round(Math.max(asset.totalInvest - asset.accumulatedDepreciation, 0))
  asset.avgCostPerDay = round(asset.totalInvest / Math.max(asset.useDays, 1))
}

// 计算启用天数：售出/退役后取对应日期
const calcUseDays = (asset: AssetSummary) => {
  if (!asset.enabledDate) return 1
  const start = parseDate(asset.enabledDate)
  let end = new Date()
  if (asset.status === 'SOLD' && asset.saleRecords.length) {
    const sortedSales = [...asset.saleRecords].sort((a, b) => (a.saleDate > b.saleDate ? 1 : -1))
    const latestSale = sortedSales[sortedSales.length - 1]
    if (latestSale) end = parseDate(latestSale.saleDate)
  }
  if (asset.status === 'RETIRED' && asset.retiredDate) {
    end = parseDate(asset.retiredDate)
  }
  return Math.max(calcDaysBetween(start, end), 1)
}

// 计算自然日差值（含首日）
const calcDaysBetween = (from: Date, to: Date) => {
  const start = Date.UTC(from.getFullYear(), from.getMonth(), from.getDate())
  const end = Date.UTC(to.getFullYear(), to.getMonth(), to.getDate())
  return Math.floor((end - start) / (24 * 3600 * 1000)) + 1
}

// 字符串日期容错解析
const parseDate = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return new Date()
  }
  return date
}

// 统一输出 YYYY-MM-DD 字符串
function formatDate(date: Date): string {
  const yyyy = date.getFullYear()
  const mm = `${date.getMonth() + 1}`.padStart(2, '0')
  const dd = `${date.getDate()}`.padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

// 金额数值统一保留两位小数
const round = (value: number) => Number(value.toFixed(2))

let idSeed = Date.now()
// 简易的自增 ID，便于前端临时演示
const generateId = () => ++idSeed

// 将后端返回的数据转换为前端可用结构
const normalizeAsset = (raw: any): AssetSummary => {
  const purchases: PurchaseRecord[] = Array.isArray(raw.purchases)
    ? raw.purchases.map((item: any) => ({
        id: Number(item.id ?? generateId()),
        assetId: Number(raw.id),
        name: item.item_name ?? item.itemName ?? item.name ?? raw.name,
        type: (item.type ?? 'PRIMARY') as PurchaseType,
        platform: item.platform ?? item.purchase_platform ?? raw.purchasePlatform ?? undefined,
        price: Number(item.price ?? 0),
        shippingCost: Number(item.shippingCost ?? item.shipping_cost ?? 0),
        purchaseDate:
          item.purchaseDate ?? item.purchase_date ?? raw.purchaseDate ?? raw.enabledDate ?? formatDate(new Date()),
        warrantyExpireDate:
          item.warrantyExpireDate ?? item.warranty_expire_date ?? raw.warrantyExpireDate ?? undefined,
        warrantyChannel: item.warrantyChannel ?? item.warranty_channel ?? raw.warrantyChannel ?? undefined,
        notes: item.notes ?? undefined
      }))
    : []

  const maintenanceRecords: MaintenanceRecord[] = Array.isArray(raw.maintenanceRecords)
    ? raw.maintenanceRecords.map((item: any) => ({
        id: Number(item.id ?? generateId()),
        assetId: Number(raw.id),
        title: item.title ?? item.name ?? '维护',
        cost: Number(item.cost ?? 0),
        date: item.date ?? item.maintenanceDate ?? formatDate(new Date()),
        notes: item.notes ?? undefined,
        type: item.type ?? undefined
      }))
    : []

  const saleRecords: SaleRecord[] = Array.isArray(raw.saleRecords)
    ? raw.saleRecords.map((item: any) => ({
        id: Number(item.id ?? generateId()),
        assetId: Number(raw.id),
        platform: item.platform ?? undefined,
        saleDate: item.saleDate ?? item.sale_date ?? formatDate(new Date()),
        salePrice: Number(item.salePrice ?? item.sale_price ?? 0),
        fee: Number(item.fee ?? 0) || undefined,
        shippingCost: Number(item.shippingCost ?? item.shipping_cost ?? 0) || undefined,
        otherCost: Number(item.otherCost ?? item.other_cost ?? 0) || undefined,
        netIncome: Number(item.netIncome ?? item.net_income ?? 0),
        realizedPnl: Number(item.realizedPnl ?? item.realized_pnl ?? 0),
        notes: item.notes ?? undefined
      }))
    : []

  const asset: AssetSummary = {
    id: Number(raw.id),
    name: raw.name ?? '未命名资产',
    category: raw.category ?? '未分类',
    brand: raw.brand ?? raw.assetBrand ?? undefined,
    model: raw.model ?? undefined,
    serialNo: raw.serialNo ?? undefined,
    status: (raw.status ?? 'IN_USE') as AssetStatus,
    wishStatus: raw.wishStatus ?? undefined,
    totalInvest: Number(raw.totalInvest ?? raw.total_invest ?? 0),
    avgCostPerDay: Number(raw.avgCostPerDay ?? raw.avg_cost_per_day ?? 0),
    accumulatedDepreciation: Number(raw.accumulatedDepreciation ?? raw.accum_depr ?? 0),
    bookValue: Number(raw.bookValue ?? raw.book_value ?? 0),
    enabledDate: raw.enabledDate ?? raw.enabled_date ?? formatDate(new Date()),
    purchaseDate: raw.purchaseDate ?? raw.purchase_date ?? undefined,
    purchasePlatform: raw.purchasePlatform ?? raw.purchase_platform ?? undefined,
    annualRate: Number(raw.annualRate ?? raw.annual_rate ?? defaultAnnualRate),
    useDays: Number(raw.useDays ?? raw.use_days ?? 0),
    warrantyChannel: raw.warrantyChannel ?? raw.warranty_channel ?? undefined,
    warrantyExpireDate: raw.warrantyExpireDate ?? raw.warranty_expire_date ?? undefined,
    tags: Array.isArray(raw.tags)
      ? raw.tags
      : typeof raw.tags === 'string'
      ? raw.tags.split(',').map((tag: string) => tag.trim()).filter(Boolean)
      : [],
    relatedAssets: Array.isArray(raw.relatedAssets)
      ? raw.relatedAssets
      : typeof raw.related_assets === 'string'
      ? raw.related_assets.split(',').map((id: string) => Number(id)).filter(Boolean)
      : raw.related_assets ?? [],
    purchaseEvidence: raw.purchaseEvidence ?? raw.purchase_evidence ?? undefined,
    coverImageUrl: raw.coverImageUrl ?? raw.cover_image_url ?? undefined,
    notes: raw.notes ?? undefined,
    retiredDate: raw.retiredDate ?? raw.retired_date ?? undefined,
    purchases,
    maintenanceRecords,
    saleRecords
  }

  if (!asset.purchases.length && asset.purchaseDate && asset.totalInvest) {
    asset.purchases.push({
      id: generateId(),
      assetId: asset.id,
      name: asset.name,
      type: 'PRIMARY',
      platform: asset.purchasePlatform,
      price: asset.totalInvest,
      shippingCost: 0,
      purchaseDate: asset.purchaseDate,
      warrantyExpireDate: asset.warrantyExpireDate,
      warrantyChannel: asset.warrantyChannel,
      notes: undefined
    })
  }

  if (!asset.useDays) {
    asset.useDays = calcUseDays(asset)
  }

  if (!asset.totalInvest) {
    asset.totalInvest = asset.purchases.reduce(
      (sum, item) => sum + (Number(item.price) || 0) + (Number(item.shippingCost) || 0),
      0
    )
  }

  if (!asset.avgCostPerDay || !asset.accumulatedDepreciation || !asset.bookValue) {
    recalcAssetFinancials(asset)
  }

  return asset
}

// 资产主档表单默认值
function createDefaultAssetForm(): AssetForm {
  return {
    name: '',
    category: '',
    brand: '',
    model: '',
    serialNo: '',
    status: 'IN_USE',
    wishStatus: '',
    purchasePlatform: '',
    purchaseDate: formatDate(new Date()),
    enabledDate: formatDate(new Date()),
    annualRate: defaultAnnualRate,
    price: 0,
    shippingCost: 0,
    warrantyChannel: '',
    warrantyExpireDate: '',
    purchaseEvidence: '',
    tags: [],
    notes: '',
    relatedAssets: []
  }
}

// 购买记录表单默认值
function createDefaultPurchaseForm(): PurchaseForm {
  return {
    name: '',
    type: 'ACCESSORY',
    platform: '',
    price: 0,
    shippingCost: 0,
    purchaseDate: formatDate(new Date()),
    warrantyExpireDate: '',
    warrantyChannel: '',
    notes: ''
  }
}

// 维护记录表单默认值
function createDefaultMaintenanceForm(): MaintenanceFormState {
  return {
    title: '',
    cost: 0,
    date: formatDate(new Date()),
    notes: '',
    type: ''
  }
}

// 售出记录表单默认值
function createDefaultSaleForm(): SaleFormState {
  return {
    platform: '',
    saleDate: formatDate(new Date()),
    salePrice: 0,
    fee: 0,
    shippingCost: 0,
    otherCost: 0,
    notes: ''
  }
}

// 本地演示使用的示例资产数据
const createMockAssets = (): AssetSummary[] => {
  const today = new Date()
  const asset1: AssetSummary = {
    id: generateId(),
    name: 'MacBook Pro 14" 2023',
    category: '笔记本电脑',
    brand: 'Apple',
    model: 'MacBook Pro 14" M2 Pro',
    serialNo: 'MBP-2023-001',
    status: 'IN_USE',
    wishStatus: 'VERY_SATISFIED',
    totalInvest: 0,
    avgCostPerDay: 0,
    accumulatedDepreciation: 0,
    bookValue: 0,
    enabledDate: '2023-02-18',
    purchaseDate: '2023-02-16',
    purchasePlatform: 'Apple Store',
    annualRate: 0.5,
    useDays: 0,
    warrantyChannel: 'Apple Care+',
    warrantyExpireDate: '2025-02-18',
    tags: ['开发', '主力机'],
    relatedAssets: [],
    purchaseEvidence: '',
    coverImageUrl: undefined,
    notes: '主力开发设备',
    retiredDate: undefined,
    purchases: [
      {
        id: generateId(),
        assetId: 0,
        name: 'MacBook Pro 14" 2023',
        type: 'PRIMARY',
        platform: 'Apple Store',
        price: 16888,
        shippingCost: 0,
        purchaseDate: '2023-02-16',
        warrantyExpireDate: '2025-02-18',
        warrantyChannel: 'Apple Care+',
        notes: undefined
      },
      {
        id: generateId(),
        assetId: 0,
        name: 'USB-C 转 HDMI 转换器',
        type: 'ACCESSORY',
        platform: '京东',
        price: 199,
        shippingCost: 0,
        purchaseDate: '2023-02-20',
        warrantyExpireDate: '',
        warrantyChannel: '',
        notes: '会议使用'
      }
    ],
    maintenanceRecords: [
      {
        id: generateId(),
        assetId: 0,
        title: '键盘清洁',
        cost: 120,
        date: '2024-01-12',
        notes: '官方保养',
        type: '保养'
      }
    ],
    saleRecords: []
  }

  const asset2: AssetSummary = {
    id: generateId(),
    name: 'Sony A7M4',
    category: '相机',
    brand: 'Sony',
    model: 'ILCE-7M4',
    serialNo: 'A7M4-7788',
    status: 'FOR_SALE',
    wishStatus: 'WANT_TO_SELL',
    totalInvest: 0,
    avgCostPerDay: 0,
    accumulatedDepreciation: 0,
    bookValue: 0,
    enabledDate: '2022-06-03',
    purchaseDate: '2022-06-01',
    purchasePlatform: '京东',
    annualRate: 0.45,
    useDays: 0,
    warrantyChannel: '索尼延保',
    warrantyExpireDate: formatDate(addDays(today, 25)),
    tags: ['摄影', '全画幅'],
    relatedAssets: [],
    purchaseEvidence: '',
    coverImageUrl: undefined,
    notes: '准备升级 A7R 系列',
    retiredDate: undefined,
    purchases: [
      {
        id: generateId(),
        assetId: 0,
        name: 'Sony A7M4',
        type: 'PRIMARY',
        platform: '京东',
        price: 17899,
        shippingCost: 0,
        purchaseDate: '2022-06-01',
        warrantyExpireDate: formatDate(addDays(today, 25)),
        warrantyChannel: '索尼延保',
        notes: undefined
      },
      {
        id: generateId(),
        assetId: 0,
        name: '索尼 FE 35mm F1.8',
        type: 'ACCESSORY',
        platform: '闲鱼',
        price: 3300,
        shippingCost: 20,
        purchaseDate: '2023-01-15',
        warrantyExpireDate: '',
        warrantyChannel: '',
        notes: undefined
      }
    ],
    maintenanceRecords: [],
    saleRecords: []
  }

  const asset3: AssetSummary = {
    id: generateId(),
    name: 'Nintendo Switch OLED',
    category: '游戏主机',
    brand: 'Nintendo',
    model: 'HEG-001',
    serialNo: 'SWITCH-5566',
    status: 'SOLD',
    wishStatus: 'MUST_SELL',
    totalInvest: 0,
    avgCostPerDay: 0,
    accumulatedDepreciation: 0,
    bookValue: 0,
    enabledDate: '2021-11-11',
    purchaseDate: '2021-11-05',
    purchasePlatform: '天猫国际',
    annualRate: 0.4,
    useDays: 0,
    warrantyChannel: '国行保修',
    warrantyExpireDate: '2023-11-11',
    tags: ['娱乐'],
    relatedAssets: [],
    purchaseEvidence: '',
    coverImageUrl: undefined,
    notes: '2024 年 3 月售出',
    retiredDate: undefined,
    purchases: [
      {
        id: generateId(),
        assetId: 0,
        name: 'Nintendo Switch OLED',
        type: 'PRIMARY',
        platform: '天猫国际',
        price: 2599,
        shippingCost: 60,
        purchaseDate: '2021-11-05',
        warrantyExpireDate: '2023-11-11',
        warrantyChannel: '国行保修',
        notes: undefined
      }
    ],
    maintenanceRecords: [],
    saleRecords: [
      {
        id: generateId(),
        assetId: 0,
        platform: '闲鱼',
        saleDate: '2024-03-10',
        salePrice: 2200,
        fee: 50,
        shippingCost: 20,
        otherCost: 0,
        netIncome: 2130,
        realizedPnl: 2130 - 0,
        notes: '包含两个手柄'
      }
    ]
  }

  ;[asset1, asset2, asset3].forEach((asset) => {
    asset.purchases = asset.purchases.map((item) => ({ ...item, assetId: asset.id }))
    asset.maintenanceRecords = asset.maintenanceRecords.map((item) => ({ ...item, assetId: asset.id }))
    asset.saleRecords = asset.saleRecords.map((item) => ({ ...item, assetId: asset.id }))
    recalcAssetFinancials(asset)
    if (asset.status === 'SOLD') {
      asset.bookValue = 0
      asset.accumulatedDepreciation = asset.totalInvest
      asset.useDays = calcUseDays(asset)
    }
  })

  asset1.relatedAssets = [asset2.id]
  asset2.relatedAssets = [asset1.id]

  return [asset1, asset2, asset3]
}

// 日期偏移工具函数
const addDays = (date: Date, days: number) => {
  const cloned = new Date(date)
  cloned.setDate(cloned.getDate() + days)
  return cloned
}

// 页面初始化时加载资产数据
onMounted(loadAssets)
</script>

<style scoped>
.dashboard {
  padding: 2rem 5vw;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(120deg, #2563eb, #1d4ed8);
  border-radius: 16px;
  padding: 2rem;
  color: #fff;
  gap: 2rem;
}

.hero-text {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 560px;
}

.hero img {
  width: 220px;
  max-width: 35vw;
}

.hero-actions {
  display: flex;
  gap: 0.75rem;
}

.overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.25rem;
}

.metric-card {
  border-radius: 14px;
  text-align: center;
}

.metric-value {
  font-size: 2rem;
  font-weight: 600;
  color: #1f2937;
}

.metric-label {
  color: #6b7280;
  margin-top: 0.5rem;
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.section-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
}

.asset-table-card {
  border-radius: 16px;
}

.details-row {
  align-items: stretch;
}

.detail-card,
.purchase-card,
.reminder-card {
  border-radius: 16px;
  margin-bottom: 1.5rem;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.block-title {
  font-size: 1.05rem;
  font-weight: 600;
  margin: 0.5rem 0;
}

.tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.notes {
  margin-top: 1rem;
}

.reminder-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profit {
  color: #16a34a;
}

.loss {
  color: #dc2626;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.full-width {
  width: 100%;
}

:deep(.active-row) {
  background-color: rgba(37, 99, 235, 0.08);
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero img {
    width: 160px;
  }
}
</style>
