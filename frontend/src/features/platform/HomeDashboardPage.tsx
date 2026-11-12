import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowRight,
  Building2,
  Clock,
  Database,
  Headphones,
  Landmark,
  Layers,
  Mail,
  PieChart as PieChartIcon,
  BarChart3,
  Server,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  UserCheck,
} from 'lucide-react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useGetExecutivePulseQuery } from '@/app/api/intelligenceApi'
import { useGetModulesQuery, useGetSystemStatusQuery } from '@/app/api/systemApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { ModuleIcon } from '@/components/layout/ModuleIcon'
import { VIBRANT_PALETTE, chartTooltipStyle } from '@/components/ui/chartThemes'

export function HomeDashboardPage() {
  const authState = useAppSelector((s) => s.auth)
  const accessToken = authState?.accessToken
  const { data: status, isError: statusError, refetch: refetchStatus } = useGetSystemStatusQuery()
  const { data: modulesRes } = useGetModulesQuery({ page: 0, size: 50 })
  const { data: pulse } = useGetExecutivePulseQuery(undefined, { skip: !accessToken })
  const [activeChartTab, setActiveChartTab] = useState<'volume' | 'distribution'>('volume')

  const modules = modulesRes?.data ?? []

  const phaseDistribution = modules.reduce<Record<number, number>>((acc, mod) => {
    acc[mod.phaseNumber] = (acc[mod.phaseNumber] ?? 0) + 1
    return acc
  }, {})

  const phaseChartData =
    pulse?.moduleBreakdown && pulse.moduleBreakdown.length > 0
      ? pulse.moduleBreakdown.map((r) => ({ phase: r.label, modules: r.count }))
      : Object.entries(phaseDistribution)
          .sort(([a], [b]) => Number(a) - Number(b))
          .map(([phase, count]) => ({
            phase: `Suite ${phase}`,
            modules: count,
          }))

  const pulseDistributionData = pulse
    ? [
        { name: 'Deals ($k)', value: Math.max(1, Math.round(Number(pulse.pipelineValue) / 1000)), color: '#6366f1' },
        { name: 'Invoiced ($k)', value: Math.max(1, Math.round(Number(pulse.totalInvoiced) / 1000)), color: '#10b981' },
        { name: 'Receivable ($k)', value: Math.max(1, Math.round(Number(pulse.totalReceivable) / 1000)), color: '#06b6d4' },
        { name: 'Workforce', value: Math.max(1, pulse.employeeHeadcount), color: '#8b5cf6' },
        { name: 'Support SLA', value: Math.max(1, pulse.openTickets), color: '#f59e0b' },
      ]
    : []

  return (
    <div className="space-y-6 animate-enter-slide">
      {/* Executive Banner with glowing animated status */}
      <div className="relative overflow-hidden rounded-2xl border border-indigo-500/20 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-6 text-white shadow-xl sm:p-8">
        <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-indigo-500/10 blur-3xl animate-pulse-glow" />
        <div className="pointer-events-none absolute -left-20 -bottom-20 h-64 w-64 rounded-full bg-cyan-500/10 blur-3xl animate-pulse-glow" />

        <div className="relative z-10 flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
          <div>
            <div className="flex items-center gap-2">
              <Badge variant="success" className="border-emerald-500/30 bg-emerald-500/20 text-emerald-300 flex items-center gap-1.5 px-2.5 py-1">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
                </span>
                {status?.status ?? 'CONNECTING'}
              </Badge>
              <span className="text-xs font-medium text-indigo-300">Enterprise Cloud Operating System</span>
            </div>
            <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl bg-gradient-to-r from-white via-slate-100 to-indigo-200 bg-clip-text text-transparent">
              Executive Command Center
            </h1>
            <p className="mt-1.5 max-w-2xl text-sm text-slate-300">
              Live cross-suite revenue, pipeline, geo-attendance, support SLAs, and AI operations unified across microservices.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" className="border-slate-700 bg-slate-900/80 text-white hover:bg-slate-800 transition-all hover:scale-105" asChild>
              <Link to="/hr">
                <Clock className="mr-1.5 h-4 w-4 text-emerald-400 animate-pulse" />
                Geo Check-In / Leave
              </Link>
            </Button>
            <Button variant="outline" className="border-slate-700 bg-slate-900/80 text-white hover:bg-slate-800 transition-all hover:scale-105" asChild>
              <Link to="/mail">
                <Mail className="mr-1.5 h-4 w-4 text-indigo-400" />
                Open Mail
              </Link>
            </Button>
            <Button className="bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md hover:from-indigo-500 hover:to-violet-500 transition-all hover:scale-105" asChild>
              <Link to="/ai">
                <Sparkles className="mr-1.5 h-4 w-4" />
                Zenius AI
              </Link>
            </Button>
          </div>
        </div>
      </div>

      {statusError && (
        <Card className="border-destructive/50 bg-destructive/5">
          <CardContent className="flex items-center justify-between p-4">
            <p className="text-sm text-destructive">
              Unable to reach API Gateway at <code>/api/v1/system/status</code>. Ensure backend microservices are running.
            </p>
            <Button size="sm" variant="outline" onClick={() => refetchStatus()}>
              Retry
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Live Business Pulse Row (shown when authenticated with pulse telemetry) */}
      {pulse ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Link to="/sales" className="group block">
            <Card className="h-full border-slate-200/80 shadow-xs card-motion">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Sales Pipeline</CardTitle>
                <div className="rounded-lg bg-indigo-50 p-2 text-indigo-600 transition-transform group-hover:scale-110 dark:bg-indigo-950 dark:text-indigo-400">
                  <TrendingUp className="h-4 w-4" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">${Number(pulse.pipelineValue).toLocaleString()}</div>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {pulse.dealCount} deals · {pulse.leadCount} leads in funnel
                </p>
              </CardContent>
            </Card>
          </Link>

          <Link to="/finance" className="group block">
            <Card className="h-full border-slate-200/80 shadow-xs card-motion">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Invoiced Revenue</CardTitle>
                <div className="rounded-lg bg-emerald-50 p-2 text-emerald-600 transition-transform group-hover:scale-110 dark:bg-emerald-950 dark:text-emerald-400">
                  <Landmark className="h-4 w-4" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">${Number(pulse.totalInvoiced).toLocaleString()}</div>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  ${Number(pulse.totalReceivable).toLocaleString()} receivable open
                </p>
              </CardContent>
            </Card>
          </Link>

          <Link to="/hr" className="group block">
            <Card className="h-full border-slate-200/80 shadow-xs card-motion">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Active Workforce</CardTitle>
                <div className="rounded-lg bg-blue-50 p-2 text-blue-600 transition-transform group-hover:scale-110 dark:bg-blue-950 dark:text-blue-400">
                  <UserCheck className="h-4 w-4" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">{pulse.employeeHeadcount}</div>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {pulse.completedTasks}/{pulse.totalTasks} sprint tasks done
                </p>
              </CardContent>
            </Card>
          </Link>

          <Link to="/support" className="group block">
            <Card className="h-full border-slate-200/80 shadow-xs card-motion">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Open Support Queue</CardTitle>
                <div className="rounded-lg bg-amber-50 p-2 text-amber-600 transition-transform group-hover:scale-110 dark:bg-amber-950 dark:text-amber-400">
                  <Headphones className="h-4 w-4" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">{pulse.openTickets}</div>
                <p className="mt-0.5 text-xs text-muted-foreground">Active customer SLA tickets</p>
              </CardContent>
            </Card>
          </Link>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Card className="border-slate-200/80 shadow-xs card-motion">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">Active Applications</CardTitle>
              <Layers className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{status?.totalModules ?? '—'}</div>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {status ? `${status.activeModules} live modules across suite` : 'Awaiting gateway'}
              </p>
            </CardContent>
          </Card>

          <Card className="border-slate-200/80 shadow-xs card-motion">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">Service Architecture</CardTitle>
              <Server className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">Microservices</div>
              <p className="mt-0.5 text-xs text-muted-foreground">{status?.version ?? 'Production GA'}</p>
            </CardContent>
          </Card>

          <Card className="border-slate-200/80 shadow-xs card-motion">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">Database Engine</CardTitle>
              <Database className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="truncate text-lg font-bold">{status?.databaseProduct ?? '—'}</div>
              <p className="mt-0.5 text-xs text-muted-foreground">Multi-tenant schema isolated</p>
            </CardContent>
          </Card>

          <Card className="border-slate-200/80 shadow-xs card-motion">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">Security Posture</CardTitle>
              <ShieldCheck className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-lg font-bold">RBAC Enforced</div>
              <p className="mt-0.5 text-xs text-muted-foreground">JWT & TOTP MFA Guard active</p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Business Applications Grid */}
      <Card className="border-slate-200/80 shadow-xs">
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Enterprise Applications</CardTitle>
            <CardDescription>Click any application workspace to manage live records and workflows</CardDescription>
          </div>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/analytics">
              <span>View Executive BI</span>
              <ArrowRight className="ml-1 h-3.5 w-3.5" />
            </Link>
          </Button>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
            {modules.map((mod) => (
              <Link
                key={mod.id}
                to={mod.routePath}
                className="group flex flex-col justify-between rounded-xl border border-slate-200/80 bg-white p-4 transition-all duration-200 hover:-translate-y-1 hover:border-indigo-400 hover:shadow-lg dark:border-border dark:bg-card"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 transition-all group-hover:scale-110 group-hover:bg-indigo-600 group-hover:text-white dark:bg-indigo-950/60 dark:text-indigo-400">
                    <ModuleIcon name={mod.iconName} className="h-5 w-5" />
                  </div>
                  <Badge variant="outline" className="text-[10px]">
                    {mod.category}
                  </Badge>
                </div>
                <div className="mt-3">
                  <p className="font-semibold text-foreground group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                    {mod.name}
                  </p>
                  <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">{mod.tagline}</p>
                </div>
              </Link>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Live Visualizations & System Runtime */}
      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="border-slate-200/80 shadow-xs card-motion">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <div>
              <CardTitle>{pulse ? 'Live Tenant Operations Volume' : 'Application Portfolio Coverage'}</CardTitle>
              <CardDescription>
                {pulse
                  ? 'Real-time record density across CRM, Sales, Finance, Support, People & Projects'
                  : 'Registered microservices and business domains in this tenant'}
              </CardDescription>
            </div>
            {pulse && (
              <div className="flex items-center gap-1 rounded-lg border bg-muted/30 p-1 text-xs">
                <button
                  type="button"
                  onClick={() => setActiveChartTab('volume')}
                  className={`flex items-center gap-1 px-2.5 py-1 rounded-md transition-all ${
                    activeChartTab === 'volume'
                      ? 'bg-primary text-primary-foreground shadow-xs font-medium'
                      : 'text-muted-foreground hover:text-foreground'
                  }`}
                >
                  <BarChart3 className="h-3.5 w-3.5" />
                  <span>Bars</span>
                </button>
                <button
                  type="button"
                  onClick={() => setActiveChartTab('distribution')}
                  className={`flex items-center gap-1 px-2.5 py-1 rounded-md transition-all ${
                    activeChartTab === 'distribution'
                      ? 'bg-primary text-primary-foreground shadow-xs font-medium'
                      : 'text-muted-foreground hover:text-foreground'
                  }`}
                >
                  <PieChartIcon className="h-3.5 w-3.5" />
                  <span>Donut</span>
                </button>
              </div>
            )}
          </CardHeader>
          <CardContent className="h-64">
            {activeChartTab === 'distribution' && pulse ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Tooltip contentStyle={chartTooltipStyle} />
                  <Pie
                    data={pulseDistributionData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={85}
                    paddingAngle={4}
                    dataKey="value"
                    animationDuration={800}
                  >
                    {pulseDistributionData.map((entry, idx) => (
                      <Cell key={`donut-${entry.name}`} fill={entry.color || VIBRANT_PALETTE[idx % VIBRANT_PALETTE.length]} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            ) : phaseChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={phaseChartData}>
                  <defs>
                    {VIBRANT_PALETTE.map((color, idx) => (
                      <linearGradient key={`grad-${idx}`} id={`barGrad-${idx}`} x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor={color} stopOpacity={1} />
                        <stop offset="100%" stopColor={color} stopOpacity={0.65} />
                      </linearGradient>
                    ))}
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.2} className="stroke-border" />
                  <XAxis dataKey="phase" fontSize={11} stroke="currentColor" opacity={0.6} />
                  <YAxis allowDecimals={false} fontSize={12} stroke="currentColor" opacity={0.6} />
                  <Tooltip contentStyle={chartTooltipStyle} cursor={{ fill: 'rgba(99, 102, 241, 0.08)' }} />
                  <Bar dataKey="modules" radius={[6, 6, 0, 0]} animationDuration={800}>
                    {phaseChartData.map((_entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={`url(#barGrad-${index % VIBRANT_PALETTE.length})`}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                Waiting for platform telemetry...
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="border-slate-200/80 shadow-xs card-motion">
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle>Platform Settings & Governance</CardTitle>
              <CardDescription>
                {status?.databaseProduct ? `Engine: ${status.databaseProduct}` : 'Runtime configuration'}
              </CardDescription>
            </div>
            <Button variant="outline" size="sm" asChild>
              <Link to="/admin">
                <Building2 className="mr-1.5 h-3.5 w-3.5" /> Org Admin
              </Link>
            </Button>
          </CardHeader>
          <CardContent className="space-y-2.5">
            {status?.databaseProduct && (
              <div className="flex items-center justify-between rounded-xl border bg-slate-50/60 px-3.5 py-2 text-xs dark:bg-muted/30">
                <span className="font-medium text-muted-foreground">Database Engine</span>
                <span className="font-semibold">{status.databaseProduct}</span>
              </div>
            )}
            {status?.publicSettings.map((setting) => (
              <div
                key={setting.key}
                className="flex items-center justify-between rounded-xl border bg-slate-50/60 px-3.5 py-2.5 text-sm dark:bg-muted/30 transition-colors hover:bg-slate-100/70 dark:hover:bg-muted/50"
              >
                <div>
                  <p className="font-mono text-xs font-medium">{setting.key}</p>
                  <p className="text-xs text-muted-foreground">{setting.description}</p>
                </div>
                <Badge variant="outline">{setting.value}</Badge>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
