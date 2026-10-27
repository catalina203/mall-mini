import { request } from './request'

export function listHotKeywords(limit = 10): Promise<string[]> {
  return request<string[]>({ url: `/search/hot?limit=${limit}` })
}
