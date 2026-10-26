<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { Plus, Refresh, Upload } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  createBanner,
  deleteBanner,
  fetchBanners,
  updateBanner,
  updateBannerStatus,
  type BannerPayload,
} from '@/api/banners'
import { uploadImage } from '@/api/upload'
import type { AdminBanner } from '@/types/admin'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const items = ref<AdminBanner[]>([])
const statusFilter = ref('')

const dialogVisible = ref(false)
const editingId = ref(0)
const form = reactive<BannerPayload>({
  title: '',
  imageUrl: '',
  linkType: 'NONE',
  linkTarget: '',
  sortOrder: 0,
  status: 'ENABLED',
})

const linkTypeLabels: Record<string, string> = {
  NONE: '无跳转',
  PRODUCT: '商品',
  CATEGORY: '分类',
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await fetchBanners(statusFilter.value || undefined)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '轮播图加载失败'
    items.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = 0
  Object.assign(form, {
    title: '',
    imageUrl: '',
    linkType: 'NONE',
    linkTarget: '',
    sortOrder: 0,
    status: 'ENABLED',
  })
  dialogVisible.value = true
}

function openEdit(row: AdminBanner) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    imageUrl: row.imageUrl,
    linkType: row.linkType,
    linkTarget: row.linkTarget,
    sortOrder: row.sortOrder,
    status: row.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (saving.value) {
    return
  }
  if (!form.imageUrl.trim()) {
    ElMessage.warning('请填写或上传轮播图片')
    return
  }
  if (form.linkType !== 'NONE' && !form.linkTarget.trim()) {
    ElMessage.warning('请填写跳转目标 ID')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      title: form.title.trim(),
      imageUrl: form.imageUrl.trim(),
      linkTarget: form.linkType === 'NONE' ? '' : form.linkTarget.trim(),
    }
    if (editingId.value) {
      await updateBanner(editingId.value, payload)
    } else {
      await createBanner(payload)
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

async function toggleStatus(row: AdminBanner) {
  const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${next === 'ENABLED' ? '启用' : '停用'}轮播图「${row.title || row.id}」？`,
      '修改轮播图状态',
      { type: 'warning' },
    )
    await updateBannerStatus(row.id, next)
    ElMessage.success('状态已更新')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
    }
  }
}

async function remove(row: AdminBanner) {
  try {
    await ElMessageBox.confirm(`确定删除轮播图「${row.title || row.id}」？`, '删除轮播图', {
      type: 'warning',
    })
    await deleteBanner(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '删除失败')
    }
  }
}

async function uploadBannerImage(options: UploadRequestOptions) {
  uploading.value = true
  try {
    form.imageUrl = await uploadImage(options.file)
    ElMessage.success('图片已上传')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="CONTENT"
      title="轮播图管理"
      description="配置首页轮播视觉与跳转目标，支持按排序展示。用户端只展示启用项。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增轮播图</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="error" type="error" :closable="false" :title="error">
      <template #default>
        <el-button link type="primary" @click="load">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable class="status" @change="load">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <span class="toolbar-note">排序值越小越靠前</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="items" empty-text="暂无轮播图">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="预览" width="170">
            <template #default="{ row }">
              <el-image :src="row.imageUrl" fit="cover" class="banner-thumb">
                <template #error><div class="banner-fallback">无图</div></template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="200">
            <template #default="{ row }">
              <div class="name">{{ row.title || '未命名轮播' }}</div>
              <div class="subtle">
                {{ linkTypeLabels[row.linkType] || row.linkType }}
                <span v-if="row.linkTarget"> · #{{ row.linkTarget }}</span>
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
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑轮播图' : '新增轮播图'"
      width="560px"
    >
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="64" placeholder="用于后台识别，可选" />
        </el-form-item>
        <el-form-item label="图片" required>
          <div class="upload-row">
            <el-input v-model="form.imageUrl" placeholder="填写图片 URL 或上传文件" />
            <el-upload
              :show-file-list="false"
              :http-request="uploadBannerImage"
              accept="image/png,image/jpeg,image/webp"
            >
              <el-button :icon="Upload" :loading="uploading">上传</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <div v-if="form.imageUrl" class="banner-preview">
          <el-image :src="form.imageUrl" fit="cover" />
          <span>图片预览</span>
        </div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="跳转类型">
              <el-select v-model="form.linkType" class="full">
                <el-option label="无跳转" value="NONE" />
                <el-option label="商品" value="PRODUCT" />
                <el-option label="分类" value="CATEGORY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.linkType !== 'NONE'" label="跳转 ID" required>
              <el-input v-model="form.linkTarget" placeholder="商品 ID / 分类 ID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="99999" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="ENABLED">启用</el-radio>
                <el-radio value="DISABLED">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存轮播图</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.table-wrap {
  padding: 0 14px;
}

.toolbar-note {
  margin-left: auto;
  color: var(--mall-faint);
  font-size: 11px;
}

.banner-thumb {
  width: 140px;
  height: 58px;
  border-radius: 9px;
}

.banner-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 140px;
  height: 58px;
  color: var(--mall-faint);
  font-size: 11px;
  background: var(--mall-surface-soft);
  border-radius: 9px;
}

.upload-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.banner-preview {
  position: relative;
  margin-bottom: 18px;
  overflow: hidden;
  border: 1px solid var(--mall-line);
  border-radius: 12px;
}

.banner-preview .el-image {
  display: block;
  width: 100%;
  height: 150px;
}

.banner-preview span {
  position: absolute;
  bottom: 8px;
  left: 10px;
  padding: 3px 8px;
  color: #fff;
  font-size: 9px;
  letter-spacing: 0.6px;
  background: rgb(35 29 24 / 52%);
  border-radius: 999px;
  backdrop-filter: blur(4px);
}

.full {
  width: 100%;
}

@media (max-width: 720px) {
  .toolbar-note {
    display: none;
  }
}
</style>
