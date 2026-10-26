<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchCategoryOptions } from '@/api/categories'
import {
  createProduct,
  deleteProduct,
  fetchProductDetail,
  fetchProducts,
  updateProduct,
  updateProductStatus,
  type ProductPayload,
} from '@/api/products'
import { uploadImage } from '@/api/upload'
import type { AdminCategory, AdminProductSummary } from '@/types/admin'
import { formatDateTime, formatPriceRange } from '@/utils/format'

interface SkuRow {
  id?: number
  skuCode: string
  specText: string
  price: number
  stock: number
  status: string
}

interface ProductForm {
  name: string
  subtitle: string
  description: string
  categoryId: number
  mainImage: string
  sortOrder: number
  status: string
  imagesText: string
  skus: SkuRow[]
}

function emptyForm(): ProductForm {
  return {
    name: '',
    subtitle: '',
    description: '',
    categoryId: 0,
    mainImage: '',
    sortOrder: 0,
    status: 'OFF_SALE',
    imagesText: '',
    skus: [{ skuCode: '', specText: '', price: 0, stock: 0, status: 'ENABLED' }],
  }
}

const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const uploading = ref(false)
const error = ref('')
const items = ref<AdminProductSummary[]>([])
const categories = ref<AdminCategory[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const status = ref('')
const categoryId = ref<number | undefined>(undefined)

const dialogVisible = ref(false)
const editingId = ref(0)
const form = reactive<ProductForm>(emptyForm())

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [result, categoryOptions] = await Promise.all([
      fetchProducts({
        page: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value.trim() || undefined,
        status: status.value || undefined,
        categoryId: categoryId.value,
      }),
      fetchCategoryOptions(),
    ])
    items.value = result.items
    total.value = result.total
    categories.value = categoryOptions
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '商品加载失败'
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
  categoryId.value = undefined
  search()
}

function resetForm() {
  Object.assign(form, emptyForm())
}

function openCreate() {
  editingId.value = 0
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: AdminProductSummary) {
  editingId.value = row.id
  detailLoading.value = true
  dialogVisible.value = true
  try {
    const detail = await fetchProductDetail(row.id)
    Object.assign(form, {
      name: detail.name,
      subtitle: detail.subtitle,
      description: detail.description || '',
      categoryId: detail.categoryId,
      mainImage: detail.mainImage,
      sortOrder: detail.sortOrder,
      status: detail.status,
      imagesText: detail.images.map((image) => image.url).join('\n'),
      skus: detail.skus.map((sku) => ({
        id: sku.id,
        skuCode: sku.skuCode,
        specText: Object.entries(sku.spec).map(([key, value]) => `${key}:${value}`).join(','),
        price: Number(sku.price),
        stock: sku.stock,
        status: sku.status,
      })),
    })
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '商品详情加载失败')
    dialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function addSku() {
  form.skus.push({ skuCode: '', specText: '', price: 0, stock: 0, status: 'ENABLED' })
}

function removeSku(index: number) {
  if (form.skus.length <= 1) {
    ElMessage.warning('至少保留一个 SKU')
    return
  }
  form.skus.splice(index, 1)
}

function parseSpec(text: string): Record<string, string> {
  const spec: Record<string, string> = {}
  const segments = text.split(/[,，;；]/).map((item) => item.trim()).filter(Boolean)
  for (const segment of segments) {
    const match = segment.split(/[:：]/)
    if (match.length < 2) {
      throw new Error('规格格式应为「属性:值」，多项用逗号分隔')
    }
    const key = match[0].trim()
    const value = match.slice(1).join(':').trim()
    if (!key || !value) {
      throw new Error('规格属性与值不能为空')
    }
    spec[key] = value
  }
  return spec
}

function buildPayload(): ProductPayload {
  if (!form.name.trim()) {
    throw new Error('请输入商品名称')
  }
  if (!form.categoryId) {
    throw new Error('请选择分类')
  }
  if (!form.skus.length) {
    throw new Error('至少需要一个 SKU')
  }
  const skus = form.skus.map((sku) => {
    if (!sku.skuCode.trim()) {
      throw new Error('SKU 编码不能为空')
    }
    if (sku.price < 0 || sku.stock < 0) {
      throw new Error('价格与库存不能为负')
    }
    return {
      id: sku.id,
      skuCode: sku.skuCode.trim(),
      spec: parseSpec(sku.specText),
      price: Number(sku.price),
      stock: Number(sku.stock),
      status: sku.status,
    }
  })
  const images = form.imagesText
    .split('\n')
    .map((url) => url.trim())
    .filter(Boolean)
    .map((url, index) => ({ url, sortOrder: index }))

  return {
    name: form.name.trim(),
    subtitle: form.subtitle.trim(),
    description: form.description,
    categoryId: form.categoryId,
    mainImage: form.mainImage.trim(),
    sortOrder: form.sortOrder,
    status: form.status,
    images,
    skus,
  }
}

async function save() {
  if (saving.value) {
    return
  }
  let payload: ProductPayload
  try {
    payload = buildPayload()
  } catch (cause) {
    ElMessage.warning(cause instanceof Error ? cause.message : '请检查表单')
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      await updateProduct(editingId.value, payload)
    } else {
      await createProduct(payload)
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

async function toggleStatus(row: AdminProductSummary) {
  const next = row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  try {
    await ElMessageBox.confirm(
      `确定${next === 'ON_SALE' ? '上架' : '下架'}商品「${row.name}」？`,
      '修改商品状态',
      { type: 'warning' },
    )
    await updateProductStatus(row.id, next)
    ElMessage.success('状态已更新')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
    }
  }
}

async function remove(row: AdminProductSummary) {
  try {
    await ElMessageBox.confirm(
      `确定删除商品「${row.name}」？已被订单引用的商品不能删除。`,
      '删除商品',
      { type: 'warning' },
    )
    await deleteProduct(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '删除失败')
    }
  }
}

async function uploadMainImage(options: UploadRequestOptions) {
  uploading.value = true
  try {
    form.mainImage = await uploadImage(options.file)
    ElMessage.success('图片已上传')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '上传失败')
  } finally {
    uploading.value = false
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
      eyebrow="GOODS"
      title="商品管理"
      description="维护商品信息、图片、SKU 与库存。已被订单引用的商品只能下架，不能删除。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
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
          placeholder="搜索商品名称或副标题"
          clearable
          class="search"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="categoryId" placeholder="全部分类" clearable filterable class="category">
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.parentName ? `${category.parentName} / ${category.name}` : category.name"
            :value="category.id"
          />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable class="status">
          <el-option label="在售" value="ON_SALE" />
          <el-option label="下架" value="OFF_SALE" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="items" empty-text="暂无商品">
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="product-cell">
                <el-image :src="row.mainImage" fit="cover" class="product-thumb">
                  <template #error><div class="product-fallback">无图</div></template>
                </el-image>
                <div class="product-copy">
                  <div class="name">{{ row.name }}</div>
                  <div class="subtle">{{ row.subtitle || '暂无副标题' }}</div>
                  <div class="product-meta">
                    <span>ID {{ row.id }}</span>
                    <span>{{ row.categoryName || '未分类' }}</span>
                    <span>{{ row.skuCount }} 个 SKU</span>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="价格" width="170">
            <template #default="{ row }">
              <span class="price">{{ formatPriceRange(row.minPrice, row.maxPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="100">
            <template #default="{ row }">
              <span
                class="stock"
                :class="{ 'stock-low': row.totalStock > 0 && row.totalStock < 10, 'stock-empty': row.totalStock <= 0 }"
              >
                {{ row.totalStock }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ON_SALE' ? '在售' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="toggleStatus(row)">
                {{ row.status === 'ON_SALE' ? '下架' : '上架' }}
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
      :title="editingId ? '编辑商品' : '新增商品'"
      width="960px"
      top="3vh"
    >
      <div v-loading="detailLoading" class="product-form">
        <el-form label-position="top">
          <div class="dialog-section">
            <div class="dialog-section-head">
              <span class="section-index">01</span>
              <div>
                <h3>基础信息</h3>
                <p>用于用户端商品展示与检索</p>
              </div>
            </div>
            <el-row :gutter="16">
              <el-col :span="14">
                <el-form-item label="商品名称" required>
                  <el-input v-model="form.name" maxlength="128" placeholder="请输入商品名称" />
                </el-form-item>
              </el-col>
              <el-col :span="10">
                <el-form-item label="分类" required>
                  <el-select v-model="form.categoryId" class="full" filterable placeholder="选择分类">
                    <el-option
                      v-for="category in categories"
                      :key="category.id"
                      :label="category.parentName ? `${category.parentName} / ${category.name}` : category.name"
                      :value="category.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="副标题">
              <el-input v-model="form.subtitle" maxlength="255" placeholder="一句话卖点，可选" />
            </el-form-item>
            <el-form-item label="商品描述">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="商品详情描述"
              />
            </el-form-item>
          </div>

          <div class="dialog-section">
            <div class="dialog-section-head">
              <span class="section-index">02</span>
              <div>
                <h3>媒体与展示</h3>
                <p>主图用于列表，更多图片用于详情</p>
              </div>
            </div>
            <el-row :gutter="16">
              <el-col :span="16">
                <el-form-item label="主图">
                  <div class="image-input">
                    <div class="image-preview">
                      <el-image v-if="form.mainImage" :src="form.mainImage" fit="cover" />
                      <span v-else>无图</span>
                    </div>
                    <div class="image-input-main">
                      <el-input v-model="form.mainImage" placeholder="图片 URL 或点击上传" />
                      <el-upload
                        :show-file-list="false"
                        :http-request="uploadMainImage"
                        accept="image/png,image/jpeg,image/webp"
                      >
                        <el-button :icon="Upload" :loading="uploading">上传主图</el-button>
                      </el-upload>
                    </div>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="排序">
                  <el-input-number v-model="form.sortOrder" :min="0" :max="99999" class="full" />
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="状态">
                  <el-select v-model="form.status" class="full">
                    <el-option label="下架" value="OFF_SALE" />
                    <el-option label="在售" value="ON_SALE" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="详情图片">
              <el-input
                v-model="form.imagesText"
                type="textarea"
                :rows="2"
                placeholder="每行一个图片 URL"
              />
            </el-form-item>
          </div>

          <div class="dialog-section sku-section">
            <div class="dialog-section-head">
              <span class="section-index">03</span>
              <div>
                <h3>SKU 与库存</h3>
                <p>规格格式：颜色:黑色,尺码:L</p>
              </div>
              <el-button class="add-sku" :icon="Plus" @click="addSku">添加 SKU</el-button>
            </div>
            <el-table :data="form.skus" size="small" class="sku-table">
              <el-table-column label="SKU 编码" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row.skuCode" maxlength="64" placeholder="SKU 编码" />
                </template>
              </el-table-column>
              <el-table-column label="规格" min-width="190">
                <template #default="{ row }">
                  <el-input v-model="row.specText" placeholder="颜色:黑色,尺码:L" />
                </template>
              </el-table-column>
              <el-table-column label="价格" width="130">
                <template #default="{ row }">
                  <el-input-number v-model="row.price" :min="0" :precision="2" :step="1" />
                </template>
              </el-table-column>
              <el-table-column label="库存" width="118">
                <template #default="{ row }">
                  <el-input-number v-model="row.stock" :min="0" :step="1" />
                </template>
              </el-table-column>
              <el-table-column label="状态" width="108">
                <template #default="{ row }">
                  <el-select v-model="row.status">
                    <el-option label="启用" value="ENABLED" />
                    <el-option label="停用" value="DISABLED" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="" width="56">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeSku($index)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <p class="hint">
              上架商品至少需要一个启用的 SKU；删除已被订单引用的 SKU 会被系统保留并停用。
            </p>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存商品</el-button>
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

.category {
  width: 200px;
}

.table-wrap {
  padding: 0 14px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.product-thumb,
.product-fallback {
  width: 54px;
  height: 54px;
  flex-shrink: 0;
  border-radius: 11px;
}

.product-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mall-faint);
  font-size: 10px;
  background: var(--mall-surface-soft);
}

.product-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--mall-faint);
  font-size: 10px;
}

.price {
  color: var(--mall-ink);
  font-weight: 600;
}

.stock {
  font-weight: 600;
}

.stock-low {
  color: var(--mall-warning);
}

.stock-empty {
  color: var(--mall-danger);
}

.product-form {
  max-height: 70vh;
  padding-right: 4px;
  overflow-y: auto;
}

.dialog-section {
  padding: 4px 0 10px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--mall-line);
}

.dialog-section:last-of-type {
  border-bottom: none;
}

.dialog-section-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.section-index {
  padding-top: 2px;
  color: var(--mall-primary);
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}

.dialog-section-head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}

.dialog-section-head p {
  margin: 3px 0 0;
  color: var(--mall-faint);
  font-size: 10px;
}

.add-sku {
  margin-left: auto;
}

.image-input {
  display: flex;
  gap: 12px;
  width: 100%;
}

.image-preview {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  overflow: hidden;
  color: var(--mall-faint);
  font-size: 9px;
  background: var(--mall-surface-soft);
  border: 1px solid var(--mall-line);
  border-radius: 10px;
}

.image-preview .el-image {
  width: 100%;
  height: 100%;
}

.image-input-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}

.full {
  width: 100%;
}

.sku-table {
  background: var(--mall-surface-soft);
  border: 1px solid var(--mall-line);
  border-radius: 12px;
}

.hint {
  margin: 10px 0 0;
  color: var(--mall-faint);
  font-size: 10px;
  line-height: 1.6;
}

@media (max-width: 720px) {
  .filter-actions {
    width: 100%;
    margin-left: 0;
  }

  .product-form {
    max-height: none;
  }
}
</style>
