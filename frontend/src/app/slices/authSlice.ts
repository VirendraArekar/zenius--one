import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

export interface OrganizationSummary {
  id: string
  slug: string
  name: string
  industry?: string | null
  baseCurrency: string
  timezone: string
}

export interface AuthUserProfile {
  id: string
  email: string
  fullName: string
  jobTitle?: string | null
  mfaEnabled: boolean
  activeOrganizationId: string | null
  permissions: string[]
  organizations: OrganizationSummary[]
}

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUserProfile | null
}

function loadInitialAuth(): AuthState {
  if (typeof window === 'undefined') {
    return { accessToken: null, refreshToken: null, user: null }
  }
  const accessToken = localStorage.getItem('zenius_access_token')
  const refreshToken = localStorage.getItem('zenius_refresh_token')
  const rawUser = localStorage.getItem('zenius_auth_user')
  let user: AuthUserProfile | null = null
  if (rawUser) {
    try {
      user = JSON.parse(rawUser) as AuthUserProfile
    } catch {
      user = null
    }
  }
  return { accessToken, refreshToken, user }
}

const initialState: AuthState = loadInitialAuth()

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setSession(
      state,
      action: PayloadAction<{
        accessToken: string
        refreshToken: string
        user: AuthUserProfile
      }>
    ) {
      state.accessToken = action.payload.accessToken
      state.refreshToken = action.payload.refreshToken
      state.user = action.payload.user
      if (typeof window !== 'undefined') {
        localStorage.setItem('zenius_access_token', action.payload.accessToken)
        localStorage.setItem('zenius_refresh_token', action.payload.refreshToken)
        localStorage.setItem('zenius_auth_user', JSON.stringify(action.payload.user))
        if (action.payload.user.activeOrganizationId) {
          localStorage.setItem('zenius_active_org_id', action.payload.user.activeOrganizationId)
        }
      }
    },
    setUserProfile(state, action: PayloadAction<AuthUserProfile>) {
      state.user = action.payload
      if (typeof window !== 'undefined') {
        localStorage.setItem('zenius_auth_user', JSON.stringify(action.payload))
        if (action.payload.activeOrganizationId) {
          localStorage.setItem('zenius_active_org_id', action.payload.activeOrganizationId)
        }
      }
    },
    clearSession(state) {
      state.accessToken = null
      state.refreshToken = null
      state.user = null
      if (typeof window !== 'undefined') {
        localStorage.removeItem('zenius_access_token')
        localStorage.removeItem('zenius_refresh_token')
        localStorage.removeItem('zenius_auth_user')
        localStorage.removeItem('zenius_active_org_id')
      }
    },
  },
})

export const { setSession, setUserProfile, clearSession } = authSlice.actions
export default authSlice.reducer

