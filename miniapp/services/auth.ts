import { request, setToken } from './request'
import type { AuthToken, User } from '../types/api'

function saveSession(session: AuthToken): AuthToken {
  setToken(session.token)
  return session
}

export function devLogin(openId: string, nickname: string): Promise<AuthToken> {
  return request<AuthToken>({
    url: '/auth/dev-login',
    method: 'POST',
    data: { openId, nickname },
  }).then(saveSession)
}

export function wechatLogin(code: string): Promise<AuthToken> {
  return request<AuthToken>({
    url: '/auth/wechat-login',
    method: 'POST',
    data: { code },
  }).then(saveSession)
}

export function getMe(): Promise<User> {
  return request<User>({ url: '/me' })
}
