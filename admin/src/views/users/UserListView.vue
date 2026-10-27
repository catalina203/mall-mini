<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { adjustUserPoints, fetchUsers, updateUserStatus } from '@/api/users'
import { exportUsers } from '@/api/exports'
import { useAuthStore } from '@/stores/auth'
import { downloadBlob } from '@/utils/download'
import type { AdminUser } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const loading = ref(false)
const exporting = ref(false)
const error = ref('')
const items = ref<AdminUser[]>([])
const pointsVisible = ref(false)
const pointsSaving = ref(false)
const pointsForm = reactive({ id: 0, nickname: '', delta: 0, remark: '' })
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const status = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchUsers({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
    })
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '用户加载失败'
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
  keyword.value = ''
  status.value = ''
  search()
}

async function toggleStatus(row: AdminUser) {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await ElMessageBox.confirm(
      `确定${next === 'ACTIVE' ? '启用' : '禁用'}用户「${row.nickname || row.id}」？`,
      '修改用户状态',
      { type: 'warning' },
    )
    await updateUserStatus(row.id, next)
    ElMessage.success('状态已更新')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
    }
  }
}

function openPoints(row: AdminUser) {
  pointsForm.id = row.id
  pointsForm.nickname = row.nickname || `用户 #${row.id}`
  pointsForm.delta = 0
  pointsForm.remark = ''
  pointsVisible.value = true
}

async function submitPoints() {
  if (pointsSaving.value) return
  if (!pointsForm.delta) {
    ElMessage.warning('积分变动不能为 0')
    return
  }
  pointsSaving.value = true
  try {
    await adjustUserPoints(pointsForm.id, pointsForm.delta, pointsForm.remark)
    ElMessage.success('积分已调整')
    pointsVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '调整失败')
  } finally {
    pointsSaving.value = false
  }
}

function levelText(level: string): string {
  return ({ NORMAL: '普通会员', SILVER: '银卡', GOLD: '金卡', PLATINUM: '钻石' } as Record<string, string>)[level] ?? level
}

function changePage(value: number) {
  page.value = value
  void load()
}

async function exportList() {
  if (exporting.value) return
  exporting.value = true
  try {
    const { blob, filename } = await exportUsers({
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
    })
    downloadBlob(blob, filename)
    ElMessage.success('导出已开始')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="CUSTOMERS"
      title="用户管理"
      description="查看小程序用户与账号状态。手机号默认脱敏展示，状态变更会写入管理员操作日志。"
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
        <el-button link type="primary" @click="load">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索昵称或手机号"
          clearable
          class="search"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="items" empty-text="暂无用户">
          <el-table-column prop="id" label="ID" width="76" />
          <el-table-column label="用户" min-width="220">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="38" :src="row.avatarUrl" class="user-avatar">
                  {{ (row.nickname || '用户').slice(0, 1) }}
                </el-avatar>
                <div>
                  <div class="name">{{ row.nickname || '未设置昵称' }}</div>
                  <div class="subtle">用户 ID · {{ row.id }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="手机号" width="160">
            <template #default="{ row }">
              <span class="mono">{{ row.phone || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="会员/积分" width="150">
            <template #default="{ row }">
              <div class="member-cell">
                <span>{{ levelText(row.memberLevel) }}</span>
                <span class="subtle">{{ row.points }} 积分</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPoints(row)">调整积分</el-button>
              <el-button link type="primary" @click="toggleStatus(row)">
                {{ row.status === 'ACTIVE' ? '禁用账号' : '启用账号' }}
              </el-button>
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

    <el-dialog v-model="pointsVisible" title="调整积分" width="420px">
      <p class="points-hint">用户：<strong>{{ pointsForm.nickname }}</strong></p>
      <el-form label-position="top">
        <el-form-item label="积分变动（正数增加，负数扣减）" required>
          <el-input-number v-model="pointsForm.delta" :step="10" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="pointsForm.remark" maxlength="255" placeholder="例如：活动补偿" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsVisible = false">取消</el-button>
        <el-button type="primary" :loading="pointsSaving" @click="submitPoints">确认调整</el-button>
      </template>
    </el-dialog>
</template>

<style scoped>
.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.table-wrap {
  padding: 0 14px;
}

.member-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 12px;
}

.points-hint {
  margin: 0 0 12px;
  color: var(--mall-muted);
  font-size: 12px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  color: var(--mall-primary-dark);
  font-size: 13px;
  font-weight: 700;
  background: var(--mall-primary-soft);
  border: 1px solid #f2d8cc;
}

@media (max-width: 720px) {
  .filter-actions {
    width: 100%;
    margin-left: 0;
  }
}
</style>
