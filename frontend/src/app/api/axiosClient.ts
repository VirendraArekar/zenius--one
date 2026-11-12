import axios, { type InternalAxiosRequestConfig } from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

axiosClient.interceptors.request.use((config) => {
  if (config.url && config.url.startsWith('/api/v1/')) {
    config.url = config.url.replace(/^\/api\/v1/, '')
  }
  const token = localStorage.getItem('zenius_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const activeOrgId = localStorage.getItem('zenius_active_org_id')
  if (activeOrgId) {
    config.headers['X-Organization-Id'] = activeOrgId
  }
  return config
})

let refreshPromise: Promise<string | null> | null = null

async function attemptTokenRefresh(): Promise<string | null> {
  const refreshToken = localStorage.getItem('zenius_refresh_token')
  if (!refreshToken) return null
  try {
    const res = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
    const session = res.data?.data
    if (session?.accessToken && session?.refreshToken) {
      localStorage.setItem('zenius_access_token', session.accessToken)
      localStorage.setItem('zenius_refresh_token', session.refreshToken)
      if (session.user) {
        localStorage.setItem('zenius_auth_user', JSON.stringify(session.user))
        if (session.user.activeOrganizationId) {
          localStorage.setItem('zenius_active_org_id', session.user.activeOrganizationId)
        }
      }
      return session.accessToken as string
    }
    return null
  } catch {
    localStorage.removeItem('zenius_access_token')
    localStorage.removeItem('zenius_refresh_token')
    localStorage.removeItem('zenius_auth_user')
    localStorage.removeItem('zenius_active_org_id')
    return null
  }
}

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    if (error.response?.status === 401 && original && !original._retry && !original.url?.includes('/auth/')) {
      original._retry = true
      if (!refreshPromise) {
        refreshPromise = attemptTokenRefresh().finally(() => {
          refreshPromise = null
        })
      }
      const nextToken = await refreshPromise
      if (nextToken) {
        original.headers.Authorization = `Bearer ${nextToken}`
        return axiosClient.request(original)
      }
    }
    return Promise.reject(error)
  }
)
