import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'

export interface AdminCategory {
  id: number
  name: string
  iconUrl: string
  sortOrder: number
  children: AdminCategory[]
}

export interface AdminProductSummary {
  id: number
  name: string
  subtitle: string
  mainImage: string
  minPrice: number | string
  maxPrice: number | string
  totalStock: number
}

export interface ProductListParams {
  page: number
  pageSize: number
  keyword?: string
}

export async function fetchProducts(params: ProductListParams): Promise<PageResult<AdminProductSummary>> {
  const response = await http.get<ApiResponse<PageResult<AdminProductSummary>>>('/products', { params })
  return response.data.data
}

export async function fetchCategories(): Promise<AdminCategory[]> {
  const response = await http.get<ApiResponse<AdminCategory[]>>('/categories')
  return response.data.data
}
