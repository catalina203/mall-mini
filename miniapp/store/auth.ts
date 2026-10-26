import { clearToken, getToken, setToken } from '../services/request'

export function isLoggedIn(): boolean {
  return Boolean(getToken())
}

export function saveToken(token: string): void {
  setToken(token)
}

export function logout(): void {
  clearToken()
}
