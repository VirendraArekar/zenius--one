import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

export type ThemeMode = 'light' | 'dark'

interface UiState {
  theme: ThemeMode
  sidebarCollapsed: boolean
  commandPaletteOpen: boolean
  appSwitcherOpen: boolean
  notificationDrawerOpen: boolean
}

function resolveInitialTheme(): ThemeMode {
  if (typeof window === 'undefined') return 'light'
  const saved = localStorage.getItem('zenius_theme')
  if (saved === 'dark' || saved === 'light') return saved
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

const initialState: UiState = {
  theme: resolveInitialTheme(),
  sidebarCollapsed: false,
  commandPaletteOpen: false,
  appSwitcherOpen: false,
  notificationDrawerOpen: false,
}

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setTheme(state, action: PayloadAction<ThemeMode>) {
      state.theme = action.payload
      if (typeof window !== 'undefined') {
        localStorage.setItem('zenius_theme', action.payload)
      }
    },
    toggleTheme(state) {
      const next = state.theme === 'dark' ? 'light' : 'dark'
      state.theme = next
      if (typeof window !== 'undefined') {
        localStorage.setItem('zenius_theme', next)
      }
    },
    toggleSidebar(state) {
      state.sidebarCollapsed = !state.sidebarCollapsed
    },
    setCommandPaletteOpen(state, action: PayloadAction<boolean>) {
      state.commandPaletteOpen = action.payload
    },
    setAppSwitcherOpen(state, action: PayloadAction<boolean>) {
      state.appSwitcherOpen = action.payload
    },
    setNotificationDrawerOpen(state, action: PayloadAction<boolean>) {
      state.notificationDrawerOpen = action.payload
    },
  },
})

export const {
  setTheme,
  toggleTheme,
  toggleSidebar,
  setCommandPaletteOpen,
  setAppSwitcherOpen,
  setNotificationDrawerOpen,
} = uiSlice.actions

export default uiSlice.reducer

