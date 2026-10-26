import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { AdminProfile } from '@/types/admin'

export const ADMIN_TOKEN_KEY = 'mallmini_admin_token'
export const ADMIN_PROFILE_KEY = 'mallmini_admin_profile'

function readProfile(): AdminProfile | null {
  const raw = localStorage.getItem(ADMIN_PROFILE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AdminProfile
  } catch {
    localStorage.removeItem(ADMIN_PROFILE_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(ADMIN_TOKEN_KEY) ?? '')
  const admin = ref<AdminProfile | null>(readProfile())
  const isAuthenticated = computed(() => token.value.length > 0)

  function setSession(value: string, profile: AdminProfile) {
    token.value = value
    admin.value = profile
    localStorage.setItem(ADMIN_TOKEN_KEY, value)
    localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(profile))
  }

  function setAdmin(profile: AdminProfile) {
    admin.value = profile
    localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(profile))
  }

  function clearToken() {
    token.value = ''
    admin.value = null
    localStorage.removeItem(ADMIN_TOKEN_KEY)
    localStorage.removeItem(ADMIN_PROFILE_KEY)
  }

  return { token, admin, isAuthenticated, setSession, setAdmin, clearToken }
})
