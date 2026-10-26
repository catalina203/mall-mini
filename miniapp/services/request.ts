import { appConfig } from '../utils/config'
import type { ApiResponse } from '../types/api'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

const TOKEN_KEY = 'mallmini_token'

export function getToken(): string {
  const token = wx.getStorageSync(TOKEN_KEY)
  return typeof token === 'string' ? token : ''
}

export function setToken(token: string): void {
  wx.setStorageSync(TOKEN_KEY, token)
}

export function clearToken(): void {
  wx.removeStorageSync(TOKEN_KEY)
}

export interface RequestOptions {
  url: string
  method?: HttpMethod
  data?: WechatMiniprogram.IAnyObject
}

export function request<T>(options: RequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const token = getToken()
    wx.request({
      url: `${appConfig.apiBaseUrl}${options.url}`,
      method: options.method ?? 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success(response) {
        const body = response.data as ApiResponse<T> | undefined
        if (response.statusCode >= 200 && response.statusCode < 300 && body?.code === 'OK') {
          resolve(body.data)
          return
        }
        if (response.statusCode === 401) {
          clearToken()
        }
        reject(new Error(body?.message ?? `请求失败（${response.statusCode}）`))
      },
      fail(error) {
        reject(new Error(error.errMsg || '网络异常，请稍后重试'))
      },
    })
  })
}
