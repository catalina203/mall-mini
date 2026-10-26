<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowRight,
  Box,
  Clock,
  Goods,
  FolderOpened,
  Picture,
  ShoppingCart,
  Tickets,
  TrendCharts,
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchDashboardSummary } from '@/api/dashboard'
import { useAuthStore } from '@/stores/auth'
import type { DashboardSummary } from '@/types/admin'
import { formatDateTime, formatPrice } from '@/utils/format'

const auth = useAuthStore()
const days = ref(30)
const loading = ref(false)
const error = ref('')
const summary = ref<DashboardSummary | null>(null)

const greeting = computed(() => {
  const hour = new Date().getHours()
  const prefix = hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'
  const name = auth.admin?.displayName || auth.admin?.username || '管理员'
  return `${prefix}，${name}`
})

const rangeText = computed(() => {
  if (!summary.value) {
    return '正在获取经营数据'
  }
  return `${formatDateTime(summary.value.rangeStart)} — ${formatDateTime(summary.value.rangeEnd)}`
})

const metrics = computed(() => [
  {
    label: '订单总数',
    value: summary.value ? String(summary.value.orderCount) : '—',
    hint: `近 ${days.value} 天创建`,
    icon: Tickets,
    tone: 'sand',
  },
  {
    label: '待发货',
    value: summary.value ? String(summary.value.pendingShipmentCount) : '—',
    hint: '需要尽快处理',
    icon: Box,
    tone: 'amber',
  },
  {
    label: '待收货',
    value: summary.value ? String(summary.value.pendingReceiptCount) : '—',
    hint: '在途订单',
    icon: Clock,
    tone: 'sage',
  },
  {
    label: '今日订单',
    value: summary.value ? String(summary.value.todayOrderCount) : '—',
    hint: '今日 00:00 起',
    icon: ShoppingCart,
    tone: 'rose',
  },
])

const quickActions = [
  { title: '商品管理', desc: '上下架、SKU 与库存', to: '/products', icon: Goods },
  { title: '订单处理', desc: '发货、物流与订单详情', to: '/orders', icon: Tickets },
  { title: '分类配置', desc: '两级分类与排序', to: '/categories', icon: FolderOpened },
  { title: '运营内容', desc: '轮播图与跳转配置', to: '/banners', icon: Picture },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    summary.value = await fetchDashboardSummary(days.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '统计数据加载失败'
    summary.value = null
  } finally {
    loading.value = false
  }
}

function changeDays(value: number) {
  days.value = value
  void load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="OVERVIEW"
      title="经营概览"
      description="把订单、收入与待处理事项放在同一张视图里，快速判断今天先做什么。"
    >
      <template #actions>
        <el-radio-group :model-value="days" size="small" @change="changeDays(Number($event))">
          <el-radio-button :value="7">7 天</el-radio-button>
          <el-radio-button :value="30">30 天</el-radio-button>
          <el-radio-button :value="90">90 天</el-radio-button>
        </el-radio-group>
      </template>
    </PageHeader>

    <el-alert v-if="error" type="error" :closable="false" :title="error" class="error">
      <template #default>
        <el-button link type="primary" @click="load">重新加载</el-button>
      </template>
    </el-alert>

    <section v-loading="loading" class="overview-grid">
      <div class="revenue-card">
        <div class="revenue-top">
          <span class="revenue-icon"><el-icon><TrendCharts /></el-icon></span>
          <span class="revenue-range">近 {{ days }} 天</span>
        </div>
        <p class="revenue-label">{{ greeting }}</p>
        <strong class="revenue-value">
          {{ summary ? formatPrice(summary.paidAmount) : '—' }}
        </strong>
        <p class="revenue-caption">已支付订单销售额</p>
        <div class="revenue-foot">
          <span>{{ rangeText }}</span>
        </div>
      </div>

      <div class="metric-grid">
        <article
          v-for="item in metrics"
          :key="item.label"
          class="metric-card"
          :class="`tone-${item.tone}`"
        >
          <span class="metric-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <div class="metric-body">
            <span class="metric-label">{{ item.label }}</span>
            <strong class="metric-value">{{ item.value }}</strong>
            <span class="metric-hint">{{ item.hint }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="lower-grid">
      <div class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-eyebrow">QUICK ACCESS</p>
            <h2>常用操作</h2>
          </div>
          <span class="panel-note">按日常运营频率排列</span>
        </div>
        <div class="action-grid">
          <router-link v-for="action in quickActions" :key="action.to" :to="action.to" class="action-card">
            <span class="action-icon"><el-icon><component :is="action.icon" /></el-icon></span>
            <span class="action-copy">
              <strong>{{ action.title }}</strong>
              <small>{{ action.desc }}</small>
            </span>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </router-link>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-eyebrow">TO-DO</p>
            <h2>待处理</h2>
          </div>
          <router-link to="/orders" class="panel-link">全部订单</router-link>
        </div>
        <div class="todo-list">
          <router-link to="/orders" class="todo-item">
            <span class="todo-dot todo-dot-amber"></span>
            <span class="todo-copy">
              <strong>待发货订单</strong>
              <small>确认物流信息后点击发货</small>
            </span>
            <span class="todo-count">{{ summary?.pendingShipmentCount ?? '—' }}</span>
          </router-link>
          <router-link to="/orders" class="todo-item">
            <span class="todo-dot todo-dot-sage"></span>
            <span class="todo-copy">
              <strong>待收货订单</strong>
              <small>关注物流并处理售后咨询</small>
            </span>
            <span class="todo-count">{{ summary?.pendingReceiptCount ?? '—' }}</span>
          </router-link>
          <router-link to="/products" class="todo-item">
            <span class="todo-dot todo-dot-rose"></span>
            <span class="todo-copy">
              <strong>商品运营</strong>
              <small>检查库存与在售状态</small>
            </span>
            <el-icon class="todo-arrow"><ArrowRight /></el-icon>
          </router-link>
        </div>
        <p v-if="summary" class="panel-foot">
          数据生成于 {{ formatDateTime(summary.generatedAt) }}
        </p>
      </div>
    </section>

    <p class="footnote">
      统计口径：订单数为所选区间内创建的订单；销售额为支付状态 PAID 的订单应付金额之和；待发货、待收货为当前全部订单。
    </p>
  </div>
</template>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(0, 1.6fr);
  gap: 18px;
}

.revenue-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 272px;
  padding: 26px;
  overflow: hidden;
  color: #f8f1ea;
  background:
    radial-gradient(circle at 88% 0%, rgb(239 138 99 / 32%), transparent 16rem),
    linear-gradient(145deg, #241c16, #1b1510 70%);
  border-radius: var(--mall-radius-lg);
  box-shadow: 0 24px 50px rgb(35 29 24 / 18%);
}

.revenue-card::after {
  position: absolute;
  right: -60px;
  bottom: -120px;
  width: 300px;
  height: 300px;
  border: 1px solid rgb(255 255 255 / 7%);
  border-radius: 50%;
  content: '';
}

.revenue-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.revenue-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: #f2a385;
  background: rgb(255 255 255 / 7%);
  border: 1px solid rgb(255 255 255 / 8%);
  border-radius: 10px;
}

.revenue-range {
  color: rgb(248 241 234 / 42%);
  font-size: 10px;
  letter-spacing: 1px;
}

.revenue-label {
  margin: 28px 0 8px;
  color: rgb(248 241 234 / 55%);
  font-size: 12px;
}

.revenue-value {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(34px, 3.6vw, 46px);
  font-weight: 400;
  letter-spacing: -1px;
}

.revenue-caption {
  margin: 6px 0 0;
  color: rgb(248 241 234 / 35%);
  font-size: 11px;
}

.revenue-foot {
  padding-top: 16px;
  margin-top: auto;
  color: rgb(248 241 234 / 32%);
  font-size: 10px;
  border-top: 1px solid rgb(255 255 255 / 8%);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  background: var(--mall-surface);
  border: 1px solid rgb(235 226 215 / 85%);
  border-radius: var(--mall-radius-md);
  box-shadow: var(--mall-shadow-sm);
}

.metric-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
}

.tone-sand .metric-icon {
  color: #8a6a45;
  background: #f4ecdf;
}

.tone-amber .metric-icon {
  color: #a66a16;
  background: #fbf0dc;
}

.tone-sage .metric-icon {
  color: #2f7d63;
  background: #e9f3ee;
}

.tone-rose .metric-icon {
  color: #c04e35;
  background: #fbe9e2;
}

.metric-body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.metric-label {
  color: var(--mall-muted);
  font-size: 11px;
}

.metric-value {
  color: var(--mall-ink);
  font-size: 26px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.5px;
}

.metric-hint {
  color: var(--mall-faint);
  font-size: 10px;
}

.lower-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.8fr);
  gap: 18px;
}

.panel {
  padding: 22px;
  background: var(--mall-surface);
  border: 1px solid rgb(235 226 215 / 85%);
  border-radius: var(--mall-radius-lg);
  box-shadow: var(--mall-shadow-sm);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-eyebrow {
  margin: 0 0 4px;
  color: var(--mall-faint);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2px;
}

.panel-head h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.panel-note {
  color: var(--mall-faint);
  font-size: 10px;
}

.panel-link {
  color: var(--mall-primary);
  font-size: 11px;
  font-weight: 600;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: var(--mall-surface-soft);
  border: 1px solid transparent;
  border-radius: 12px;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    transform 0.18s ease;
}

.action-card:hover {
  background: #fffaf5;
  border-color: var(--mall-line-strong);
  transform: translateY(-1px);
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: var(--mall-primary-dark);
  background: var(--mall-primary-soft);
  border-radius: 10px;
}

.action-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.action-copy strong {
  font-size: 12px;
  font-weight: 600;
}

.action-copy small {
  color: var(--mall-faint);
  font-size: 10px;
}

.action-arrow {
  color: var(--mall-faint);
  font-size: 13px;
}

.todo-list {
  display: flex;
  flex-direction: column;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--mall-line);
}

.todo-item:last-child {
  border-bottom: none;
}

.todo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.todo-dot-amber {
  background: #d29a3f;
  box-shadow: 0 0 0 4px rgb(210 154 63 / 12%);
}

.todo-dot-sage {
  background: var(--mall-success);
  box-shadow: 0 0 0 4px rgb(47 125 99 / 12%);
}

.todo-dot-rose {
  background: var(--mall-primary);
  box-shadow: 0 0 0 4px rgb(217 84 54 / 12%);
}

.todo-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.todo-copy strong {
  font-size: 12px;
  font-weight: 600;
}

.todo-copy small {
  color: var(--mall-faint);
  font-size: 10px;
}

.todo-count {
  color: var(--mall-ink);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.4px;
}

.todo-arrow {
  color: var(--mall-faint);
  font-size: 14px;
}

.panel-foot {
  padding-top: 14px;
  margin: 6px 0 0;
  color: var(--mall-faint);
  font-size: 10px;
  border-top: 1px dashed var(--mall-line);
}

.footnote {
  margin: 0;
  color: var(--mall-faint);
  font-size: 10px;
  line-height: 1.7;
}

@media (max-width: 1180px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .revenue-card {
    min-height: 220px;
  }

  .lower-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .metric-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    padding: 16px;
  }
}
</style>
