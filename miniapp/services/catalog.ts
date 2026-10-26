import { request } from './request'
import type { Category, HomeData, PageResult, ProductDetail, ProductSummary } from '../types/api'

export function getHome(): Promise<HomeData> {
  return request<HomeData>({ url: '/home' })
}

export function getCategories(): Promise<Category[]> {
  return request<Category[]>({ url: '/categories' })
}

export function getProduct(id: number | string): Promise<ProductDetail> {
  return request<ProductDetail>({ url: `/products/${id}` })
}

export interface ProductListParams {
  page?: number
  pageSize?: number
  categoryId?: number
  keyword?: string
  sort?: 'new' | 'price_asc' | 'price_desc'
}

export function listProducts(params: ProductListParams = {}): Promise<PageResult<ProductSummary>> {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  return request<PageResult<ProductSummary>>({
    url: query ? `/products?${query}` : '/products',
  })
}
