<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { approveReview, fetchReviews, rejectReview, replyReview } from '@/api/reviews'
import type { AdminReview } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const items = ref<AdminReview[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const status = ref('PENDING')
const productId = ref<number | undefined>(undefined)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchReviews({
      page: page.value,
      pageSize: pageSize.value,
      status: status.value || undefined,
      productId: productId.value || undefined,
    })
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '评价加载失败'
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
  status.value = 'PENDING'
  productId.value = undefined
  search()
}

async function approve(row: AdminReview) {
  if (saving.value) return
  saving.value = true
  try {
    await approveReview(row.id)
    ElMessage.success('已通过')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  } finally {
    saving.value = false
  }
}

async function reject(row: AdminReview) {
  if (saving.value) return
  saving.value = true
  try {
    await rejectReview(row.id)
    ElMessage.success('已拒绝')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  } finally {
    saving.value = false
  }
}

async function reply(row: AdminReview) {
  try {
    const { value } = await ElMessageBox.prompt('请输入商家回复', '回复评价', {
      inputValue: row.reply || '',
      inputValidator: (input) => (input && input.trim().length > 0 ? true : '请输入回复内容'),
    })
    await replyReview(row.id, value.trim())
    ElMessage.success('回复已保存')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '回复失败')
    }
  }
}

function changePage(value: number) {
  page.value = value
  void load()
}

function stars(rating: number): string {
  return '★★★★★'.slice(0, rating) + '☆☆☆☆☆'.slice(0, 5 - rating)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="CONTENT"
      title="评价管理"
      description="审核用户评价；通过后展示在商品评价页。支持商家回复。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="error" type="error" :closable="false" :title="error" />

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model.number="productId"
          placeholder="商品 ID"
          clearable
          class="product-id"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="items" empty-text="暂无评价">
          <el-table-column label="商品" width="120">
            <template #default="{ row }"><span class="mono">#{{ row.productId }}</span></template>
          </el-table-column>
          <el-table-column label="用户" width="130">
            <template #default="{ row }">{{ row.userNickname }}</template>
          </el-table-column>
          <el-table-column label="评分" width="130">
            <template #default="{ row }">
              <span class="stars">{{ stars(row.rating) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="260">
            <template #default="{ row }">
              <div class="content-cell">
                <span>{{ row.content || '（无文字评价）' }}</span>
                <span v-if="row.reply" class="reply">商家回复：{{ row.reply }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'APPROVED' ? 'success' : row.status === 'PENDING' ? 'warning' : 'info'"
                effect="light"
              >
                {{ row.status === 'APPROVED' ? '已通过' : row.status === 'PENDING' ? '待审核' : '已拒绝' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" link type="primary" @click="approve(row)">通过</el-button>
              <el-button v-if="row.status === 'PENDING'" link type="danger" @click="reject(row)">拒绝</el-button>
              <el-button link type="primary" @click="reply(row)">回复</el-button>
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
.product-id { width: 140px; }
.table-wrap { padding: 0 14px; }
.stars { color: #f59e0b; font-size: 12px; }
.content-cell { display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.reply { color: var(--mall-muted); font-size: 11px; }
@media (max-width: 720px) { .filter-actions { width: 100%; margin-left: 0; } }
</style>
