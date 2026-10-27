<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchNotifications } from '@/api/notifications'
import type { AdminNotification } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const STATUS_OPTIONS = [
  { label: '已发送', value: 'SENT' },
  { label: '失败', value: 'FAILED' },
  { label: '已跳过', value: 'SKIPPED' },
  { label: '待发送', value: 'PENDING' },
]

const loading = ref(false)
const error = ref('')
const items = ref<AdminNotification[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const status = ref('')
const templateCode = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchNotifications({
      page: page.value,
      pageSize: pageSize.value,
      status: status.value || undefined,
      templateCode: templateCode.value.trim() || undefined,
    })
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '通知记录加载失败'
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
  status.value = ''
  templateCode.value = ''
  search()
}

function changePage(value: number) {
  page.value = value
  void load()
}

function statusType(value: string): 'success' | 'danger' | 'warning' | 'info' {
  if (value === 'SENT') return 'success'
  if (value === 'FAILED') return 'danger'
  if (value === 'PENDING') return 'warning'
  return 'info'
}

function statusText(value: string): string {
  return STATUS_OPTIONS.find((item) => item.value === value)?.label ?? value
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="NOTIFICATIONS"
      title="通知记录"
      description="站内通知流水。支付、发货、售后审核与退款成功会写入通知记录；外部订阅消息需后续接入模板。"
    />

    <el-alert v-if="error" type="error" :closable="false" :title="error">
      <template #default>
        <el-button link type="primary" @click="load">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="templateCode"
          placeholder="模板编码"
          clearable
          class="template-code"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option
            v-for="option in STATUS_OPTIONS"
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
        <el-table v-loading="loading" :data="items" empty-text="暂无通知记录">
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="接收人" width="140">
            <template #default="{ row }">
              <div class="recipient">
                <strong>{{ row.recipientType === 'ADMIN' ? '管理员' : `用户 #${row.recipientId}` }}</strong>
                <span class="subtle">{{ row.channel }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="模板" width="200">
            <template #default="{ row }">
              <span class="mono subtle">{{ row.templateCode }}</span>
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="260">
            <template #default="{ row }">
              <div class="content-cell">
                <strong>{{ row.title }}</strong>
                <span>{{ row.content }}</span>
                <span v-if="row.bizNo" class="subtle mono">{{ row.bizType }} {{ row.bizNo }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="light" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="150">
            <template #default="{ row }">
              <span class="subtle">{{ row.errorMessage || '—' }}</span>
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
.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.template-code {
  width: 220px;
}

.table-wrap {
  padding: 0 14px;
}

.recipient,
.content-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 12px;
}

.content-cell span {
  color: var(--mall-muted);
  font-size: 11px;
  line-height: 1.5;
}

@media (max-width: 720px) {
  .filter-actions {
    width: 100%;
    margin-left: 0;
  }
}
</style>
