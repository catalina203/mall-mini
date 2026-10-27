<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  createShippingTemplate,
  deleteShippingTemplate,
  fetchShippingTemplates,
  updateShippingStatus,
  updateShippingTemplate,
  type ShippingPayload,
} from '@/api/shipping'
import type { AdminShippingRule, AdminShippingTemplate } from '@/types/admin'
import { formatPrice } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const items = ref<AdminShippingTemplate[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<ShippingPayload>({
  name: '',
  defaultFee: 10,
  freeThreshold: 99,
  status: 'ENABLED',
  rules: [],
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await fetchShippingTemplates()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '运费模板加载失败'
    items.value = []
  } finally {
    loading.value = false
  }
}

function blankRule(): AdminShippingRule {
  return {
    regionCode: '',
    firstUnit: 1,
    firstFee: 10,
    additionalUnit: 1,
    additionalFee: 0,
    freeThreshold: 99,
    status: 'ENABLED',
    sortOrder: 0,
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    defaultFee: 10,
    freeThreshold: 99,
    status: 'ENABLED',
    rules: [blankRule()],
  })
  dialogVisible.value = true
}

function openEdit(row: AdminShippingTemplate) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    defaultFee: row.defaultFee,
    freeThreshold: row.freeThreshold,
    status: row.status,
    rules: row.rules.map((rule) => ({ ...rule })),
  })
  dialogVisible.value = true
}

function addRule() {
  form.rules.push(blankRule())
}

function removeRule(index: number) {
  form.rules.splice(index, 1)
}

async function submit() {
  if (saving.value) return
  if (!form.name.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  saving.value = true
  try {
    const payload: ShippingPayload = {
      ...form,
      name: form.name.trim(),
      rules: form.rules.map((rule, index) => ({
        ...rule,
        regionCode: rule.regionCode.trim(),
        sortOrder: index,
      })),
    }
    if (editingId.value) {
      await updateShippingTemplate(editingId.value, payload)
    } else {
      await createShippingTemplate(payload)
    }
    ElMessage.success('运费模板已保存')
    dialogVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: AdminShippingTemplate) {
  const target = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await updateShippingStatus(row.id, target)
    ElMessage.success(target === 'ENABLED' ? '已启用' : '已停用')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  }
}

async function remove(row: AdminShippingTemplate) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.name}」？有商品使用时不能删除。`, '删除模板', {
      type: 'warning',
    })
    await deleteShippingTemplate(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '删除失败')
    }
  }
}

function ruleSummary(row: AdminShippingTemplate): string {
  if (!row.rules.length) return '无区域规则，使用默认运费'
  return row.rules
    .map((rule) => `${rule.regionCode || '全国'} ${formatPrice(rule.firstFee)}`)
    .join('；')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="FULFILLMENT"
      title="运费模板"
      description="按省份/全国配置首件与续件运费；商品未绑定模板时使用全局运费配置。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增模板</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="error" type="error" :closable="false" :title="error" />

    <div class="card-grid">
      <article v-for="row in items" :key="row.id" class="ship-card">
        <div class="ship-head">
          <div>
            <h3>{{ row.name }}</h3>
            <p>默认 {{ formatPrice(row.defaultFee) }} · 满 {{ formatPrice(row.freeThreshold) }} 包邮</p>
          </div>
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </div>
        <p class="rule-text">{{ ruleSummary(row) }}</p>
        <div class="ship-actions">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="toggleStatus(row)">
            {{ row.status === 'ENABLED' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </div>
      </article>
      <el-empty v-if="!loading && !items.length" description="暂无运费模板" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑运费模板' : '新增运费模板'" width="760px">
      <el-form label-position="top">
        <div class="row">
          <el-form-item label="模板名称" required>
            <el-input v-model="form.name" maxlength="64" />
          </el-form-item>
          <el-form-item label="默认运费">
            <el-input-number v-model="form.defaultFee" :min="0" />
          </el-form-item>
          <el-form-item label="默认包邮门槛">
            <el-input-number v-model="form.freeThreshold" :min="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </div>

        <div class="rule-head">
          <h4>区域规则</h4>
          <el-button size="small" :icon="Plus" @click="addRule">添加规则</el-button>
        </div>
        <el-table :data="form.rules" size="small">
          <el-table-column label="地区（空=全国）" min-width="130">
            <template #default="{ row }"><el-input v-model="row.regionCode" placeholder="如 广东省" /></template>
          </el-table-column>
          <el-table-column label="首件" width="90">
            <template #default="{ row }"><el-input-number v-model="row.firstUnit" :min="1" size="small" /></template>
          </el-table-column>
          <el-table-column label="首费" width="110">
            <template #default="{ row }"><el-input-number v-model="row.firstFee" :min="0" size="small" /></template>
          </el-table-column>
          <el-table-column label="续件" width="90">
            <template #default="{ row }"><el-input-number v-model="row.additionalUnit" :min="1" size="small" /></template>
          </el-table-column>
          <el-table-column label="续费" width="110">
            <template #default="{ row }"><el-input-number v-model="row.additionalFee" :min="0" size="small" /></template>
          </el-table-column>
          <el-table-column label="包邮门槛" width="120">
            <template #default="{ row }"><el-input-number v-model="row.freeThreshold" :min="0" size="small" /></template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeRule($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
.ship-card { display: flex; flex-direction: column; gap: 10px; padding: 20px; background: #fffdf9; border: 1px solid var(--mall-line); border-radius: 16px; }
.ship-head { display: flex; justify-content: space-between; gap: 12px; }
.ship-head h3 { margin: 0 0 4px; font-size: 16px; }
.ship-head p { margin: 0; color: var(--mall-muted); font-size: 11px; }
.rule-text { flex: 1; margin: 0; color: var(--mall-muted); font-size: 11px; line-height: 1.7; }
.ship-actions { display: flex; gap: 8px; }
.row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.rule-head { display: flex; align-items: center; justify-content: space-between; margin: 8px 0; }
.rule-head h4 { margin: 0; font-size: 13px; }
</style>
