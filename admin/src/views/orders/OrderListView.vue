<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  cancelOrder,
  fetchOrderDetail,
  fetchOrders,
  shipOrder,
} from '@/api/orders'
import type { AdminOrderDetail, AdminOrderSummary } from '@/types/admin'
import { formatDateTime, formatPrice } from '@/utils/format'
import { ORDER_STATUS_OPTIONS, orderStatusTagType, orderStatusText } from '@/utils/order'

const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const error = ref('')
const items = ref<AdminOrderSummary[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const orderNo = ref('')
const status = ref('')
const userId = ref<number | undefined>(undefined)

const detailVisible = ref(false)
const detail = ref<AdminOrderDetail | null>(null)
const shipVisible = ref(false)
const shipForm = reactive({
  orderNo: '',
  logisticsCompany: '',
  logisticsNo: '',
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchOrders({
      page: page.value,
      pageSize: pageSize.value,
      orderNo: orderNo.value.trim() || undefined,
      status: status.value || undefined,
      userId: userId.value || undefined,
    })
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '订单加载失败'
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
  orderNo.value = ''
  status.value = ''
  userId.value = undefined
  search()
}

async function openDetail(row: AdminOrderSummary) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchOrderDetail(row.orderNo)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '订单详情加载失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function openShip(row: AdminOrderSummary) {
  shipForm.orderNo = row.orderNo
  shipForm.logisticsCompany = ''
  shipForm.logisticsNo = ''
  shipVisible.value = true
}

async function submitShip() {
  if (saving.value) {
    return
  }
  if (!shipForm.logisticsCompany.trim() || !shipForm.logisticsNo.trim()) {
    ElMessage.warning('请填写物流公司与物流单号')
    return
  }
  saving.value = true
  try {
    detail.value = await shipOrder(
      shipForm.orderNo,
      shipForm.logisticsCompany.trim(),
      shipForm.logisticsNo.trim(),
    )
    ElMessage.success('已发货')
    shipVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '发货失败')
  } finally {
    saving.value = false
  }
}

async function cancel(row: AdminOrderSummary) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      inputValue: '管理员取消',
      inputValidator: (input) => (input && input.trim().length > 0 ? true : '请输入取消原因'),
      type: 'warning',
    })
    await cancelOrder(row.orderNo, value.trim())
    ElMessage.success('订单已取消')
    await load()
    if (detailVisible.value && detail.value?.orderNo === row.orderNo) {
      detail.value = await fetchOrderDetail(row.orderNo)
    }
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '取消失败')
    }
  }
}

function changePage(value: number) {
  page.value = value
  void load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="TRANSACTIONS"
      title="订单管理"
      description="查看订单明细与状态流转，处理待发货订单。仅待发货可发货，仅待付款可取消。"
    />

    <el-alert v-if="error" type="error" :closable="false" :title="error">
      <template #default>
        <el-button link type="primary" @click="load">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="orderNo"
          placeholder="订单号"
          clearable
          class="order-no"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-input
          v-model.number="userId"
          placeholder="用户 ID"
          clearable
          class="user-id"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option
            v-for="option in ORDER_STATUS_OPTIONS"
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
        <el-table v-loading="loading" :data="items" empty-text="暂无订单">
          <el-table-column label="订单号" min-width="220">
            <template #default="{ row }">
              <div class="order-no-cell">
                <span class="mono">{{ row.orderNo }}</span>
                <span class="subtle">用户 #{{ row.userId }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="orderStatusTagType(row.status)" effect="light">
                {{ orderStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">
              <span class="amount">{{ formatPrice(row.payableAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalQuantity" label="件数" width="76" />
          <el-table-column label="收货人" width="130">
            <template #default="{ row }">
              <div class="name">{{ row.receiverName }}</div>
              <div class="subtle mono">{{ row.receiverPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="row.status === 'PENDING_SHIPMENT'"
                link
                type="primary"
                @click="openShip(row)"
              >发货</el-button>
              <el-button
                v-if="row.status === 'PENDING_PAYMENT'"
                link
                type="danger"
                @click="cancel(row)"
              >取消</el-button>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="920px" top="4vh">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-hero">
            <div>
              <p class="detail-eyebrow">ORDER REFERENCE</p>
              <h3 class="mono">{{ detail.orderNo }}</h3>
              <p class="detail-time">下单于 {{ formatDateTime(detail.createdAt) }}</p>
            </div>
            <div class="detail-hero-right">
              <el-tag :type="orderStatusTagType(detail.status)" effect="light">
                {{ orderStatusText(detail.status) }}
              </el-tag>
              <strong>{{ formatPrice(detail.payableAmount) }}</strong>
              <span>支付状态 {{ detail.paymentStatus }}</span>
            </div>
          </div>

          <div class="detail-grid">
            <section class="detail-card">
              <p class="detail-label">收货信息</p>
              <strong>{{ detail.address.receiverName }} · {{ detail.address.phone }}</strong>
              <span>
                {{ detail.address.province }}{{ detail.address.city }}{{ detail.address.district }}{{ detail.address.detail }}
              </span>
            </section>
            <section class="detail-card">
              <p class="detail-label">物流与备注</p>
              <strong>{{ detail.logisticsCompany || '尚未发货' }}</strong>
              <span>{{ detail.logisticsNo || '—' }}</span>
              <span class="detail-remark">{{ detail.remark || '无订单备注' }}</span>
            </section>
          </div>

          <section class="detail-section">
            <div class="section-head">
              <h4>商品清单</h4>
              <span>{{ detail.items.length }} 个商品</span>
            </div>
            <el-table :data="detail.items" size="small">
              <el-table-column prop="productName" label="商品" min-width="180" />
              <el-table-column prop="skuSpec" label="规格" min-width="120" />
              <el-table-column label="单价" width="100">
                <template #default="{ row }">{{ formatPrice(row.unitPrice) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="70" />
              <el-table-column label="小计" width="110">
                <template #default="{ row }">{{ formatPrice(row.subtotal) }}</template>
              </el-table-column>
            </el-table>
          </section>

          <div class="detail-bottom">
            <section class="timeline-card">
              <div class="section-head">
                <h4>状态记录</h4>
              </div>
              <el-timeline v-if="detail.statusLogs.length">
                <el-timeline-item
                  v-for="log in detail.statusLogs"
                  :key="`${log.createdAt}-${log.toStatus}`"
                  :timestamp="formatDateTime(log.createdAt)"
                >
                  <strong>
                    {{ log.fromStatus ? orderStatusText(log.fromStatus) : '创建' }} →
                    {{ orderStatusText(log.toStatus) }}
                  </strong>
                  <p class="log-meta">{{ log.operatorType }} {{ log.remark }}</p>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无状态记录" :image-size="54" />
            </section>

            <section class="amount-card">
              <div class="amount-row"><span>商品金额</span><span>{{ formatPrice(detail.goodsAmount) }}</span></div>
              <div class="amount-row"><span>运费</span><span>{{ formatPrice(detail.shippingFee) }}</span></div>
              <div class="amount-row"><span>优惠</span><span>-{{ formatPrice(detail.discountAmount) }}</span></div>
              <div class="amount-row amount-total">
                <span>应付金额</span>
                <strong>{{ formatPrice(detail.payableAmount) }}</strong>
              </div>
            </section>
          </div>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="订单发货" width="480px">
      <div class="ship-order">
        <span>订单号</span>
        <strong class="mono">{{ shipForm.orderNo }}</strong>
      </div>
      <el-form label-position="top">
        <el-form-item label="物流公司" required>
          <el-input v-model="shipForm.logisticsCompany" maxlength="64" placeholder="例如：顺丰速运" />
        </el-form-item>
        <el-form-item label="物流单号" required>
          <el-input v-model="shipForm.logisticsNo" maxlength="64" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitShip">确认发货</el-button>
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

.order-no {
  width: 230px;
}

.user-id {
  width: 130px;
}

.table-wrap {
  padding: 0 14px;
}

.order-no-cell {
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
  grid-template-columns: minmax(0, 1.4fr) minmax(220px, 0.6fr);
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
  color: var(--mall-muted);
  font-size: 11px;
}

.amount-total {
  padding-top: 10px;
  margin-top: 4px;
  border-top: 1px dashed var(--mall-line);
}

.amount-total strong {
  color: var(--mall-primary-dark);
  font-size: 19px;
}

.ship-order {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  margin-bottom: 18px;
  background: var(--mall-surface-soft);
  border: 1px solid var(--mall-line);
  border-radius: 10px;
}

.ship-order span {
  color: var(--mall-muted);
  font-size: 10px;
}

.ship-order strong {
  font-size: 11px;
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
