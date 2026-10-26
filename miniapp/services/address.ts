import { request } from './request'
import type { Address, AddressPayload } from '../types/api'

export function listAddresses(): Promise<Address[]> {
  return request<Address[]>({ url: '/me/addresses' })
}

export function createAddress(payload: AddressPayload): Promise<Address> {
  return request<Address>({ url: '/me/addresses', method: 'POST', data: payload })
}

export function updateAddress(id: number, payload: AddressPayload): Promise<Address> {
  return request<Address>({ url: `/me/addresses/${id}`, method: 'PUT', data: payload })
}

export function deleteAddress(id: number): Promise<void> {
  return request<void>({ url: `/me/addresses/${id}`, method: 'DELETE' })
}

export function setDefaultAddress(id: number): Promise<void> {
  return request<void>({ url: `/me/addresses/${id}/default`, method: 'PUT' })
}
