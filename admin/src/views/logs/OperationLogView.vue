<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchOperationLogs } from '@/api/operationLogs'
import type { AdminOperationLog } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const items = ref<AdminOperationLog[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const adminId = ref<number | undefined>(undefined)
const action = ref('')
const targetType = ref('')

const TARGET_TYPES = [
  { label: '订单', value: 'ORDER' },
  { label: '售后', value: 'AFTER_SALE' },
  { label: '商品', value: 'PRODUCT' },
  { label: 'SKU', value: 'SKU' },
  { label: '分类', value: 'CATEGORY' },
  { label: '用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' },
  { label: '库存', value: 'STOCK' },
  { label: '轮播图', value: 'BANNER' },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchOperationLogs({
      page: page.value,
      pageSize: pageSize.value,
      adminId: adminId.value || undefined,
      action: action.value.trim() || undefined,
      targetType: targetType.value || undefined,
    })
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '操作日志加载失败'
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
  adminId.value = undefined
  action.value = ''
  targetType.value = ''
  search()
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
      eyebrow="AUDIT TRAIL"
      title="操作日志"
      description="关键后台操作审计。日志包含操作人、动作、对象与 requestId，不记录密码或令牌。"
    />

    <el-alert v-if="error" type="error" :closable="false" :title="error">
      <template #default>
        <el-button link type="primary" @click="load">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model.number="adminId"
          placeholder="管理员 ID"
          clearable
          class="admin-id"
          @keyup.enter="search"
          @clear="search"
        />
        <el-input
          v-model="action"
          placeholder="动作，如 ORDER_SHIP"
          clearable
          class="action"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="targetType" placeholder="全部对象" clearable class="target">
          <el-option
            v-for="option in TARGET_TYPES"
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
        <el-table v-loading="loading" :data="items" empty-text="暂无操作日志">
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="管理员" width="150">
            <template #default="{ row }">
              <div class="admin-cell">
                <strong>{{ row.adminUsername || `#${row.adminId}` }}</strong>
                <span class="subtle mono">ID {{ row.adminId }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="动作" width="180">
            <template #default="{ row }"><span class="mono">{{ row.action }}</span></template>
          </el-table-column>
          <el-table-column label="对象" width="180">
            <template #default="{ row }">
              <span>{{ row.targetType || '—' }}</span>
              <span class="subtle mono"> {{ row.targetId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
          <el-table-column label="Request ID" width="190">
            <template #default="{ row }"><span class="mono subtle">{{ row.requestId || '—' }}</span></template>
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

.admin-id {
  width: 130px;
}

.action {
  width: 210px;
}

.target {
  width: 140px;
}

.table-wrap {
  padding: 0 14px;
}

.admin-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 12px;
}

@media (max-width: 720px) {
  .filter-actions {
    width: 100%;
    margin-left: 0;
  }
}
</style>
