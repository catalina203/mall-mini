<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  createCategory,
  deleteCategory,
  fetchCategories,
  fetchCategoryOptions,
  updateCategory,
  updateCategoryStatus,
  type CategoryPayload,
} from '@/api/categories'
import type { AdminCategory } from '@/types/admin'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const items = ref<AdminCategory[]>([])
const options = ref<AdminCategory[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const status = ref('')

const dialogVisible = ref(false)
const editingId = ref(0)
const form = reactive<CategoryPayload>({
  name: '',
  parentId: 0,
  iconUrl: '',
  sortOrder: 0,
  status: 'ENABLED',
})

const parentOptions = computed(() =>
  options.value.filter((category) => category.parentId === 0 && category.id !== editingId.value),
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [pageResult, categoryOptions] = await Promise.all([
      fetchCategories({
        page: page.value,
        pageSize: pageSize.value,
        status: status.value || undefined,
        keyword: keyword.value.trim() || undefined,
      }),
      fetchCategoryOptions(),
    ])
    items.value = pageResult.items
    total.value = pageResult.total
    options.value = categoryOptions
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '分类加载失败'
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
  editingId.value = 0
  Object.assign(form, {
    name: '',
    parentId: 0,
    iconUrl: '',
    sortOrder: 0,
    status: 'ENABLED',
  })
  dialogVisible.value = true
}

function openEdit(row: AdminCategory) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    parentId: row.parentId,
    iconUrl: row.iconUrl,
    sortOrder: row.sortOrder,
    status: row.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (saving.value) {
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, { ...form, name: form.name.trim() })
    } else {
      await createCategory({ ...form, name: form.name.trim() })
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: AdminCategory) {
  const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${next === 'ENABLED' ? '启用' : '停用'}分类「${row.name}」？`,
      '修改分类状态',
      { type: 'warning' },
    )
    await updateCategoryStatus(row.id, next)
    ElMessage.success('状态已更新')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
    }
  }
}

async function remove(row: AdminCategory) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '删除分类', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '删除失败')
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
      eyebrow="CATALOG"
      title="分类管理"
      description="维护两级分类结构，控制用户端分类入口与商品归属。有子分类或关联商品时不可删除。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
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
          placeholder="搜索分类名称"
          clearable
          class="search"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="items" empty-text="暂无分类">
          <el-table-column prop="id" label="ID" width="76" />
          <el-table-column label="分类" min-width="210">
            <template #default="{ row }">
              <div class="category-cell">
                <span class="category-avatar">{{ row.name.slice(0, 1) }}</span>
                <div>
                  <div class="name">{{ row.name }}</div>
                  <div class="subtle">{{ row.parentName ? `隶属于 ${row.parentName}` : '顶级分类' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="toggleStatus(row)">
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      width="520px"
    >
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-form-item label="分类名称" required>
              <el-input v-model="form.name" maxlength="64" placeholder="例如：数码、耳机" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="99999" class="full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="父分类">
          <el-select v-model="form.parentId" class="full" placeholder="选择父分类">
            <el-option label="顶级分类" :value="0" />
            <el-option
              v-for="option in parentOptions"
              :key="option.id"
              :label="option.name"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图标地址">
          <el-input v-model="form.iconUrl" maxlength="512" placeholder="可选，填写图片 URL" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存分类</el-button>
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

.table-wrap {
  padding: 0 14px;
}

.category-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: var(--mall-primary-dark);
  font-size: 13px;
  font-weight: 700;
  background: var(--mall-primary-soft);
  border-radius: 10px;
}

.full {
  width: 100%;
}

@media (max-width: 720px) {
  .filter-actions {
    width: 100%;
    margin-left: 0;
  }
}
</style>
