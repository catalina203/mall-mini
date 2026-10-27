<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Search, Warning } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { adjustStock, fetchStockAlerts, fetchStockLogs } from '@/api/stock'
import { exportStockLogs } from '@/api/exports'
import { useAuthStore } from '@/stores/auth'
import { downloadBlob } from '@/utils/download'
import type { AdminStockAlert, AdminStockLog } from '@/types/admin'
import { formatDateTime, formatPrice } from '@/utils/format'

const CHANGE_TYPE_OPTIONS = [
  { label: '下单扣减', value: 'ORDER_DEDUCT' },
  { label: '取消回补', value: 'ORDER_CANCEL_RESTORE' },
  { label: '售后退货回补', value: 'AFTER_SALE_RESTORE' },
  { label: '手动调整', value: 'MANUAL_ADJUST' },
  { label: '商品初始库存', value: 'PRODUCT_INIT' },
  { label: '盘点', value: 'STOCKTAKE' },
]

const CHANGE_TYPE_TEXT = Object.fromEntries(
  CHANGE_TYPE_OPTIONS.map((item) => [item.value, item.label]),
) as Record<string, string>

const auth = useAuthStore()
const loading = ref(false)
const exporting = ref(false)
const alertLoading = ref(false)
const saving = ref(false)
const error = ref('')
const logs = ref<AdminStockLog[]>([])
const alerts = ref<AdminStockAlert[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(15)
const skuId = ref<number | undefined>(undefined)
const bizNo = ref('')
const changeType = ref('')

const adjustVisible = ref(false)
const adjustForm = reactive({
  skuId: 0,
  productName: '',
  skuCode: '',
  currentStock: 0,
  stock: 0,
  remark: '',
})

async function loadLogs() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchStockLogs({
      page: page.value,
      pageSize: pageSize.value,
      skuId: skuId.value || undefined,
      bizNo: bizNo.value.trim() || undefined,
      changeType: changeType.value || undefined,
    })
    logs.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '库存流水加载失败'
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadAlerts() {
  alertLoading.value = true
  try {
    alerts.value = await fetchStockAlerts()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '库存预警加载失败')
    alerts.value = []
  } finally {
    alertLoading.value = false
  }
}

function search() {
  page.value = 1
  void loadLogs()
}

function resetFilters() {
  skuId.value = undefined
  bizNo.value = ''
  changeType.value = ''
  search()
}

function openAdjust(row: AdminStockAlert) {
  adjustForm.skuId = row.skuId
  adjustForm.productName = row.productName
  adjustForm.skuCode = row.skuCode
  adjustForm.currentStock = row.stock
  adjustForm.stock = row.stock
  adjustForm.remark = ''
  adjustVisible.value = true
}

async function submitAdjust() {
  if (saving.value) {
    return
  }
  if (adjustForm.stock < 0) {
    ElMessage.warning('库存不能为负')
    return
  }
  saving.value = true
  try {
    await adjustStock(adjustForm.skuId, adjustForm.stock, adjustForm.remark)
    ElMessage.success('库存已调整')
    adjustVisible.value = false
    await Promise.all([loadLogs(), loadAlerts()])
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '调整失败')
  } finally {
    saving.value = false
  }
}

function changePage(value: number) {
  page.value = value
  void loadLogs()
}

async function exportList() {
  if (exporting.value) return
  exporting.value = true
  try {
    const { blob, filename } = await exportStockLogs({
      skuId: skuId.value || undefined,
      bizNo: bizNo.value.trim() || undefined,
      changeType: changeType.value || undefined,
    })
    downloadBlob(blob, filename)
    ElMessage.success('导出已开始')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

function changeTypeText(value: string): string {
  return CHANGE_TYPE_TEXT[value] ?? value
}

onMounted(() => {
  void loadLogs()
  void loadAlerts()
})
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="INVENTORY"
      title="库存中心"
      description="查看库存流水与低库存预警。每次订单扣减、取消回补、售后退货和手动调整都会留痕。"
    >
      <template #actions>
        <el-button
          v-if="auth.can('EXPORT_READ')"
          :icon="Download"
          :loading="exporting"
          @click="exportList"
        >导出 CSV</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="error" type="error" :closable="false" :title="error">
      <template #default>
        <el-button link type="primary" @click="loadLogs">重新加载</el-button>
      </template>
    </el-alert>

    <div class="alert-card">
      <div class="alert-head">
        <div>
          <p class="eyebrow">LOW STOCK</p>
          <h2>库存预警</h2>
          <p>默认阈值为 10，低于或等于阈值的启用 SKU 会出现在这里。</p>
        </div>
        <div class="alert-count">
          <el-icon><Warning /></el-icon>
          <strong>{{ alerts.length }}</strong>
          <span>个 SKU</span>
        </div>
      </div>
      <el-table v-loading="alertLoading" :data="alerts" size="small" empty-text="暂无低库存 SKU">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="product-cell">
              <strong>{{ row.productName }}</strong>
              <span class="subtle mono">{{ row.skuCode }} · {{ row.specText || '默认规格' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="当前库存" width="110">
          <template #default="{ row }">
            <span class="stock-low">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="阈值" width="80" prop="threshold" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAdjust(row)">调整库存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model.number="skuId"
          placeholder="SKU ID"
          clearable
          class="sku-id"
          @keyup.enter="search"
          @clear="search"
        />
        <el-input
          v-model="bizNo"
          placeholder="业务单号 / 订单号"
          clearable
          class="biz-no"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="changeType" placeholder="全部变动类型" clearable class="change-type">
          <el-option
            v-for="option in CHANGE_TYPE_OPTIONS"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="logs" empty-text="暂无库存流水">
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="商品 / SKU" min-width="210">
            <template #default="{ row }">
              <div class="product-cell">
                <strong>{{ row.productName || `商品 #${row.productId}` }}</strong>
                <span class="subtle mono">{{ row.skuCode || `SKU #${row.skuId}` }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="变动" width="120">
            <template #default="{ row }">
              <span :class="row.quantityDelta > 0 ? 'delta-in' : 'delta-out'">
                {{ row.quantityDelta > 0 ? '+' : '' }}{{ row.quantityDelta }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="变动后" width="100">
            <template #default="{ row }">
              <span class="mono">{{ row.stockBefore }} → {{ row.stockAfter }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ changeTypeText(row.changeType) }}</template>
          </el-table-column>
          <el-table-column label="业务单号" min-width="180">
            <template #default="{ row }">
              <span class="mono subtle">{{ row.bizNo || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作人" width="100">
            <template #default="{ row }">
              {{ row.operatorType }}{{ row.operatorId ? ` #${row.operatorId}` : '' }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="pagination">
        <el-pagination
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="changePage"
        />
      </div>
    </div>

    <el-dialog v-model="adjustVisible" title="调整库存" width="460px">
      <div class="adjust-sku">
        <div>
          <strong>{{ adjustForm.productName }}</strong>
          <span class="mono subtle">{{ adjustForm.skuCode }}</span>
        </div>
        <span>当前库存 {{ adjustForm.currentStock }}</span>
      </div>
      <el-form label-position="top">
        <el-form-item label="调整后库存" required>
          <el-input-number v-model="adjustForm.stock" :min="0" :max="99999999" :step="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustForm.remark" maxlength="255" placeholder="例如：盘点补货" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.alert-card {
  padding: 20px 20px 8px;
  margin-bottom: 18px;
  background:
    radial-gradient(circle at 92% 0%, rgb(239 138 99 / 12%), transparent 18rem),
    #fffdf9;
  border: 1px solid var(--mall-line);
  border-radius: 16px;
}

.alert-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 0 4px 14px;
}

.alert-head h2 {
  margin: 2px 0 4px;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 22px;
  font-weight: 500;
}

.alert-head p {
  margin: 0;
  color: var(--mall-muted);
  font-size: 11px;
}

.alert-count {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: var(--mall-primary-dark);
}

.alert-count strong {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 32px;
  font-weight: 400;
}

.alert-count span {
  color: var(--mall-muted);
  font-size: 10px;
}

.alert-card :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
}

.stock-low {
  color: var(--el-color-danger);
  font-weight: 700;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.sku-id {
  width: 120px;
}

.biz-no {
  width: 240px;
}

.change-type {
  width: 160px;
}

.table-wrap {
  padding: 0 14px;
}

.product-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 12px;
}

.delta-in {
  color: var(--el-color-success);
  font-weight: 700;
}

.delta-out {
  color: var(--el-color-danger);
  font-weight: 700;
}

.adjust-sku {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 18px;
  background: var(--mall-surface-soft);
  border: 1px solid var(--mall-line);
  border-radius: 10px;
}

.adjust-sku div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.adjust-sku > span {
  color: var(--mall-muted);
  font-size: 11px;
}

@media (max-width: 720px) {
  .filter-actions {
    width: 100%;
    margin-left: 0;
  }

  .alert-head {
    flex-direction: column;
  }
}
</style>
