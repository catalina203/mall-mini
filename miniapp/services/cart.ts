import { request } from './request'
import type { Cart } from '../types/api'

export function getCart(): Promise<Cart> {
  return request<Cart>({ url: '/me/cart' })
}

export function addCartItem(skuId: number, quantity: number): Promise<Cart> {
  return request<Cart>({
    url: '/me/cart',
    method: 'POST',
    data: { skuId, quantity },
  })
}

export function updateCartItem(id: number, payload: { quantity?: number; selected?: boolean }): Promise<Cart> {
  return request<Cart>({ url: `/me/cart/${id}`, method: 'PUT', data: payload })
}

export function deleteCartItem(id: number): Promise<Cart> {
  return request<Cart>({ url: `/me/cart/${id}`, method: 'DELETE' })
}

export function clearCart(): Promise<Cart> {
  return request<Cart>({ url: '/me/cart', method: 'DELETE' })
}

export function selectCartItems(selected: boolean, ids?: number[]): Promise<Cart> {
  return request<Cart>({
    url: '/me/cart/selection',
    method: 'PUT',
    data: { selected, ids },
  })
}
