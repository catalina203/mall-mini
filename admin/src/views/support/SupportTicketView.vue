<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  closeSupportTicket,
  fetchSupportTickets,
  replySupportTicket,
} from '@/api/support'
import type { AdminSupportTicket } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const STATUS_TEXT: Record<string, string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已回复',
  CLOSED: '已关闭',
}
const TYPE_TEXT: Record<string, string> = {
  ORDER: '订单',
  PRODUCT: '商品',
  REFUND: '退款/售后',
  OTHER: '其他',
}

const loading = ref(false)
const error = ref('')
const items = ref<AdminSupportTicket[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const status = ref('OPEN')
const type = ref('')
const keyword = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchSupportTickets({
      page: page.value,
      pageSize: pageSize.value,
      status: status.value || undefined,
      type: type.value || undefined,
      keyword: keyword.value.trim() || undefined,
    })
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '工单加载失败'
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
  status.value = 'OPEN'
  type.value = ''
  keyword.value = ''
  search()
}

async function reply(row: AdminSupportTicket) {
  try {
    const { value } = await ElMessageBox.prompt('请输入回复内容', `回复 ${row.ticketNo}`, {
      inputValue: row.reply || '',
      inputValidator: (input) => (input && input.trim().length > 0 ? true : '请输入回复内容'),
    })
    await replySupportTicket(row.ticketNo, value.trim())
    ElMessage.success('回复已发送')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '回复失败')
    }
  }
}

async function close(row: AdminSupportTicket) {
  try {
    await ElMessageBox.confirm(`确定关闭工单 ${row.ticketNo}？`, '关闭工单', { type: 'warning' })
    await closeSupportTicket(row.ticketNo)
    ElMessage.success('已关闭')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
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
      eyebrow="CUSTOMER CARE"
      title="客服工单"
      description="处理用户提交的订单/商品/退款问题。回复后状态变为已回复，可手动关闭。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="error" type="error" :closable="false" :title="error" />

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="工单号 / 标题 / 订单号"
          clearable
          class="keyword"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option label="待处理" value="OPEN" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已回复" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-select v-model="type" placeholder="全部类型" clearable class="type">
          <el-option label="订单" value="ORDER" />
          <el-option label="商品" value="PRODUCT" />
          <el-option label="退款/售后" value="REFUND" />
          <el-option label="其他" value="OTHER" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="items" empty-text="暂无工单">
          <el-table-column label="工单" min-width="220">
            <template #default="{ row }">
              <div class="ticket-cell">
                <strong>{{ row.subject }}</strong>
                <span class="subtle mono">{{ row.ticketNo }}</span>
                <span v-if="row.orderNo" class="subtle mono">订单 {{ row.orderNo }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="用户" width="130">
            <template #default="{ row }">{{ row.userNickname }} #{{ row.userId }}</template>
          </el-table-column>
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ TYPE_TEXT[row.type] ?? row.type }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'OPEN' ? 'warning' : row.status === 'RESOLVED' ? 'success' : 'info'"
                effect="light"
              >
                {{ STATUS_TEXT[row.status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="260">
            <template #default="{ row }">
              <div class="content-cell">
                <span>{{ row.content }}</span>
                <span v-if="row.reply" class="reply">回复：{{ row.reply }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="reply(row)">回复</el-button>
              <el-button v-if="row.status !== 'CLOSED'" link type="danger" @click="close(row)">关闭</el-button>
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
  </div>
</template>

<style scoped>
.filter-actions { display: flex; gap: 8px; margin-left: auto; }
.keyword { width: 260px; }
.type { width: 120px; }
.table-wrap { padding: 0 14px; }
.ticket-cell { display: flex; flex-direction: column; gap: 3px; font-size: 12px; }
.content-cell { display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.reply { color: var(--mall-muted); font-size: 11px; }
@media (max-width: 720px) { .filter-actions { width: 100%; margin-left: 0; } }
</style>
