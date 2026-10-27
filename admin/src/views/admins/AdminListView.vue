<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  createAdmin,
  fetchAdmins,
  resetAdminPassword,
  updateAdmin,
  updateAdminStatus,
} from '@/api/adminAccounts'
import { useAuthStore } from '@/stores/auth'
import type { AdminProfile } from '@/types/admin'

const ROLE_LABELS: Record<string, string> = {
  SUPER_ADMIN: '超级管理员',
  OPERATOR: '运营',
  CUSTOMER_SERVICE: '客服',
  WAREHOUSE: '仓储',
}
const ROLE_OPTIONS = Object.entries(ROLE_LABELS).map(([value, label]) => ({ value, label }))

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const items = ref<AdminProfile[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const status = ref('')

const createVisible = ref(false)
const editVisible = ref(false)
const resetVisible = ref(false)
const createForm = reactive({
  username: '',
  displayName: '',
  password: '',
  role: 'OPERATOR',
})
const editForm = reactive({
  id: 0,
  username: '',
  displayName: '',
  role: 'OPERATOR',
})
const resetForm = reactive({
  id: 0,
  username: '',
  password: '',
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchAdmins({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
    })
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '管理员加载失败'
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

function openCreate() {
  createForm.username = ''
  createForm.displayName = ''
  createForm.password = ''
  createForm.role = 'OPERATOR'
  createVisible.value = true
}

async function submitCreate() {
  if (saving.value) return
  if (!createForm.username.trim() || createForm.password.length < 8) {
    ElMessage.warning('请填写用户名，密码至少 8 位')
    return
  }
  saving.value = true
  try {
    await createAdmin({
      username: createForm.username.trim(),
      displayName: createForm.displayName.trim(),
      password: createForm.password,
      role: createForm.role,
    })
    ElMessage.success('管理员已创建')
    createVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '创建失败')
  } finally {
    saving.value = false
  }
}

function openEdit(row: AdminProfile) {
  editForm.id = row.id
  editForm.username = row.username
  editForm.displayName = row.displayName
  editForm.role = row.role
  editVisible.value = true
}

async function submitEdit() {
  if (saving.value) return
  saving.value = true
  try {
    await updateAdmin(editForm.id, {
      displayName: editForm.displayName.trim(),
      role: editForm.role,
    })
    ElMessage.success('管理员已更新')
    editVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '更新失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: AdminProfile) {
  const target = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await ElMessageBox.confirm(
      target === 'DISABLED'
        ? `确定禁用管理员 ${row.username}？旧登录令牌将失效。`
        : `确定启用管理员 ${row.username}？`,
      target === 'DISABLED' ? '禁用管理员' : '启用管理员',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
    await updateAdminStatus(row.id, target)
    ElMessage.success(target === 'DISABLED' ? '已禁用' : '已启用')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
    }
  }
}

function openReset(row: AdminProfile) {
  resetForm.id = row.id
  resetForm.username = row.username
  resetForm.password = ''
  resetVisible.value = true
}

async function submitReset() {
  if (saving.value) return
  if (resetForm.password.length < 8) {
    ElMessage.warning('密码至少 8 位')
    return
  }
  saving.value = true
  try {
    await resetAdminPassword(resetForm.id, resetForm.password)
    ElMessage.success('密码已重置，该管理员需要重新登录')
    resetVisible.value = false
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '重置失败')
  } finally {
    saving.value = false
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
      eyebrow="ACCESS CONTROL"
      title="管理员管理"
      description="按角色控制后台权限。禁用、改角色或重置密码后，旧令牌立即失效。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增管理员</el-button>
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
          placeholder="用户名 / 显示名称"
          clearable
          class="keyword"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="items" empty-text="暂无管理员">
          <el-table-column label="管理员" min-width="200">
            <template #default="{ row }">
              <div class="admin-cell">
                <strong>{{ row.displayName || row.username }}</strong>
                <span class="subtle mono">@{{ row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="140">
            <template #default="{ row }">{{ ROLE_LABELS[row.role] ?? row.role }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="权限数量" width="100">
            <template #default="{ row }">{{ row.permissions?.length ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
              <el-button
                link
                :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                :disabled="row.id === auth.admin?.id && row.status === 'ACTIVE'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
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

    <el-dialog v-model="createVisible" title="新增管理员" width="460px">
      <el-form label-position="top">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" maxlength="64" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="createForm.displayName" maxlength="64" placeholder="默认使用用户名" />
        </el-form-item>
        <el-form-item label="初始密码" required>
          <el-input v-model="createForm.password" type="password" show-password placeholder="至少 8 位" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="createForm.role" class="full">
            <el-option v-for="role in ROLE_OPTIONS" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑管理员" width="460px">
      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="editForm.displayName" maxlength="64" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="editForm.role" class="full">
            <el-option v-for="role in ROLE_OPTIONS" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <p class="hint">修改角色或显示名称后，该管理员需要重新登录。</p>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <p class="hint">正在为 <strong>{{ resetForm.username }}</strong> 重置密码。</p>
      <el-form label-position="top">
        <el-form-item label="新密码" required>
          <el-input v-model="resetForm.password" type="password" show-password placeholder="至少 8 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReset">确认重置</el-button>
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

.keyword {
  width: 240px;
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

.full {
  width: 100%;
}

.hint {
  margin: 0 0 12px;
  color: var(--mall-muted);
  font-size: 11px;
  line-height: 1.6;
}

@media (max-width: 720px) {
  .filter-actions {
    width: 100%;
    margin-left: 0;
  }
}
</style>
