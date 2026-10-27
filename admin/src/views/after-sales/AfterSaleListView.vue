<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  approveAfterSale,
  confirmAfterSaleReturn,
  fetchAfterSaleDetail,
  fetchAfterSales,
  refundAfterSale,
  rejectAfterSale,
} from '@/api/afterSales'
import type { AdminAfterSaleDetail, AdminAfterSaleSummary, AfterSaleStatus } from '@/types/admin'
import { formatDateTime, formatPrice } from '@/utils/format'
import {
  AFTER_SALE_STATUS_OPTIONS,
  afterSaleStatusTagType,
  afterSaleStatusText,
  afterSaleTypeText,
} from '@/utils/afterSale'

const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const error = ref('')
const items = ref<AdminAfterSaleSummary[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const afterSaleNo = ref('')
const orderNo = ref('')
const status = ref('')

const detailVisible = ref(false)
const detail = ref<AdminAfterSaleDetail | null>(null)

const refundVisible = ref(false)
const refundForm = reactive({
  afterSaleNo: '',
  manualRefundNo: '',
  remark: '',
})

const detailActions = computed(() => {
  const value = detail.value
  if (!value) {
    return { canApprove: false, canReject: false, canConfirmReturn: false, canRefund: false }
  }
  return {
    canApprove: value.status === 'APPLIED',
    canReject: value.status === 'APPLIED',
    canConfirmReturn: value.status === 'RETURNED',
    canRefund:
      value.type === 'REFUND_ONLY'
        ? value.status === 'APPROVED' || value.status === 'REFUND_FAILED'
        : value.status === 'RETURN_RECEIVED' || value.status === 'REFUND_FAILED',
  }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchAfterSales({
      page: page.value,
      pageSize: pageSize.value,
      afterSaleNo: afterSaleNo.value.trim() || undefined,
      orderNo: orderNo.value.trim() || undefined,
      status: status.value || undefined,
    })
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '售后单加载失败'
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void load()
}

function resetFilters() {
  afterSaleNo.value = ''
  orderNo.value = ''
  status.value = ''
  search()
}

async function openDetail(row: AdminAfterSaleSummary) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchAfterSaleDetail(row.afterSaleNo)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '售后详情加载失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function approve() {
  if (!detail.value || saving.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '审核通过后，仅退款将进入待退款；退货退款将等待用户寄回商品。',
      '审核通过',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  saving.value = true
  try {
    detail.value = await approveAfterSale(detail.value.afterSaleNo)
    ElMessage.success('已审核通过')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '审核失败')
  } finally {
    saving.value = false
  }
}

async function reject() {
  if (!detail.value || saving.value) {
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('请填写拒绝原因', '拒绝售后', {
      inputValue: '不符合售后条件',
      inputValidator: (input) => (input && input.trim().length > 0 ? true : '请输入拒绝原因'),
      type: 'warning',
    })
    saving.value = true
    detail.value = await rejectAfterSale(detail.value.afterSaleNo, value.trim())
    ElMessage.success('已拒绝售后')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
    }
  } finally {
    saving.value = false
  }
}

async function confirmReturn() {
  if (!detail.value || saving.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '确认已收到用户退回的商品？确认后可发起退款，库存将在退款成功时回补。',
      '确认收到退货',
      { confirmButtonText: '确认收货', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  saving.value = true
  try {
    detail.value = await confirmAfterSaleReturn(detail.value.afterSaleNo)
    ElMessage.success('已确认收到退货')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  } finally {
    saving.value = false
  }
}

function openRefund() {
  if (!detail.value) {
    return
  }
  refundForm.afterSaleNo = detail.value.afterSaleNo
  refundForm.manualRefundNo = ''
  refundForm.remark = ''
  refundVisible.value = true
}

async function submitRefund() {
  if (!detail.value || saving.value) {
    return
  }
  saving.value = true
  try {
    detail.value = await refundAfterSale(
      refundForm.afterSaleNo,
      refundForm.manualRefundNo,
      refundForm.remark,
    )
    if (detail.value.status === 'REFUNDED') {
      ElMessage.success('退款成功')
    } else if (detail.value.status === 'REFUND_FAILED') {
      ElMessage.error(detail.value.refund?.failedReason || '退款失败，请重试')
    }
    refundVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '退款失败')
  } finally {
    saving.value = false
  }
}

function changePage(value: number) {
  page.value = value
  void load()
}

function statusLabel(value: AfterSaleStatus | null): string {
  return value ? afterSaleStatusText(value) : '创建'
}

function refundStatusText(value?: string): string {
  if (value === 'SUCCESS') return '已退款'
  if (value === 'FAILED') return '退款失败'
  if (value === 'PENDING') return '退款中'
  return value || '—'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="AFTER SALES"
      title="售后管理"
      description="审核退款与退货退款申请。仅待审核可审核；收到退货后才能退款；退款金额由服务端计算。"
    />

    <el-alert v-if="error" type="error" :closable="false" :title="error">
      <template #default>
        <el-button link type="primary" @click="load">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="afterSaleNo"
          placeholder="售后单号"
          clearable
          class="after-sale-no"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-input
          v-model="orderNo"
          placeholder="订单号"
          clearable
          class="order-no"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option
            v-for="option in AFTER_SALE_STATUS_OPTIONS"
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
        <el-table v-loading="loading" :data="items" empty-text="暂无售后单">
          <el-table-column label="售后单号" min-width="220">
            <template #default="{ row }">
              <div class="order-no-cell">
                <span class="mono">{{ row.afterSaleNo }}</span>
                <span class="subtle">订单 {{ row.orderNo }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">{{ afterSaleTypeText(row.type) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="afterSaleStatusTagType(row.status)" effect="light">
                {{ afterSaleStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="退款金额" width="120">
            <template #default="{ row }">
              <span class="amount">{{ formatPrice(row.refundAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="商品" min-width="180">
            <template #default="{ row }">
              <div class="product-cell">
                <span>{{ row.firstProductName }}</span>
                <span class="subtle">共 {{ row.totalQuantity }} 件</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
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

    <el-dialog v-model="detailVisible" title="售后详情" width="920px" top="4vh">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-hero">
            <div>
              <p class="detail-eyebrow">AFTER SALES REFERENCE</p>
              <h3 class="mono">{{ detail.afterSaleNo }}</h3>
              <p class="detail-time">
                订单 {{ detail.orderNo }} · 申请于 {{ formatDateTime(detail.createdAt) }}
              </p>
            </div>
            <div class="detail-hero-right">
              <el-tag :type="afterSaleStatusTagType(detail.status)" effect="light">
                {{ afterSaleStatusText(detail.status) }}
              </el-tag>
              <strong>{{ formatPrice(detail.refundAmount) }}</strong>
              <span>{{ afterSaleTypeText(detail.type) }}</span>
            </div>
          </div>

          <div class="detail-grid">
            <section class="detail-card">
              <p class="detail-label">申请原因</p>
              <strong>{{ detail.reason }}</strong>
              <span>{{ detail.description || '无补充说明' }}</span>
            </section>
            <section class="detail-card">
              <p class="detail-label">退货物流</p>
              <strong>{{ detail.returnCompany || '未填写' }}</strong>
              <span>{{ detail.returnTrackingNo || '—' }}</span>
              <span v-if="detail.reviewRemark" class="detail-remark">
                审核备注：{{ detail.reviewRemark }}
              </span>
            </section>
          </div>

          <section class="detail-section">
            <div class="section-head">
              <h4>售后商品</h4>
              <span>{{ detail.items.length }} 个明细</span>
            </div>
            <el-table :data="detail.items" size="small">
              <el-table-column prop="productName" label="商品" min-width="180" />
              <el-table-column prop="skuSpec" label="规格" min-width="120" />
              <el-table-column label="单价" width="100">
                <template #default="{ row }">{{ formatPrice(row.unitPrice) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="70" />
              <el-table-column label="退款金额" width="110">
                <template #default="{ row }">{{ formatPrice(row.refundAmount) }}</template>
              </el-table-column>
            </el-table>
          </section>

          <div class="detail-bottom">
            <section class="timeline-card">
              <div class="section-head">
                <h4>售后记录</h4>
              </div>
              <el-timeline v-if="detail.logs.length">
                <el-timeline-item
                  v-for="log in detail.logs"
                  :key="`${log.createdAt}-${log.toStatus}`"
                  :timestamp="formatDateTime(log.createdAt)"
                >
                  <strong>{{ statusLabel(log.fromStatus) }} → {{ statusLabel(log.toStatus) }}</strong>
                  <p class="log-meta">{{ log.operatorType }} {{ log.remark }}</p>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无记录" :image-size="54" />
            </section>

            <section class="amount-card">
              <div class="amount-row"><span>退款金额</span><strong>{{ formatPrice(detail.refundAmount) }}</strong></div>
              <div class="amount-row"><span>退款渠道</span><span>{{ detail.refund?.channel || '未发起' }}</span></div>
              <div class="amount-row"><span>退款状态</span><span>{{ refundStatusText(detail.refund?.status) }}</span></div>
              <div class="amount-row"><span>退款单号</span><span class="mono">{{ detail.refund?.refundNo || '—' }}</span></div>
              <div v-if="detail.refund?.failedReason" class="amount-row fail">
                <span>失败原因</span><span>{{ detail.refund.failedReason }}</span>
              </div>
            </section>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailActions.canReject" type="danger" plain :loading="saving" @click="reject">
          拒绝
        </el-button>
        <el-button v-if="detailActions.canApprove" type="primary" :loading="saving" @click="approve">
          审核通过
        </el-button>
        <el-button
          v-if="detailActions.canConfirmReturn"
          type="primary"
          :loading="saving"
          @click="confirmReturn"
        >
          确认收到退货
        </el-button>
        <el-button v-if="detailActions.canRefund" type="primary" :loading="saving" @click="openRefund">
          发起退款
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundVisible" title="发起退款" width="480px">
      <el-alert
        type="info"
        :closable="false"
        title="开发环境未填写凭证时使用模拟退款；生产未配置退款渠道时必须填写线下退款凭证。"
        class="refund-tip"
      />
      <el-form label-position="top">
        <el-form-item label="线下退款凭证">
          <el-input
            v-model="refundForm.manualRefundNo"
            maxlength="64"
            placeholder="转账流水号 / 退款凭证号（模拟退款可留空）"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="refundForm.remark" maxlength="255" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.after-sale-no {
  width: 230px;
}

.order-no {
  width: 210px;
}

.table-wrap {
  padding: 0 14px;
}

.order-no-cell,
.product-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 12px;
}

.amount {
  color: var(--mall-primary-dark);
  font-weight: 700;
}

.detail-body {
  min-height: 160px;
}

.detail-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 22px;
  margin-bottom: 16px;
  color: #f7f0e9;
  background:
    radial-gradient(circle at 90% 0%, rgb(239 138 99 / 28%), transparent 14rem),
    linear-gradient(145deg, #241c16, #1c1611);
  border-radius: 16px;
}

.detail-eyebrow {
  margin: 0 0 6px;
  color: rgb(247 240 233 / 38%);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2px;
}

.detail-hero h3 {
  margin: 0;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.detail-time {
  margin: 7px 0 0;
  color: rgb(247 240 233 / 42%);
  font-size: 10px;
}

.detail-hero-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.detail-hero-right strong {
  color: #f4a284;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 28px;
  font-weight: 400;
}

.detail-hero-right span {
  color: rgb(247 240 233 / 40%);
  font-size: 10px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.detail-card {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 16px;
  background: var(--mall-surface-soft);
  border: 1px solid var(--mall-line);
  border-radius: 12px;
}

.detail-label {
  margin: 0 0 2px;
  color: var(--mall-faint);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.4px;
}

.detail-card strong {
  font-size: 12px;
}

.detail-card span {
  color: var(--mall-muted);
  font-size: 11px;
  line-height: 1.6;
}

.detail-remark {
  padding-top: 6px;
  margin-top: 4px;
  border-top: 1px dashed var(--mall-line);
}

.detail-section {
  margin-bottom: 18px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-head h4 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
}

.section-head span {
  color: var(--mall-faint);
  font-size: 10px;
}

.detail-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(240px, 0.6fr);
  gap: 16px;
}

.timeline-card,
.amount-card {
  padding: 16px;
  border: 1px solid var(--mall-line);
  border-radius: 12px;
}

.timeline-card :deep(.el-timeline) {
  padding-left: 2px;
}

.timeline-card :deep(.el-timeline-item__timestamp) {
  font-size: 10px;
}

.timeline-card strong {
  font-size: 11px;
}

.log-meta {
  margin: 3px 0 0;
  color: var(--mall-faint);
  font-size: 10px;
}

.amount-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--mall-surface-soft);
}

.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--mall-muted);
  font-size: 11px;
}

.amount-row strong {
  color: var(--mall-primary-dark);
  font-size: 18px;
}

.amount-row.fail {
  color: var(--el-color-danger);
}

.refund-tip {
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .detail-grid,
  .detail-bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .filter-actions {
    width: 100%;
    margin-left: 0;
  }

  .detail-hero-right {
    align-items: flex-start;
  }
}
</style>
