import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  Bell,
  Building2,
  CheckCheck,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Command,
  Grid,
  LogOut,
  Menu,
  Moon,
  Radio,
  Search,
  Sparkles,
  Sun,
  UserCircle2,
  X,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import {
  setAppSwitcherOpen,
  setCommandPaletteOpen,
  setNotificationDrawerOpen,
  toggleSidebar,
  toggleTheme,
} from '@/app/slices/uiSlice'
import { clearSession, setSession } from '@/app/slices/authSlice'
import { API_BASE_URL } from '@/app/api/axiosClient'
import { useGetModulesQuery, useGetSystemStatusQuery } from '@/app/api/systemApi'
import {
  adminApi,
  useGetNotificationsQuery,
  useLogoutMutation,
  useMarkAllNotificationsReadMutation,
  useMarkNotificationReadMutation,
  useSwitchOrganizationMutation,
} from '@/app/api/adminApi'
import { crmApi } from '@/app/api/crmApi'
import { erpApi } from '@/app/api/erpApi'
import { growthAndSupportApi } from '@/app/api/growthAndSupportApi'
import { peopleApi } from '@/app/api/peopleApi'
import { workplaceApi } from '@/app/api/workplaceApi'
import { intelligenceApi } from '@/app/api/intelligenceApi'
import { aiApi } from '@/app/api/aiApi'
import { CommandPalette } from './CommandPalette'
import { ModuleIcon } from './ModuleIcon'

interface LiveToastEvent {
  title: string
  message: string
  category?: string
  linkPath?: string
}

const CATEGORY_LABELS: Record<string, string> = {
  PLATFORM: 'Platform & Admin',
  REVENUE: 'Growth & Revenue',
  CUSTOMER: 'Customer Success',
  FINANCE: 'Finance & Billing',
  OPERATIONS: 'Supply & Delivery',
  PEOPLE: 'People & Payroll',
  WORKPLACE: 'Workplace & Docs',
  INTELLIGENCE: 'BI, Creator & AI',
}

export function AppShell() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const { theme, sidebarCollapsed, appSwitcherOpen, notificationDrawerOpen } = useAppSelector(
    (state) => state.ui
  )
  const { user, accessToken, refreshToken } = useAppSelector((state) => state.auth)
  const [orgMenuOpen, setOrgMenuOpen] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [streamConnected, setStreamConnected] = useState(false)
  const [liveToast, setLiveToast] = useState<LiveToastEvent | null>(null)

  const { data: modulesRes, isLoading: modulesLoading, isError: modulesError } = useGetModulesQuery({
    page: 0,
    size: 50,
  })
  const { data: systemStatus } = useGetSystemStatusQuery()
  const { data: notifRes, refetch: refetchNotifications } = useGetNotificationsQuery(undefined, {
    skip: !accessToken,
  })
  const [switchOrg] = useSwitchOrganizationMutation()
  const [markRead] = useMarkNotificationReadMutation()
  const [markAllRead] = useMarkAllNotificationsReadMutation()
  const [logoutApi] = useLogoutMutation()

  const modules = modulesRes?.data ?? []
  const activeOrg = user?.organizations.find((o) => o.id === user.activeOrganizationId)
  const unreadCount = notifRes?.data?.unreadCount ?? 0
  const notifications = notifRes?.data?.items ?? []

  useEffect(() => {
    const root = document.documentElement
    if (theme === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
  }, [theme])

  useEffect(() => {
    setMobileMenuOpen(false)
  }, [location.pathname])

  // Real-time Server-Sent Events subscription over Notification Event Bus
  useEffect(() => {
    if (!accessToken || typeof window === 'undefined' || typeof window.fetch !== 'function') {
      setStreamConnected(false)
      return
    }
    const controller = new AbortController()
    let active = true

    const connectStream = async () => {
      try {
        const headers: Record<string, string> = {
          Accept: 'text/event-stream',
          Authorization: `Bearer ${accessToken}`,
        }
        if (user?.activeOrganizationId) {
          headers['X-Organization-Id'] = user.activeOrganizationId
        }
        const response = await fetch(`${API_BASE_URL}/notifications/stream`, {
          headers,
          signal: controller.signal,
        })
        if (!response.ok || !response.body) {
          return
        }
        if (active) setStreamConnected(true)

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (active) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const frames = buffer.split('\n\n')
          buffer = frames.pop() ?? ''

          for (const frame of frames) {
            if (!frame.includes('event:notification') && !frame.includes('event: notification')) {
              continue
            }
            const dataLine = frame
              .split('\n')
              .find((l) => l.startsWith('data:'))
            if (dataLine) {
              try {
                const payload = JSON.parse(dataLine.slice(5).trim()) as LiveToastEvent
                setLiveToast(payload)
                void refetchNotifications()
              } catch {
                void refetchNotifications()
              }
            }
          }
        }
      } catch {
        if (active) setStreamConnected(false)
      }
    }

    void connectStream()
    return () => {
      active = false
      controller.abort()
    }
  }, [accessToken, user?.activeOrganizationId, refetchNotifications])

  useEffect(() => {
    if (!liveToast) return
    const t = setTimeout(() => setLiveToast(null), 6000)
    return () => clearTimeout(t)
  }, [liveToast])

  const handleOrgSwitch = async (orgId: string) => {
    setOrgMenuOpen(false)
    if (orgId === user?.activeOrganizationId) return
    try {
      const session = await switchOrg(orgId).unwrap()
      dispatch(
        setSession({
          accessToken: session.accessToken,
          refreshToken: session.refreshToken,
          user: session.user,
        })
      )
      // Reset all RTK Query API caches across modules so every query re-fetches with new tenant context
      dispatch(adminApi.util.resetApiState())
      dispatch(crmApi.util.resetApiState())
      dispatch(erpApi.util.resetApiState())
      dispatch(growthAndSupportApi.util.resetApiState())
      dispatch(peopleApi.util.resetApiState())
      dispatch(workplaceApi.util.resetApiState())
      dispatch(intelligenceApi.util.resetApiState())
      dispatch(aiApi.util.resetApiState())

      const switchedOrg = session.user.organizations.find((o) => o.id === orgId)
      setLiveToast({
        title: 'Active Organization Switched',
        message: `Tenant updated to ${switchedOrg?.name || 'Organization'} (${switchedOrg?.baseCurrency || 'USD'}). All records reloaded.`,
      })
    } catch (err) {
      console.error('Failed to switch organization:', err)
    }
  }

  const handleSignOut = async () => {
    try {
      await logoutApi({ refreshToken }).unwrap()
    } catch {
      // ignore network error during sign-out cleanup
    }
    dispatch(clearSession())
    navigate('/login')
  }

  const groupedCategories = Array.from(new Set(modules.map((m) => m.category)))

  const renderSidebarLinks = (collapsed: boolean, onNavigate?: () => void) => (
    <div className="flex-1 space-y-4 overflow-y-auto px-2.5 py-4">
      {modulesLoading && !collapsed && (
        <p className="px-2.5 py-2 text-xs text-slate-400">Loading workspace...</p>
      )}
      {modulesError && !collapsed && (
        <p className="px-2.5 py-2 text-xs text-rose-400">Service gateway unavailable</p>
      )}

      {groupedCategories.map((category) => {
        const items = modules.filter((m) => m.category === category)
        return (
          <div key={category} className="space-y-1">
            {!collapsed && (
              <p className="px-2.5 pb-1 text-[10px] font-semibold uppercase tracking-widest text-slate-500">
                {CATEGORY_LABELS[category] ?? category}
              </p>
            )}
            {items.map((mod) => (
              <NavLink
                key={mod.id}
                to={mod.routePath}
                title={mod.name}
                onClick={onNavigate}
                className={({ isActive }) =>
                  cn(
                    'group flex items-center justify-between rounded-xl px-2.5 py-2 text-[13px] font-medium transition-all duration-150',
                    isActive
                      ? 'bg-gradient-to-r from-indigo-600 to-blue-600 text-white shadow-md shadow-indigo-950/50'
                      : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                  )
                }
              >
                <span className="flex items-center gap-2.5 truncate">
                  <ModuleIcon name={mod.iconName} className="h-4 w-4 shrink-0 opacity-90" />
                  {!collapsed && <span className="truncate">{mod.name}</span>}
                </span>
              </NavLink>
            ))}
          </div>
        )
      })}
    </div>
  )

  return (
    <div className="flex min-h-screen bg-slate-50/70 text-foreground dark:bg-background">
      <CommandPalette />

      {/* Mobile Slide-Over Navigation Drawer */}
      {mobileMenuOpen && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          <div
            className="fixed inset-0 bg-slate-950/70 backdrop-blur-xs transition-opacity"
            onClick={() => setMobileMenuOpen(false)}
          />
          <aside className="relative z-10 flex h-full w-72 max-w-[85vw] flex-col border-r border-slate-800 bg-slate-950 text-slate-200 shadow-2xl">
            <div className="flex h-14 items-center justify-between border-b border-slate-800 px-4">
              <Link to="/" onClick={() => setMobileMenuOpen(false)} className="flex items-center gap-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-blue-600 font-bold text-white shadow-md">
                  Z
                </div>
                <div>
                  <p className="text-sm font-bold leading-tight text-white">Zenius One</p>
                  <p className="text-[10px] text-slate-400">Enterprise Cloud OS</p>
                </div>
              </Link>
              <Button
                variant="ghost"
                size="icon"
                className="text-slate-300 hover:bg-slate-800 hover:text-white"
                aria-label="Close navigation"
                onClick={() => setMobileMenuOpen(false)}
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
            {renderSidebarLinks(false, () => setMobileMenuOpen(false))}
          </aside>
        </div>
      )}

      {/* Premium Obsidian Desktop Sidebar */}
      <aside
        className={cn(
          'sticky top-0 hidden h-screen flex-col border-r border-slate-800/80 bg-slate-950 text-slate-200 transition-all duration-200 md:flex',
          sidebarCollapsed ? 'w-16' : 'w-64'
        )}
      >
        <div className="flex h-14 items-center justify-between border-b border-slate-800/80 px-3">
          <Link to="/" className="flex items-center gap-2.5 overflow-hidden">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-blue-600 font-bold text-white shadow-md shadow-indigo-900/40">
              Z
            </div>
            {!sidebarCollapsed && (
              <div className="truncate">
                <p className="truncate text-sm font-bold tracking-tight text-white">Zenius One</p>
                <p className="truncate text-[10px] font-medium text-indigo-300">Enterprise Suite</p>
              </div>
            )}
          </Link>
          <Button
            variant="ghost"
            size="icon"
            className="text-slate-400 hover:bg-slate-800 hover:text-white"
            aria-label="Toggle sidebar"
            onClick={() => dispatch(toggleSidebar())}
          >
            {sidebarCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </Button>
        </div>

        {renderSidebarLinks(sidebarCollapsed)}

        {!sidebarCollapsed && (
          <div className="border-t border-slate-800/80 p-3">
            <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-2.5 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-white truncate">{activeOrg?.name ?? 'Enterprise'}</span>
                <span className="inline-flex h-2 w-2 shrink-0 rounded-full bg-emerald-400" />
              </div>
              <p className="mt-1 text-[11px] text-slate-400">
                {systemStatus?.status === 'OPERATIONAL' ? 'All microservices live' : 'Connected'}
              </p>
            </div>
          </div>
        )}
      </aside>

      {/* Main Content Wrapper */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-2 border-b border-slate-200/80 bg-white/85 px-3 backdrop-blur-md dark:border-border dark:bg-card/85 sm:px-6">
          <div className="flex items-center gap-2 sm:gap-3">
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              aria-label="Open navigation"
              onClick={() => setMobileMenuOpen(true)}
            >
              <Menu className="h-5 w-5" />
            </Button>

            <div className="relative">
              <Button
                variant="outline"
                size="icon"
                aria-label="App Switcher"
                onClick={() => dispatch(setAppSwitcherOpen(!appSwitcherOpen))}
              >
                <Grid className="h-4 w-4" />
              </Button>
              {appSwitcherOpen && (
                <div className="animate-fadeSlideIn absolute left-0 top-11 z-40 w-[calc(100vw-2rem)] max-w-sm rounded-2xl border bg-card p-3 shadow-2xl sm:w-80">
                  <div className="mb-2 flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Zenius One Suite
                    </span>
                    <Link
                      to="/"
                      onClick={() => dispatch(setAppSwitcherOpen(false))}
                      className="text-xs text-primary hover:underline"
                    >
                      Dashboard
                    </Link>
                  </div>
                  <div className="grid max-h-72 grid-cols-3 gap-1.5 overflow-y-auto">
                    {modules.map((mod) => (
                      <Link
                        key={mod.id}
                        to={mod.routePath}
                        onClick={() => dispatch(setAppSwitcherOpen(false))}
                        className="flex flex-col items-center gap-1 rounded-xl p-2 text-center text-[11px] transition-colors hover:bg-accent"
                      >
                        <ModuleIcon name={mod.iconName} className="h-4 w-4 text-primary" />
                        <span className="w-full truncate">{mod.name}</span>
                      </Link>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={() => dispatch(setCommandPaletteOpen(true))}
              className="flex h-9 w-44 items-center justify-between rounded-xl border border-slate-200 bg-slate-50/80 px-3 text-xs text-muted-foreground transition-colors hover:border-primary/40 hover:bg-white sm:w-64 lg:w-80 dark:border-border dark:bg-background"
            >
              <span className="flex items-center gap-2 truncate">
                <Search className="h-3.5 w-3.5 shrink-0" />
                <span className="truncate">Search apps, actions or records...</span>
              </span>
              <span className="hidden items-center gap-0.5 rounded border bg-white px-1.5 py-0.5 text-[10px] sm:inline-flex dark:bg-muted">
                <Command className="h-2.5 w-2.5" />K
              </span>
            </button>
          </div>

          <div className="flex items-center gap-1.5 sm:gap-2">
            {/* Top Organization Selection Tabs (Desktop / Tablet) */}
            {user && user.organizations.length > 1 && (
              <div
                role="tablist"
                aria-label="Active Organization Switcher"
                className="hidden lg:flex items-center gap-1 rounded-xl border border-slate-200 bg-slate-100/90 p-1 dark:border-border dark:bg-muted/60"
              >
                {user.organizations.map((org) => {
                  const isActive = org.id === user.activeOrganizationId
                  return (
                    <button
                      key={org.id}
                      role="tab"
                      aria-selected={isActive}
                      type="button"
                      onClick={() => !isActive && handleOrgSwitch(org.id)}
                      className={cn(
                        'flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs transition-all duration-150',
                        isActive
                          ? 'bg-white font-semibold text-primary shadow-xs ring-1 ring-black/5 dark:bg-card dark:text-primary dark:ring-white/10'
                          : 'text-muted-foreground hover:bg-white/60 hover:text-foreground dark:hover:bg-card/40'
                      )}
                    >
                      <Building2 className={cn('h-3.5 w-3.5', isActive ? 'text-primary' : 'text-muted-foreground')} />
                      <span className="truncate max-w-[130px]">{org.name}</span>
                      <span
                        className={cn(
                          'rounded px-1 text-[9px] font-mono font-medium',
                          isActive
                            ? 'bg-primary/10 text-primary dark:bg-primary/20'
                            : 'bg-slate-200/70 text-muted-foreground dark:bg-slate-800'
                        )}
                      >
                        {org.baseCurrency}
                      </span>
                    </button>
                  )
                })}
              </div>
            )}

            {user && (
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setOrgMenuOpen(!orgMenuOpen)}
                  className="flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs shadow-xs transition-colors hover:bg-slate-50 dark:border-border dark:bg-background"
                >
                  <Building2 className="h-3.5 w-3.5 text-primary" />
                  <span className="max-w-36 truncate font-medium">
                    {activeOrg?.name ?? 'Select Org'}
                  </span>
                  <ChevronDown className="h-3 w-3 text-muted-foreground" />
                </button>
                {orgMenuOpen && (
                  <div className="animate-fadeSlideIn absolute right-0 top-10 z-40 w-64 rounded-2xl border bg-card p-2 shadow-2xl">
                    <p className="px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                      Switch Active Organization
                    </p>
                    {user.organizations.map((org) => {
                      const isSelected = org.id === user.activeOrganizationId
                      return (
                        <button
                          key={org.id}
                          type="button"
                          onClick={() => handleOrgSwitch(org.id)}
                          className={cn(
                            'flex w-full items-center justify-between rounded-lg px-2.5 py-2 text-left text-xs transition-colors hover:bg-accent',
                            isSelected && 'bg-primary/10 font-semibold text-primary'
                          )}
                        >
                          <div className="flex items-center gap-2 truncate">
                            <Building2 className={cn('h-3.5 w-3.5', isSelected ? 'text-primary' : 'text-muted-foreground')} />
                            <span className="truncate">{org.name}</span>
                          </div>
                          <span className={cn('rounded px-1.5 py-0.5 text-[9px] font-mono', isSelected ? 'bg-primary text-white' : 'bg-slate-100 text-muted-foreground dark:bg-muted')}>
                            {org.baseCurrency}
                          </span>
                        </button>
                      )
                    })}
                    <div className="my-1 border-t" />
                    <Link
                      to="/admin"
                      onClick={() => setOrgMenuOpen(false)}
                      className="block rounded-lg px-2.5 py-1.5 text-xs text-primary hover:bg-accent"
                    >
                      Manage Organizations →
                    </Link>
                  </div>
                )}
              </div>
            )}

            <Button variant="outline" size="sm" asChild className="hidden md:inline-flex">
              <Link to="/ai">
                <Sparkles className="h-3.5 w-3.5 text-primary" />
                <span>Zenius AI</span>
              </Link>
            </Button>

            <Button
              variant="ghost"
              size="icon"
              aria-label="Toggle theme"
              onClick={() => dispatch(toggleTheme())}
            >
              {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </Button>

            <div className="relative">
              <Button
                variant="ghost"
                size="icon"
                aria-label="Notifications"
                onClick={() => dispatch(setNotificationDrawerOpen(!notificationDrawerOpen))}
              >
                <Bell className="h-4 w-4" />
                {unreadCount > 0 && (
                  <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-indigo-600 px-1 text-[10px] font-bold text-white">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </Button>
              {notificationDrawerOpen && (
                <div className="animate-fadeSlideIn absolute right-0 top-11 z-40 w-[calc(100vw-2rem)] max-w-sm rounded-2xl border bg-card p-4 shadow-2xl">
                  <div className="flex items-center justify-between border-b pb-2.5">
                    <div>
                      <h4 className="text-sm font-semibold">
                        Notifications {unreadCount > 0 ? `(${unreadCount})` : ''}
                      </h4>
                      <p className="flex items-center gap-1 text-[10px] text-muted-foreground">
                        <Radio className={cn('h-3 w-3', streamConnected ? 'text-emerald-500' : 'text-amber-500')} />
                        {streamConnected ? 'Real-Time SSE Event Stream active' : 'Event feed synced'}
                      </p>
                    </div>
                    {accessToken && unreadCount > 0 && (
                      <button
                        type="button"
                        onClick={() => markAllRead()}
                        className="flex items-center gap-1 text-xs text-primary hover:underline"
                      >
                        <CheckCheck className="h-3.5 w-3.5" />
                        <span>Mark all read</span>
                      </button>
                    )}
                  </div>
                  {notifications.length === 0 ? (
                    <p className="mt-3 text-xs text-muted-foreground">No notifications yet.</p>
                  ) : (
                    <div className="mt-2 max-h-80 space-y-2 overflow-y-auto">
                      {notifications.map((n) => (
                        <div
                          key={n.id}
                          onClick={() => {
                            if (!n.read) void markRead(n.id)
                            if (n.linkPath) {
                              dispatch(setNotificationDrawerOpen(false))
                              navigate(n.linkPath)
                            }
                          }}
                          className={cn(
                            'cursor-pointer rounded-xl border p-2.5 text-xs transition-colors hover:bg-muted/40',
                            !n.read && 'border-indigo-500/40 bg-indigo-50/50 dark:bg-indigo-950/20'
                          )}
                        >
                          <div className="flex items-center justify-between gap-2">
                            <p className="font-semibold">{n.title}</p>
                            <Badge variant="outline" className="text-[9px]">
                              {n.category}
                            </Badge>
                          </div>
                          <p className="mt-0.5 text-muted-foreground">{n.message}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>

            {user && (
              <div className="flex items-center gap-1">
                <Link
                  to="/admin"
                  className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs shadow-xs transition-colors hover:bg-slate-50 dark:border-border dark:bg-background"
                >
                  <UserCircle2 className="h-4 w-4 text-primary" />
                  <span className="hidden max-w-28 truncate font-medium sm:inline">
                    {user.fullName}
                  </span>
                </Link>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Sign out"
                  title="Sign out"
                  onClick={handleSignOut}
                >
                  <LogOut className="h-4 w-4" />
                </Button>
              </div>
            )}
          </div>
        </header>

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <div key={location.pathname} className="animate-fadeSlideIn mx-auto max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>

      {/* Live Push Notification Toast */}
      {liveToast && (
        <div className="animate-fadeSlideIn fixed bottom-5 right-5 z-50 w-80 rounded-2xl border border-indigo-200 bg-white p-4 shadow-2xl dark:border-indigo-900 dark:bg-slate-900">
          <div className="flex items-start justify-between gap-2">
            <div>
              <Badge variant="secondary" className="mb-1 text-[10px]">
                {liveToast.category || 'LIVE EVENT'}
              </Badge>
              <p className="text-sm font-semibold">{liveToast.title}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">{liveToast.message}</p>
            </div>
            <button
              type="button"
              onClick={() => setLiveToast(null)}
              className="text-xs text-muted-foreground hover:text-foreground"
            >
              ✕
            </button>
          </div>
          {liveToast.linkPath && (
            <Button
              size="sm"
              variant="outline"
              className="mt-3 w-full text-xs"
              onClick={() => {
                const target = liveToast.linkPath || '/'
                setLiveToast(null)
                navigate(target)
              }}
            >
              Open Workspace →
            </Button>
          )}
        </div>
      )}
    </div>
  )
}
