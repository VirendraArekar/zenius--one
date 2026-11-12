import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BarChart3, PieChart as PieChartIcon, Play, PlusCircle, TrendingUp } from 'lucide-react'
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
import {
  type ReportExecutionDto,
  useCreateDashboardMutation,
  useCreateReportMutation,
  useGetDashboardsQuery,
  useGetExecutivePulseQuery,
  useGetReportsQuery,
  useRunReportMutation,
} from '@/app/api/intelligenceApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { VIBRANT_PALETTE, chartTooltipStyle } from '@/components/ui/chartThemes'

export function AnalyticsWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [dashName, setDashName] = useState('')
  const [reportName, setReportName] = useState('')
  const [dataSource, setDataSource] = useState('CRM_DEALS')
  const [chartType, setChartType] = useState('BAR')
  const [selectedDashId, setSelectedDashId] = useState('')
  const [reportResult, setReportResult] = useState<ReportExecutionDto | null>(null)
  const [feedback, setFeedback] = useState<string | null>(null)
  const [activePulseTab, setActivePulseTab] = useState<'bar' | 'pie'>('bar')

  const skip = !accessToken || !user
  const { data: pulse } = useGetExecutivePulseQuery(undefined, { skip })
  const { data: dashboards = [] } = useGetDashboardsQuery(undefined, { skip })
  const { data: reports = [] } = useGetReportsQuery(undefined, { skip })

  const [createDashboard] = useCreateDashboardMutation()
  const [createReport] = useCreateReportMutation()
  const [runReport] = useRunReportMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl card-motion">
        <CardHeader>
          <CardTitle>Sign In to Access Analytics & BI</CardTitle>
          <CardDescription>Cross-app executive KPIs, custom BI dashboards, report builder, and CSV exports.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const handleCreateDashboard = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!dashName.trim()) return
    try {
      const created = await createDashboard({ name: dashName.trim(), shared: true }).unwrap()
      setSelectedDashId(created.id)
      setDashName('')
      setFeedback(`Created dashboard ${created.name}`)
    } catch {
      setFeedback('Failed to create dashboard')
    }
  }

  const handleCreateReport = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!reportName.trim()) return
    try {
      const created = await createReport({
        dashboardId: selectedDashId || dashboards[0]?.id || undefined,
        name: reportName.trim(),
        dataSource,
        chartType,
      }).unwrap()
      setReportName('')
      setFeedback(`Saved report ${created.name}`)
    } catch {
      setFeedback('Failed to create report')
    }
  }

  const handleRunReport = async (reportId: string) => {
    try {
      const res = await runReport(reportId).unwrap()
      setReportResult(res)
      setFeedback(`Executed report ${res.report.name}`)
    } catch {
      setFeedback('Failed to execute report')
    }
  }

  const moduleBreakdown = pulse?.moduleBreakdown ?? []

  return (
    <div className="space-y-6 animate-enter-slide">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight bg-gradient-to-r from-foreground via-foreground/90 to-primary bg-clip-text text-transparent">
            Enterprise Analytics & BI
          </h1>
          <p className="text-sm text-muted-foreground">
            Live cross-module executive pulse, custom BI dashboards, and report datasets
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" className="transition-all hover:scale-105" asChild>
            <Link to="/creator">Creator Studio</Link>
          </Button>
          <Button variant="outline" className="transition-all hover:scale-105" asChild>
            <Link to="/automation">Automation & RPA</Link>
          </Button>
        </div>
      </div>

      {feedback && (
        <div className="flex items-center justify-between rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-xs text-primary animate-fadeSlideIn">
          <span>{feedback}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Metric KPI cards with card motion */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="card-motion">
          <CardHeader className="pb-2">
            <CardDescription>Pipeline Value</CardDescription>
            <CardTitle className="text-2xl text-indigo-600 dark:text-indigo-400">
              ${Number(pulse?.pipelineValue ?? 0).toLocaleString()}
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            {pulse?.dealCount ?? 0} deals · Forecast ${Number(pulse?.weightedForecast ?? 0).toLocaleString()}
          </CardContent>
        </Card>
        <Card className="card-motion">
          <CardHeader className="pb-2">
            <CardDescription>Total Invoiced</CardDescription>
            <CardTitle className="text-2xl text-emerald-600 dark:text-emerald-400">
              ${Number(pulse?.totalInvoiced ?? 0).toLocaleString()}
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            Receivable: ${Number(pulse?.totalReceivable ?? 0).toLocaleString()}
          </CardContent>
        </Card>
        <Card className="card-motion">
          <CardHeader className="pb-2">
            <CardDescription>Open Support Tickets</CardDescription>
            <CardTitle className="text-2xl text-amber-600 dark:text-amber-400">
              {pulse?.openTickets ?? 0}
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            Active Leads: {pulse?.leadCount ?? 0}
          </CardContent>
        </Card>
        <Card className="card-motion">
          <CardHeader className="pb-2">
            <CardDescription>Workforce & Delivery</CardDescription>
            <CardTitle className="text-2xl text-cyan-600 dark:text-cyan-400">
              {pulse?.employeeHeadcount ?? 0} staff
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            Tasks Done: {pulse?.completedTasks ?? 0} / {pulse?.totalTasks ?? 0}
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="card-motion">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <TrendingUp className="h-4 w-4 text-primary" /> Cross-App Activity Distribution
            </CardTitle>
            <div className="flex items-center gap-1 rounded-lg border bg-muted/30 p-1 text-xs">
              <button
                type="button"
                onClick={() => setActivePulseTab('bar')}
                className={`flex items-center gap-1 px-2.5 py-1 rounded-md transition-all ${
                  activePulseTab === 'bar'
                    ? 'bg-primary text-primary-foreground shadow-xs font-medium'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <BarChart3 className="h-3.5 w-3.5" />
                <span>Bars</span>
              </button>
              <button
                type="button"
                onClick={() => setActivePulseTab('pie')}
                className={`flex items-center gap-1 px-2.5 py-1 rounded-md transition-all ${
                  activePulseTab === 'pie'
                    ? 'bg-primary text-primary-foreground shadow-xs font-medium'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <PieChartIcon className="h-3.5 w-3.5" />
                <span>Donut</span>
              </button>
            </div>
          </CardHeader>
          <CardContent className="h-64">
            {activePulseTab === 'pie' ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Tooltip contentStyle={chartTooltipStyle} />
                  <Pie
                    data={moduleBreakdown}
                    dataKey="count"
                    nameKey="label"
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={85}
                    paddingAngle={4}
                    animationDuration={700}
                  >
                    {moduleBreakdown.map((_r, idx) => (
                      <Cell key={`pie-cell-${idx}`} fill={VIBRANT_PALETTE[idx % VIBRANT_PALETTE.length]} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={moduleBreakdown}>
                  <defs>
                    {VIBRANT_PALETTE.map((color, idx) => (
                      <linearGradient key={`anGrad-${idx}`} id={`anGrad-${idx}`} x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor={color} stopOpacity={1} />
                        <stop offset="100%" stopColor={color} stopOpacity={0.7} />
                      </linearGradient>
                    ))}
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.2} />
                  <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip contentStyle={chartTooltipStyle} cursor={{ fill: 'rgba(99, 102, 241, 0.08)' }} />
                  <Bar dataKey="count" radius={[5, 5, 0, 0]} animationDuration={700}>
                    {moduleBreakdown.map((_r, idx) => (
                      <Cell key={`bar-cell-${idx}`} fill={`url(#anGrad-${idx % VIBRANT_PALETTE.length})`} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card className="card-motion">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <PlusCircle className="h-4 w-4 text-primary" /> Dashboards & Custom Reports
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <form onSubmit={handleCreateDashboard} className="flex gap-2">
              <Input
                placeholder="New dashboard name"
                value={dashName}
                onChange={(e) => setDashName(e.target.value)}
              />
              <Button type="submit" size="sm">
                Add Dashboard
              </Button>
            </form>

            <form onSubmit={handleCreateReport} className="grid gap-2 sm:grid-cols-3">
              <Input
                placeholder="Report title"
                value={reportName}
                onChange={(e) => setReportName(e.target.value)}
              />
              <select
                aria-label="Data Source"
                className="h-9 rounded-md border border-input bg-background px-2 text-xs"
                value={dataSource}
                onChange={(e) => setDataSource(e.target.value)}
              >
                <option value="CRM_DEALS">CRM Deals</option>
                <option value="FINANCE_INVOICES">Finance Invoices</option>
                <option value="SUPPORT_TICKETS">Support Tickets</option>
                <option value="HR_EMPLOYEES">HR Employees</option>
                <option value="PROJECTS_TASKS">Project Tasks</option>
              </select>
              <div className="flex gap-2">
                <select
                  aria-label="Chart Type"
                  className="h-9 rounded-md border border-input bg-background px-2 text-xs"
                  value={chartType}
                  onChange={(e) => setChartType(e.target.value)}
                >
                  <option value="BAR">BAR</option>
                  <option value="PIE">PIE</option>
                  <option value="TABLE">TABLE</option>
                </select>
                <Button type="submit" size="sm">
                  Create Report
                </Button>
              </div>
            </form>

            <div className="space-y-2">
              {reports.map((r) => (
                <div key={r.id} className="flex items-center justify-between rounded-lg border p-2.5 text-xs transition-colors hover:bg-slate-50 dark:hover:bg-muted/40">
                  <div>
                    <p className="font-medium text-sm">{r.name}</p>
                    <p className="text-muted-foreground">
                      {r.dataSource} · {r.chartType}
                    </p>
                  </div>
                  <Button size="sm" variant="outline" className="transition-all hover:scale-105" onClick={() => handleRunReport(r.id)}>
                    <Play className="mr-1 h-3 w-3 text-emerald-500" /> Run
                  </Button>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {reportResult && (
        <Card className="card-motion animate-enter-slide">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="flex items-center gap-2 text-base">
                  <BarChart3 className="h-4 w-4 text-primary" /> Report Output: {reportResult.report.name}
                </CardTitle>
                <CardDescription>
                  Source: {reportResult.report.dataSource} · Format: {reportResult.report.chartType}
                </CardDescription>
              </div>
              <Badge variant="outline">{reportResult.rows.length} Data Points</Badge>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {reportResult.rows.length === 0 ? (
              <p className="text-xs text-muted-foreground">No records in selected source yet.</p>
            ) : (
              <>
                {reportResult.report.chartType === 'BAR' && (
                  <div className="h-64 rounded-xl border bg-slate-50/50 p-4 dark:bg-muted/20">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={reportResult.rows}>
                        <CartesianGrid strokeDasharray="3 3" opacity={0.2} />
                        <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                        <YAxis tick={{ fontSize: 11 }} />
                        <Tooltip contentStyle={chartTooltipStyle} />
                        <Bar dataKey="count" radius={[5, 5, 0, 0]} animationDuration={800}>
                          {reportResult.rows.map((_, idx) => (
                            <Cell key={`rep-bar-${idx}`} fill={VIBRANT_PALETTE[idx % VIBRANT_PALETTE.length]} />
                          ))}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                )}

                {reportResult.report.chartType === 'PIE' && (
                  <div className="h-64 rounded-xl border bg-slate-50/50 p-4 dark:bg-muted/20">
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Tooltip contentStyle={chartTooltipStyle} />
                        <Pie
                          data={reportResult.rows}
                          dataKey="count"
                          nameKey="label"
                          cx="50%"
                          cy="50%"
                          innerRadius={45}
                          outerRadius={80}
                          paddingAngle={4}
                          animationDuration={800}
                        >
                          {reportResult.rows.map((_, idx) => (
                            <Cell key={`rep-pie-${idx}`} fill={VIBRANT_PALETTE[idx % VIBRANT_PALETTE.length]} />
                          ))}
                        </Pie>
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                )}

                <div className="space-y-2">
                  {reportResult.rows.map((row, idx) => (
                    <div key={row.label} className="flex items-center justify-between rounded-lg border px-3.5 py-2 text-sm transition-colors hover:bg-slate-50 dark:hover:bg-muted/30">
                      <div className="flex items-center gap-2.5">
                        <span
                          className="h-2.5 w-2.5 rounded-full"
                          style={{ backgroundColor: VIBRANT_PALETTE[idx % VIBRANT_PALETTE.length] }}
                        />
                        <span className="font-medium">{row.label}</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <Badge variant="secondary">{row.count} records</Badge>
                        {Number(row.amount) > 0 && (
                          <span className="font-mono text-xs font-semibold text-emerald-600 dark:text-emerald-400">
                            ${Number(row.amount).toLocaleString()}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
