<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  createCoupon,
  deleteCoupon,
  fetchCoupons,
  updateCoupon,
  updateCouponStatus,
  type CouponPayload,
} from '@/api/coupons'
import type { AdminCouponTemplate } from '@/types/admin'
import { formatDateTime, formatPrice } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const items = ref<AdminCouponTemplate[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<CouponPayload>({
  name: '',
  type: 'FIXED',
  value: 10,
  minAmount: 0,
  scope: 'ALL',
  scopeId: undefined,
  totalCount: 100,
  perUserLimit: 1,
  validFrom: '',
  validTo: '',
  status: 'ENABLED',
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await fetchCoupons()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '优惠券加载失败'
    items.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    type: 'FIXED',
    value: 10,
    minAmount: 0,
    scope: 'ALL',
    scopeId: undefined,
    totalCount: 100,
    perUserLimit: 1,
    validFrom: '',
    validTo: '',
    status: 'ENABLED',
  })
  dialogVisible.value = true
}

function openEdit(row: AdminCouponTemplate) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    type: row.type,
    value: row.value,
    minAmount: row.minAmount,
    scope: row.scope,
    scopeId: row.scopeId ?? undefined,
    totalCount: row.totalCount,
    perUserLimit: 1,
    validFrom: row.validFrom,
    validTo: row.validTo,
    status: row.status,
  })
  dialogVisible.value = true
}

async function submit() {
  if (saving.value) return
  if (!form.name.trim() || !form.validFrom || !form.validTo) {
    ElMessage.warning('请填写名称和有效期')
    return
  }
  if (form.scope !== 'ALL' && !form.scopeId) {
    ElMessage.warning('请填写适用分类/商品 ID')
    return
  }
  saving.value = true
  try {
    const payload: CouponPayload = {
      ...form,
      name: form.name.trim(),
      scopeId: form.scope === 'ALL' ? undefined : form.scopeId,
    }
    if (editingId.value) {
      await updateCoupon(editingId.value, payload)
    } else {
      await createCoupon(payload)
    }
    ElMessage.success('优惠券已保存')
    dialogVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: AdminCouponTemplate) {
  const target = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await updateCouponStatus(row.id, target)
    ElMessage.success(target === 'ENABLED' ? '已启用' : '已停用')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  }
}

async function remove(row: AdminCouponTemplate) {
  try {
    await ElMessageBox.confirm(`确定删除优惠券「${row.name}」？`, '删除优惠券', { type: 'warning' })
    await deleteCoupon(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '删除失败')
    }
  }
}

function valueText(row: AdminCouponTemplate): string {
  return row.type === 'FIXED' ? formatPrice(row.value) : `${row.value}% off`
}

function scopeText(row: AdminCouponTemplate): string {
  if (row.scope === 'ALL') return '全场'
  return `${row.scope === 'CATEGORY' ? '分类' : '商品'} #${row.scopeId}`
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader eyebrow="MARKETING" title="优惠券" description="配置满减/折扣券，用户领取后下单使用。首版每人限领 1 张。">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增优惠券</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="error" type="error" :closable="false" :title="error" />

    <div class="table-card">
      <div class="table-wrap">
        <el-table v-loading="loading" :data="items" empty-text="暂无优惠券">
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column label="类型/面额" width="130">
            <template #default="{ row }">{{ row.type === 'FIXED' ? '满减' : '折扣' }} · {{ valueText(row) }}</template>
          </el-table-column>
          <el-table-column label="门槛" width="110">
            <template #default="{ row }">{{ Number(row.minAmount) > 0 ? `满 ${formatPrice(row.minAmount)}` : '无门槛' }}</template>
          </el-table-column>
          <el-table-column label="范围" width="120">
            <template #default="{ row }">{{ scopeText(row) }}</template>
          </el-table-column>
          <el-table-column label="领取" width="110">
            <template #default="{ row }">{{ row.claimedCount }} / {{ row.totalCount || '不限' }}</template>
          </el-table-column>
          <el-table-column label="有效期" min-width="230">
            <template #default="{ row }">{{ formatDateTime(row.validFrom) }} 至 {{ formatDateTime(row.validTo) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
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
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑优惠券' : '新增优惠券'" width="560px">
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <div class="row">
          <el-form-item label="类型" required>
            <el-select v-model="form.type">
              <el-option label="满减券" value="FIXED" />
              <el-option label="折扣券（百分比 off）" value="PERCENT" />
            </el-select>
          </el-form-item>
          <el-form-item :label="form.type === 'FIXED' ? '减免金额' : '折扣百分比'" required>
            <el-input-number v-model="form.value" :min="0.01" :max="form.type === 'PERCENT' ? 100 : 99999" />
          </el-form-item>
          <el-form-item label="使用门槛" required>
            <el-input-number v-model="form.minAmount" :min="0" />
          </el-form-item>
        </div>
        <div class="row">
          <el-form-item label="适用范围" required>
            <el-select v-model="form.scope">
              <el-option label="全场" value="ALL" />
              <el-option label="指定分类" value="CATEGORY" />
              <el-option label="指定商品" value="PRODUCT" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.scope !== 'ALL'" label="分类/商品 ID" required>
            <el-input-number v-model="form.scopeId" :min="1" />
          </el-form-item>
          <el-form-item label="发放总量（0=不限）" required>
            <el-input-number v-model="form.totalCount" :min="0" />
          </el-form-item>
        </div>
        <div class="row">
          <el-form-item label="生效时间" required>
            <el-date-picker
              v-model="form.validFrom"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="开始时间"
            />
          </el-form-item>
          <el-form-item label="失效时间" required>
            <el-date-picker
              v-model="form.validTo"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="结束时间"
            />
          </el-form-item>
        </div>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button value="ENABLED">启用</el-radio-button>
            <el-radio-button value="DISABLED">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.table-wrap { padding: 0 14px; }
.row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
</style>
