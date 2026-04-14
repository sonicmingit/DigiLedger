<template>
  <div class="mobile-layout">
    <div class="mobile-content">
      <RouterView v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </div>
    
    <TabBar @fab-click="openQuickAdd" />

    <!-- Quick Add Sheet -->
    <Teleport to="body">
      <div v-if="showQuickAdd" class="quick-add-overlay" @click="closeQuickAdd">
        <div class="quick-add-sheet" @click.stop>
          <div class="sheet-header">
            <h3>快速记账</h3>
            <button class="close-btn" @click="closeQuickAdd"><i class="mdi mdi-close"></i></button>
          </div>
          
          <div class="sheet-body">
            <div class="form-group">
              <label>物品名称</label>
              <input v-model="form.name" type="text" placeholder="例如：iPhone 15" class="input-field" autofocus />
            </div>
            
            <div class="form-group">
              <label>金额</label>
              <div class="price-input">
                <span>￥</span>
                <input v-model.number="form.price" type="number" placeholder="0.00" class="input-field" />
              </div>
            </div>

            <!-- Simple Category Selection (Mocked for now or flat list) -->
            <div class="form-group">
              <label>大致分类</label>
              <div class="chip-group">
                <span 
                  v-for="cat in quickCategories" 
                  :key="cat.id" 
                  class="chip"
                  :class="{ active: form.categoryId === cat.id }"
                  @click="form.categoryId = cat.id"
                >
                  {{ cat.name }}
                </span>
              </div>
            </div>
          </div>
          
          <div class="sheet-footer">
            <button class="submit-btn" @click="submit" :disabled="submitting">
              <i class="mdi mdi-check" v-if="!submitting"></i>
              <i class="mdi mdi-loading mdi-spin" v-else></i>
              {{ submitting ? '保存中...' : '确认添加' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import TabBar from '@/mobile/components/TabBar.vue'
import '@/mobile/styles/main.css'
import { createAsset } from '@/api/asset'
import { ElMessage } from 'element-plus'

const router = useRouter()
const showQuickAdd = ref(false)
const submitting = ref(false)

const form = reactive({
  name: '',
  price: undefined as number | undefined,
  categoryId: 0
})

const quickCategories = [
  { id: 1, name: '数码' }, // IDs should normally come from API
  { id: 2, name: '家电' },
  { id: 3, name: '办公' },
  { id: 4, name: '生活' },
  { id: 5, name: '其他' }
]

const openQuickAdd = () => {
  form.name = ''
  form.price = undefined
  form.categoryId = 5 // Default Other
  showQuickAdd.value = true
}

const closeQuickAdd = () => {
  showQuickAdd.value = false
}

const submit = async () => {
  if (!form.name) return ElMessage.warning('请输入名称')
  if (!form.price) return ElMessage.warning('请输入金额')
  
  submitting.value = true
  try {
    // Basic payload for quick add
    await createAsset({
      name: form.name,
      status: '使用中',
      totalCostStrategy: 'CUSTOM', // or PRICE
      targetCostValue: form.price,
      categoryId: form.categoryId,
      purchaseDate: new Date().toISOString().split('T')[0],
       // We might need to construct a Purchase object if the backend requires it for the price to show up as cost
       // But AssetPayload has targetCostValue which might be enough for summary
      purchases: [
        {
          type: 'PRIMARY',
          price: form.price,
          purchaseDate: new Date().toISOString().split('T')[0],
          quantity: 1
        }
      ]
    })
    
    ElMessage.success('添加成功')
    closeQuickAdd()
    // Refresh current view if possible, or just let user navigate
    // If on home, maybe reload? For now simple close.
    if (router.currentRoute.value.name === 'mobileHome') {
      // Trigger reload mechanism if we had one (e.g. global event bus or store)
      // Since we don't have store setup here, we just reload page or navigate
      window.location.reload()
    }
  } catch (e) {
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mobile-layout {
  min-height: 100vh;
  background-color: var(--dl-bg-base);
  padding-bottom: calc(var(--dl-tabbar-height) + env(safe-area-inset-bottom));
}

.mobile-content {
  height: 100%;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

/* Quick Add Sheet */
.quick-add-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  backdrop-filter: blur(4px);
}

.quick-add-sheet {
  width: 100%;
  background: var(--dl-bg-surface);
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
  padding: 24px;
  padding-bottom: calc(24px + env(safe-area-inset-bottom));
  animation: slide-up 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

@keyframes slide-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.sheet-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--dl-text-primary);
}

.close-btn {
  background: transparent;
  border: none;
  color: var(--dl-text-muted);
  font-size: 24px;
  padding: 4px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 12px;
  color: var(--dl-text-muted);
  margin-bottom: 8px;
}

.input-field {
  width: 100%;
  height: 48px;
  background: var(--dl-bg-surface-light);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 0 16px;
  color: var(--dl-text-primary);
  font-size: 16px;
  outline: none;
  transition: all 0.2s;
}

.input-field:focus {
  border-color: var(--dl-primary);
  background: rgba(255, 255, 255, 0.05);
}

.price-input {
  position: relative;
  display: flex;
  align-items: center;
}

.price-input span {
  position: absolute;
  left: 16px;
  color: var(--dl-text-primary);
  font-weight: 700;
  font-size: 18px;
}

.price-input input {
  padding-left: 36px;
  font-family: var(--dl-font-mono);
  font-weight: 700;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  padding: 8px 16px;
  background: var(--dl-bg-surface-light);
  border-radius: 20px;
  font-size: 13px;
  color: var(--dl-text-secondary);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.chip.active {
  background: var(--dl-primary-dim);
  color: var(--dl-primary);
  border-color: var(--dl-primary);
}

.sheet-footer {
  margin-top: 32px;
}

.submit-btn {
  width: 100%;
  height: 50px;
  background: var(--dl-primary);
  border: none;
  border-radius: 25px;
  color: #000;
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 0 15px var(--dl-primary-dim);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
