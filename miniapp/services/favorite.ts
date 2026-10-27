import { request } from './request'
import type { FavoriteProduct, PageResult } from '../types/api'

export function isFavorite(productId: number): Promise<boolean> {
  return request<boolean>({ url: `/me/favorites/${productId}/status` })
}

export function addFavorite(productId: number): Promise<void> {
  return request<void>({ url: `/me/favorites/${productId}`, method: 'POST' })
}

export function removeFavorite(productId: number): Promise<void> {
  return request<void>({ url: `/me/favorites/${productId}`, method: 'DELETE' })
}

export function listFavorites(page = 1, pageSize = 20): Promise<PageResult<FavoriteProduct>> {
  return request<PageResult<FavoriteProduct>>({
    url: `/me/favorites?page=${page}&pageSize=${pageSize}`,
  })
}

export function recordHistory(productId: number): Promise<void> {
  return request<void>({ url: `/me/browse-history/${productId}`, method: 'POST' })
}

export function listHistory(page = 1, pageSize = 20): Promise<PageResult<FavoriteProduct>> {
  return request<PageResult<FavoriteProduct>>({
    url: `/me/browse-history?page=${page}&pageSize=${pageSize}`,
  })
}

export function clearHistory(): Promise<void> {
  return request<void>({ url: '/me/browse-history', method: 'DELETE' })
}
