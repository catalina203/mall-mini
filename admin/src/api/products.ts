import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminProductDetail, AdminProductSummary } from '@/types/admin'

export interface ProductQuery {
  page: number
  pageSize: number
  status?: string
  categoryId?: number
  keyword?: string
}

export interface ProductSkuPayload {
  id?: number
  skuCode: string
  spec: Record<string, string>
  price: number
  stock: number
  status: string
}

export interface ProductPayload {
  name: string
  subtitle: string
  description: string
  categoryId: number
  mainImage: string
  sortOrder: number
  status: string
  images: Array<{ url: string; sortOrder: number }>
  skus: ProductSkuPayload[]
}

export async function fetchProducts(params: ProductQuery): Promise<PageResult<AdminProductSummary>> {
  const response = await http.get<ApiResponse<PageResult<AdminProductSummary>>>('/admin/products', {
    params,
  })
  return response.data.data
}

export async function fetchProductDetail(id: number): Promise<AdminProductDetail> {
  const response = await http.get<ApiResponse<AdminProductDetail>>(`/admin/products/${id}`)
  return response.data.data
}

export async function createProduct(payload: ProductPayload): Promise<AdminProductDetail> {
  const response = await http.post<ApiResponse<AdminProductDetail>>('/admin/products', payload)
  return response.data.data
}

export async function updateProduct(id: number, payload: ProductPayload): Promise<AdminProductDetail> {
  const response = await http.put<ApiResponse<AdminProductDetail>>(`/admin/products/${id}`, payload)
  return response.data.data
}

export async function updateProductStatus(id: number, status: string): Promise<void> {
  await http.patch(`/admin/products/${id}/status`, { status })
}

export async function updateSkuStock(skuId: number, stock: number): Promise<void> {
  await http.patch(`/admin/skus/${skuId}/stock`, { stock })
}

export async function deleteProduct(id: number): Promise<void> {
  await http.delete(`/admin/products/${id}`)
}
